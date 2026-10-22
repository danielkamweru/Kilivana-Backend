#!/bin/bash

# Standalone script to test the Daraja (M-PESA Express / STK Push) integration.
#
# Reads the credentials from the environment, falling back to the .env file in the
# project root, so it can be run without exporting anything first.
#
#   ./test-daraja.sh                 # OAuth token + STK Push against the configured environment
#   DARAJA_TEST_PHONE=2547... ./test-daraja.sh
#
# Exits non-zero when Daraja refuses the request, so it can gate a deploy.

set -u
cd "$(dirname "$0")" || exit 1

if [ -f .env ]; then
    set -a
    # shellcheck disable=SC1091
    . ./.env
    set +a
fi

echo "=== Daraja API Test Script ==="
echo ""

for required in DARAJA_CONSUMER_KEY DARAJA_CONSUMER_SECRET DARAJA_PASSKEY; do
    if [ -z "${!required:-}" ]; then
        echo "ERROR: $required is not set"
        echo "Put it in .env, or export it before running this script."
        exit 1
    fi
done

# Set defaults
DARAJA_SHORTCODE=${DARAJA_SHORTCODE:-174379}
DARAJA_ENVIRONMENT=${DARAJA_ENVIRONMENT:-sandbox}
DARAJA_CALLBACK_URL=${DARAJA_CALLBACK_URL:-https://example.com/callback}
# The sandbox test MSISDN. Safaricom prompts this number instead of a real customer.
DARAJA_TEST_PHONE=${DARAJA_TEST_PHONE:-254708374149}
DARAJA_TEST_AMOUNT=${DARAJA_TEST_AMOUNT:-1}

if [ "$DARAJA_ENVIRONMENT" = "production" ]; then
    OAUTH_URL="https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
    STK_URL="https://api.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
else
    OAUTH_URL="https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
    STK_URL="https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
fi

echo "Configuration:"
echo "  Environment: $DARAJA_ENVIRONMENT"
echo "  Shortcode: $DARAJA_SHORTCODE"
echo "  Callback: $DARAJA_CALLBACK_URL"
echo "  Test Phone: $DARAJA_TEST_PHONE"
echo "  Consumer key: ${DARAJA_CONSUMER_KEY:0:4}***"
echo ""

# Test 1: OAuth Token Generation
echo "Test 1: OAuth Token Generation"
echo "-------------------------------"

CREDENTIALS="$DARAJA_CONSUMER_KEY:$DARAJA_CONSUMER_SECRET"
ENCODED_CREDENTIALS=$(printf '%s' "$CREDENTIALS" | base64 | tr -d '\n')

TOKEN_RESPONSE=$(curl -s -m 30 -X GET "$OAUTH_URL" \
  -H "Authorization: Basic $ENCODED_CREDENTIALS" \
  -H "Content-Type: application/json")

# Safaricom formats the token with or without a space after the colon, so the
# value is read loosely instead of with a fixed pattern.
ACCESS_TOKEN=$(printf '%s' "$TOKEN_RESPONSE" | grep -oE '"access_token"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "✗ Failed to obtain OAuth token"
    echo "Response: $(printf '%s' "$TOKEN_RESPONSE" | head -c 400)"
    exit 1
fi

echo "✓ OAuth token obtained"
echo "  Token (first 20 chars): ${ACCESS_TOKEN:0:20}..."
echo ""

# Test 2: STK Push Initiation
echo "Test 2: STK Push Initiation"
echo "----------------------------"

TIMESTAMP=$(date +%Y%m%d%H%M%S)
PASSWORD_RAW="$DARAJA_SHORTCODE$DARAJA_PASSKEY$TIMESTAMP"
PASSWORD=$(printf '%s' "$PASSWORD_RAW" | base64 | tr -d '\n')

echo "Initiating STK Push to: $DARAJA_TEST_PHONE"
echo "Timestamp: $TIMESTAMP"
echo "STK URL: $STK_URL"
echo ""

STK_PAYLOAD=$(cat <<EOF
{
  "BusinessShortCode": "$DARAJA_SHORTCODE",
  "Password": "$PASSWORD",
  "Timestamp": "$TIMESTAMP",
  "TransactionType": "CustomerPayBillOnline",
  "Amount": $DARAJA_TEST_AMOUNT,
  "PartyA": "$DARAJA_TEST_PHONE",
  "PartyB": "$DARAJA_SHORTCODE",
  "PhoneNumber": "$DARAJA_TEST_PHONE",
  "CallBackURL": "$DARAJA_CALLBACK_URL",
  "AccountReference": "TEST001",
  "TransactionDesc": "Test Payment"
}
EOF
)

STK_RESPONSE=$(curl -s -m 40 -X POST "$STK_URL" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$STK_PAYLOAD")

echo "Response: $STK_RESPONSE"
echo ""

RESPONSE_CODE=$(printf '%s' "$STK_RESPONSE" | grep -oE '"ResponseCode"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4)
ERROR_CODE=$(printf '%s' "$STK_RESPONSE" | grep -oE '"errorCode"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4)
ERROR_MESSAGE=$(printf '%s' "$STK_RESPONSE" | grep -oE '"errorMessage"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4)
CHECKOUT_REQUEST_ID=$(printf '%s' "$STK_RESPONSE" | grep -oE '"CheckoutRequestID"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4)
MERCHANT_REQUEST_ID=$(printf '%s' "$STK_RESPONSE" | grep -oE '"MerchantRequestID"[[:space:]]*:[[:space:]]*"[^"]*"' | cut -d'"' -f4)

if [ "$RESPONSE_CODE" = "0" ]; then
    echo "✓ STK Push accepted"
    echo "  MerchantRequestID: $MERCHANT_REQUEST_ID"
    echo "  CheckoutRequestID: $CHECKOUT_REQUEST_ID"
    echo ""
    echo "Safaricom now calls back $DARAJA_CALLBACK_URL with the result."
    exit 0
fi

echo "✗ STK Push refused"
echo "  ResponseCode: ${RESPONSE_CODE:-none}"
echo "  errorCode: ${ERROR_CODE:-none}"
echo "  errorMessage: ${ERROR_MESSAGE:-none}"
echo ""

case "$ERROR_CODE" in
    404.001.03)
        echo "The access token was refused. Either the consumer key is not enabled for"
        echo "M-PESA Express STK Push in the Daraja portal, or the short code and pass key"
        echo "belong to a different Daraja application than the consumer key."
        ;;
    404.001.02)
        echo "The pass key does not match the short code. Copy both from the same"
        echo "application in the Daraja portal."
        ;;
    *)
        echo "Safaricom refused the request. The OAuth token was issued, so the"
        echo "credentials are valid; check the short code, pass key and the products"
        echo "enabled for the application in the Daraja portal."
        ;;
esac

exit 1

#!/bin/bash

# Standalone script to test Daraja API integration
# This script tests OAuth token generation and STK Push initiation
# Usage: ./test-daraja.sh

echo "=== Daraja API Test Script ==="
echo ""

# Check for required environment variables
if [ -z "$DARAJA_CONSUMER_KEY" ]; then
    echo "ERROR: DARAJA_CONSUMER_KEY environment variable is not set"
    echo "Please set it: export DARAJA_CONSUMER_KEY=your_consumer_key"
    exit 1
fi

if [ -z "$DARAJA_CONSUMER_SECRET" ]; then
    echo "ERROR: DARAJA_CONSUMER_SECRET environment variable is not set"
    echo "Please set it: export DARAJA_CONSUMER_SECRET=your_consumer_secret"
    exit 1
fi

if [ -z "$DARAJA_PASSKEY" ]; then
    echo "ERROR: DARAJA_PASSKEY environment variable is not set"
    echo "Please set it: export DARAJA_PASSKEY=your_passkey"
    exit 1
fi

# Set defaults
DARAJA_SHORTCODE=${DARAJA_SHORTCODE:-174379}
DARAJA_ENVIRONMENT=${DARAJA_ENVIRONMENT:-sandbox}
DARAJA_CALLBACK_URL=${DARAJA_CALLBACK_URL:-https://example.com/callback}
DARAJA_TEST_PHONE=${DARAJA_TEST_PHONE:-254722000000}

echo "Configuration:"
echo "  Environment: $DARAJA_ENVIRONMENT"
echo "  Shortcode: $DARAJA_SHORTCODE"
echo "  Test Phone: $DARAJA_TEST_PHONE"
echo ""

# Test 1: OAuth Token Generation
echo "Test 1: OAuth Token Generation"
echo "-------------------------------"

if [ "$DARAJA_ENVIRONMENT" = "production" ]; then
    OAUTH_URL="https://api.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
else
    OAUTH_URL="https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials"
fi

CREDENTIALS="$DARAJA_CONSUMER_KEY:$DARAJA_CONSUMER_SECRET"
ENCODED_CREDENTIALS=$(echo -n "$CREDENTIALS" | base64)

echo "Requesting OAuth token from: $OAUTH_URL"

TOKEN_RESPONSE=$(curl -s -X GET "$OAUTH_URL" \
  -H "Authorization: Basic $ENCODED_CREDENTIALS" \
  -H "Content-Type: application/json")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"access_token":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Failed to obtain OAuth token"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
fi

echo "✓ OAuth token obtained successfully"
echo "  Token (first 20 chars): ${ACCESS_TOKEN:0:20}..."
echo ""

# Test 2: STK Push Initiation
echo "Test 2: STK Push Initiation"
echo "----------------------------"

TIMESTAMP=$(date +%Y%m%d%H%M%S)
PASSWORD_RAW="$DARAJA_SHORTCODE$DARAJA_PASSKEY$TIMESTAMP"
PASSWORD=$(echo -n "$PASSWORD_RAW" | base64)

if [ "$DARAJA_ENVIRONMENT" = "production" ]; then
    STK_URL="https://api.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
else
    STK_URL="https://sandbox.safaricom.co.ke/mpesa/stkpush/v1/processrequest"
fi

echo "Initiating STK Push to: $DARAJA_TEST_PHONE"
echo "Timestamp: $TIMESTAMP"
echo "STK URL: $STK_URL"

STK_PAYLOAD=$(cat <<EOF
{
  "BusinessShortCode": $DARAJA_SHORTCODE,
  "Password": "$PASSWORD",
  "Timestamp": "$TIMESTAMP",
  "TransactionType": "CustomerPayBillOnline",
  "Amount": 1,
  "PartyA": "$DARAJA_TEST_PHONE",
  "PartyB": $DARAJA_SHORTCODE,
  "PhoneNumber": "$DARAJA_TEST_PHONE",
  "CallBackURL": "$DARAJA_CALLBACK_URL",
  "AccountReference": "TEST001",
  "TransactionDesc": "Test Payment"
}
EOF
)

STK_RESPONSE=$(curl -s -X POST "$STK_URL" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "$STK_PAYLOAD")

echo "Response: $STK_RESPONSE"
echo ""

# Parse response
RESPONSE_CODE=$(echo "$STK_RESPONSE" | grep -o '"ResponseCode":"[^"]*' | cut -d'"' -f4)
CHECKOUT_REQUEST_ID=$(echo "$STK_RESPONSE" | grep -o '"CheckoutRequestID":"[^"]*' | cut -d'"' -f4)
MERCHANT_REQUEST_ID=$(echo "$STK_RESPONSE" | grep -o '"MerchantRequestID":"[^"]*' | cut -d'"' -f4)

if [ "$RESPONSE_CODE" = "0" ]; then
    echo "✓ STK Push initiated successfully"
    echo "  MerchantRequestID: $MERCHANT_REQUEST_ID"
    echo "  CheckoutRequestID: $CHECKOUT_REQUEST_ID"
    echo ""
    echo "Note: If $DARAJA_TEST_PHONE is a real Safaricom number, you should receive an STK prompt."
    echo "Enter your M-PESA PIN to complete the test transaction."
else
    echo "⚠ STK Push request failed or returned an error"
    echo "  ResponseCode: $RESPONSE_CODE"
    echo ""
    echo "This is expected if:"
    echo "  - Using test credentials (phone number may not be valid)"
    echo "  - Phone number is not a Safaricom M-PESA number"
    echo "  - Test environment has restrictions"
    echo ""
    echo "The OAuth token test passed, so your credentials are valid."
fi

echo ""
echo "=== Test Complete ==="

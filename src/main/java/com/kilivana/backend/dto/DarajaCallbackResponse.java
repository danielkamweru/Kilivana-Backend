package com.kilivana.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response sent back to Safaricom after processing the callback.
 * Must return 200 OK within 30 seconds or Safaricom will retry.
 */
@Schema(description = "Response to Safaricom acknowledging callback receipt")
public class DarajaCallbackResponse {

    @Schema(description = "Result code: 0 = accepted, non-zero = error", example = "0")
    private int resultCode = 0;

    @Schema(description = "Description for Safaricom", example = "Callback processed successfully")
    private String resultDesc = "Callback processed successfully";

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
    }
}
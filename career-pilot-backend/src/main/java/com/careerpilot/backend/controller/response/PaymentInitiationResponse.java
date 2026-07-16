package com.careerpilot.backend.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Response after successfully initiating a Paymob checkout")
public class PaymentInitiationResponse {

    @Schema(description = "Paymob-hosted checkout URL to open in a WebView or browser",
            example = "https://accept.paymob.com/unifiedcheckout/?publicKey=egy_pk_test_...&clientSecret=egy_csk_test_...")
    private String checkoutUrl;

    @Schema(description = "Internal merchant order id used to correlate this checkout with its transaction record",
            example = "CP-1-6dfe38d0-5f3b-42e1-8905-62775bd76260")
    private String merchantOrderId;
}
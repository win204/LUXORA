package com.luxora.commerce.shipping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "luxora.shipping.ghn")
public record GhnProperties(
        String apiUrl,
        String token,
        String shopId,
        Integer fromDistrictId,
        String fromWardCode,
        String clientPhone,
        String webhookToken) {

    public GhnProperties {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api";
        }
        if (token == null || token.isBlank()) {
            token = "d017a81e-190e-11ee-8bfa-8a0ded896952";
        }
        if (shopId == null || shopId.isBlank()) {
            shopId = "124922";
        }
        if (fromDistrictId == null) {
            fromDistrictId = 1442; // District 1, HCMC
        }
        if (fromWardCode == null || fromWardCode.isBlank()) {
            fromWardCode = "20101"; // Ben Nghe Ward
        }
        if (clientPhone == null || clientPhone.isBlank()) {
            clientPhone = "0901234567";
        }
        if (webhookToken == null || webhookToken.isBlank()) {
            webhookToken = "luxora_ghn_webhook_secret_key";
        }
    }
}

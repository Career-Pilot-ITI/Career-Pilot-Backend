package com.careerpilot.backend.security.paymob;

import com.careerpilot.backend.config.PaymobConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymobHmacVerifier {

    private final PaymobConfig config;

    private static final String[] HMAC_KEYS = {
            "amount_cents", "created_at", "currency", "error_occured",
            "has_parent_transaction", "id", "integration_id", "is_3d_secure",
            "is_auth", "is_capture", "is_refunded", "is_standalone_payment",
            "is_voided", "order.id", "owner", "pending",
            "source_data.pan", "source_data.sub_type", "source_data.type", "success"
    };

    public boolean isValid(Map<String, Object> obj, String receivedHmac) {
        if (receivedHmac == null) return false;
        StringBuilder sb = new StringBuilder();
        for (String key : HMAC_KEYS) {
            sb.append(resolve(obj, key));
        }
        return hmacSha512(sb.toString(), config.getHmacSecret()).equalsIgnoreCase(receivedHmac);
    }

    @SuppressWarnings("unchecked")
    private Object resolve(Map<String, Object> data, String dottedKey) {
        Object current = data;
        for (String part : dottedKey.split("\\.")) {
            if (!(current instanceof Map)) return "";
            current = ((Map<String, Object>) current).get(part);
        }
        return current == null ? "" : current;
    }

    private String hmacSha512(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
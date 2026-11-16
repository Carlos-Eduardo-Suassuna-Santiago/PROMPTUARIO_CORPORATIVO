package br.edu.ifrn.promptuario.reporting.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebhookDeliveryServiceTest {

    @Test
    void signsExactPayloadUsingHmacSha256() throws Exception {
        byte[] payload = "{\"event\":\"report.completed\"}".getBytes(StandardCharsets.UTF_8);
        String secret = "shared-secret";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));

        assertEquals(expected, WebhookDeliveryService.signatureHeader(payload, secret));
    }
}

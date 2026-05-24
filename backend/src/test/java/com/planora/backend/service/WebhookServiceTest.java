package com.planora.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService")
class WebhookServiceTest {

    private static final String SECRET = "test-secret";
    private static final String EVENT_ISSUES = "issues";

    @Mock private WebhookEventHandler handler;

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(List.of(handler));
        ReflectionTestUtils.setField(webhookService, "webhookSecret", SECRET);
        lenient().when(handler.supports(EVENT_ISSUES)).thenReturn(true);
    }

    private static String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("validação de assinatura HMAC")
    class HmacValidation {

        @Test
        @DisplayName("deve retornar false quando assinatura é nula")
        void deveRetornarFalse_quandoAssinaturaEhNula() {
            boolean result = webhookService.processEvent(EVENT_ISSUES, null, "{}");

            assertThat(result).isFalse();
            verify(handler, never()).handle(any());
        }

        @Test
        @DisplayName("deve retornar false quando assinatura não começa com sha256=")
        void deveRetornarFalse_quandoAssinaturaSemPrefixo() {
            boolean result = webhookService.processEvent(EVENT_ISSUES, "md5=abc", "{}");

            assertThat(result).isFalse();
            verify(handler, never()).handle(any());
        }

        @Test
        @DisplayName("deve retornar false quando HMAC computado não bate")
        void deveRetornarFalse_quandoHmacInvalido() {
            boolean result = webhookService.processEvent(EVENT_ISSUES, "sha256=deadbeef", "{}");

            assertThat(result).isFalse();
            verify(handler, never()).handle(any());
        }

        @Test
        @DisplayName("deve retornar false quando payload é alterado após assinatura")
        void deveRetornarFalse_quandoPayloadAlteradoAposAssinatura() {
            String original = "{\"action\":\"opened\"}";
            String tampered = "{\"action\":\"modified\"}";

            boolean result = webhookService.processEvent(EVENT_ISSUES, hmacSha256(original, SECRET), tampered);

            assertThat(result).isFalse();
            verify(handler, never()).handle(any());
        }

        @Test
        @DisplayName("deve aceitar e retornar true quando HMAC bate exatamente")
        void deveRetornarTrue_quandoHmacValido() {
            String payload = "{\"action\":\"opened\"}";

            boolean result = webhookService.processEvent(EVENT_ISSUES, hmacSha256(payload, SECRET), payload);

            assertThat(result).isTrue();
            verify(handler).handle(payload);
        }

        @Test
        @DisplayName("deve aceitar qualquer assinatura quando webhookSecret está vazio")
        void deveAceitar_quandoSecretVazio() {
            ReflectionTestUtils.setField(webhookService, "webhookSecret", "");
            String payload = "{\"action\":\"opened\"}";

            boolean result = webhookService.processEvent(EVENT_ISSUES, null, payload);

            assertThat(result).isTrue();
            verify(handler).handle(payload);
        }
    }

    @Nested
    @DisplayName("dispatch — Strategy")
    class Dispatch {

        @Test
        @DisplayName("deve retornar false quando nenhum handler suporta o evento")
        void deveRetornarFalse_quandoNenhumHandlerSuporta() {
            String payload = "{\"ref\":\"main\"}";

            boolean result = webhookService.processEvent("push", hmacSha256(payload, SECRET), payload);

            assertThat(result).isFalse();
            verify(handler, never()).handle(any());
        }

        @Test
        @DisplayName("deve delegar ao handler correto e retornar true")
        void deveDelegarAoHandlerCorreto() {
            String payload = "{\"action\":\"opened\"}";

            boolean result = webhookService.processEvent(EVENT_ISSUES, hmacSha256(payload, SECRET), payload);

            assertThat(result).isTrue();
            verify(handler).handle(payload);
        }
    }
}

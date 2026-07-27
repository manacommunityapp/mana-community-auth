package com.manacommunity.auth.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.manacommunity.auth.config.RegistrationSecurityProperties;
import com.manacommunity.common.exception.ManaCommunityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Verifies Google reCAPTCHA tokens server-side against the siteverify endpoint.
 *
 * <p>Config-gated by {@code app.security.registration.recaptcha.enabled}: when
 * disabled (the default) {@link #verify} is a no-op so authenticated/admin
 * registration and tests are unaffected. When enabled, a missing or invalid
 * token raises a {@code 400} {@link ManaCommunityException}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecaptchaService {

    private final RegistrationSecurityProperties props;
    private final RestClient restClient = RestClient.create();

    /** Throws {@link ManaCommunityException} (400) when verification is required and fails. */
    public void verify(String token, String remoteIp) {
        RegistrationSecurityProperties.Recaptcha cfg = props.getRecaptcha();
        if (!cfg.isEnabled()) {
            return; // feature off — nothing to verify
        }
        if (!StringUtils.hasText(cfg.getSecret())) {
            log.error("reCAPTCHA is enabled but app.security.registration.recaptcha.secret is not set.");
            throw new ManaCommunityException(
                    "Captcha verification is misconfigured.", HttpStatus.INTERNAL_SERVER_ERROR, "RECAPTCHA_MISCONFIGURED");
        }
        if (!StringUtils.hasText(token)) {
            throw new ManaCommunityException(
                    "Captcha verification is required.", HttpStatus.BAD_REQUEST, "RECAPTCHA_MISSING");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", cfg.getSecret());
        form.add("response", token);
        if (StringUtils.hasText(remoteIp)) {
            form.add("remoteip", remoteIp);
        }

        try {
            SiteVerifyResponse body = restClient.post()
                    .uri(cfg.getVerifyUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(SiteVerifyResponse.class);

            if (body == null || !body.success()) {
                log.warn("reCAPTCHA verification failed: {}",
                        body == null ? "no response" : body.errorCodes());
                throw new ManaCommunityException(
                        "Captcha verification failed. Please try again.", HttpStatus.BAD_REQUEST, "RECAPTCHA_FAILED");
            }
        } catch (ManaCommunityException e) {
            throw e;
        } catch (Exception e) {
            log.error("reCAPTCHA siteverify call errored: {}", e.getMessage());
            throw new ManaCommunityException(
                    "Could not verify captcha right now. Please try again.", HttpStatus.BAD_GATEWAY, "RECAPTCHA_UNAVAILABLE");
        }
    }

    /** Subset of Google's siteverify response we care about. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SiteVerifyResponse(
            boolean success,
            @JsonProperty("error-codes") List<String> errorCodes) {
    }
}

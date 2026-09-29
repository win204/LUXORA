package com.luxora.commerce.auth.security;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.luxora.commerce.common.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Duration accessTokenTtl;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${luxora.auth.jwt.secret:}") String configuredSecret,
            @Value("${luxora.auth.jwt.access-token-ttl:PT15M}") Duration accessTokenTtl) {
        this.objectMapper = objectMapper;
        this.secret = resolveSecret(configuredSecret);
        this.accessTokenTtl = accessTokenTtl;
    }

    public String createAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = Map.of(
                "sub", userId.toString(),
                "email", email,
                "roles", roles,
                "iat", now.getEpochSecond(),
                "exp", now.plus(accessTokenTtl).getEpochSecond());
        return sign(header, claims);
    }

    public AuthenticatedUser parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedException("INVALID_TOKEN", "Invalid access token");
            }

            String unsignedToken = parts[0] + "." + parts[1];
            String expectedSignature = signature(unsignedToken);
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new UnauthorizedException("INVALID_TOKEN", "Invalid access token");
            }

            Map<String, Object> claims = objectMapper.readValue(BASE64_URL_DECODER.decode(parts[1]), new TypeReference<>() {
            });
            Number expiresAt = (Number) claims.get("exp");
            if (expiresAt == null || Instant.ofEpochSecond(expiresAt.longValue()).isBefore(Instant.now())) {
                throw new UnauthorizedException("TOKEN_EXPIRED", "Access token expired");
            }

            String subject = (String) claims.get("sub");
            String email = (String) claims.get("email");
            List<String> roles = ((List<?>) claims.getOrDefault("roles", List.of())).stream()
                    .map(Object::toString)
                    .toList();
            return new AuthenticatedUser(UUID.fromString(subject), email, roles);
        } catch (UnauthorizedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new UnauthorizedException("INVALID_TOKEN", "Invalid access token");
        }
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    private String sign(Map<String, Object> header, Map<String, Object> claims) {
        try {
            String encodedHeader = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(header));
            String encodedClaims = BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String unsignedToken = encodedHeader + "." + encodedClaims;
            return unsignedToken + "." + signature(unsignedToken);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create access token", exception);
        }
    }

    private String signature(String unsignedToken) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return BASE64_URL_ENCODER.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    }

    private byte[] resolveSecret(String configuredSecret) {
        if (StringUtils.hasText(configuredSecret)) {
            return configuredSecret.getBytes(StandardCharsets.UTF_8);
        }
        byte[] generated = new byte[32];
        new java.security.SecureRandom().nextBytes(generated);
        return generated;
    }
}

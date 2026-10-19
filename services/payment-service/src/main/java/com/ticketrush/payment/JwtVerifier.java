package com.ticketrush.payment;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Verifies access tokens issued by user-service (HS256, shared JWT_SECRET). */
@Component
public class JwtVerifier {

    private final JwtParser parser;

    public JwtVerifier(@Value("${ticketrush.jwt-secret}") String secret) {
        this.parser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    public AuthUser verify(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "missing bearer token");
        }
        try {
            var claims = parser.parseSignedClaims(authorization.substring(7)).getPayload();
            if (!"access".equals(claims.get("token_type"))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "access token required");
            }
            // simplejwt writes user_id as a string; accept a number too
            var userId = Long.parseLong(String.valueOf(claims.get("user_id")));
            return new AuthUser(userId, claims.get("email", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid or expired token");
        }
    }
}

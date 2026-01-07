package com.decoaromas.decoaromaspos.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    @Value("${jwt.secret:default_change_me_long_secret}")
    private String secret;

    @Value("${jwt.expirationMinutes:1000}")
    private long expirationMinutes;

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(secret);
    }

    public String createToken(String email, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(expirationMinutes));
        return JWT.create()
                .withSubject(email)
                .withIssuer("decoaromas")
                .withIssuedAt(now)
                .withExpiresAt(exp)
                .withClaim("role", role)
                .sign(getAlgorithm());
    }

    public boolean isValid(String token) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            verifier.verify(token);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    public DecodedJWT decode(String token) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(getAlgorithm()).build();
        return verifier.verify(token);
    }

    // Método que pidió el filtro: obtiene el email (subject) de forma segura
    public String getEmail(String token) throws JWTVerificationException {
        DecodedJWT jwt = decode(token);
        return jwt.getSubject();
    }

    // Alias útil por claridad
    public String extractEmail(String token) throws JWTVerificationException {
        return getEmail(token);
    }

    public String getRole(String token) throws JWTVerificationException {
        DecodedJWT jwt = decode(token);
        return jwt.getClaim("role").asString();
    }
}

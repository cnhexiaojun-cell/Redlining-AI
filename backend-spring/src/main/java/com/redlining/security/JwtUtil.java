package com.redlining.security;

import com.redlining.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final AppProperties appProperties;
    private final SecretKey key;

    public JwtUtil(AppProperties appProperties) {
        this.appProperties = appProperties;
        String secret = appProperties.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            secret = secret + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId) {
        long expMs = appProperties.getJwt().getExpirationMinutes() * 60L * 1000L;
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String sub = claims.getSubject();
            return sub == null ? null : Long.parseLong(sub);
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Short-lived token for OnlyOffice to load document file (no user auth). */
    public String createDocumentFileToken(String documentId) {
        int expirySeconds = 5 * 60;
        return Jwts.builder()
                .subject("doc:" + documentId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirySeconds * 1000L))
                .signWith(key)
                .compact();
    }

    public String parseDocumentIdFromFileToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String sub = claims.getSubject();
            if (sub != null && sub.startsWith("doc:")) {
                return sub.substring(4);
            }
            return null;
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Short-lived token for contract preview (OnlyOffice fetches file from /api/preview/file). */
    public String createPreviewFileToken(String minioKey) {
        int expirySeconds = 15 * 60;
        return Jwts.builder()
                .subject("preview:" + minioKey)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirySeconds * 1000L))
                .signWith(key)
                .compact();
    }

    public String parsePreviewKeyFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String sub = claims.getSubject();
            if (sub != null && sub.startsWith("preview:")) {
                return sub.substring(8);
            }
            return null;
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            return null;
        }
    }
}

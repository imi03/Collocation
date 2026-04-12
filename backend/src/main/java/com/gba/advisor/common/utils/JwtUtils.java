package com.gba.advisor.common.utils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {
    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.access-token-expiration}") private long accessExp;
    @Value("${jwt.refresh-token-expiration}") private long refreshExp;

    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

    public String generateAccessToken(Long userId, String username) { return build(userId, username, accessExp*1000L, "access"); }
    public String generateRefreshToken(Long userId, String username) { return build(userId, username, refreshExp*1000L, "refresh"); }

    private String build(Long userId, String username, long expMs, String type) {
        return Jwts.builder().subject(String.valueOf(userId))
                .claim("username", username).claim("type", type)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expMs))
                .signWith(key()).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }
    public Long getUserId(String token) { return Long.valueOf(parse(token).getSubject()); }
    public String getUsername(String token) { return parse(token).get("username", String.class); }

    public boolean validate(String token) {
        try { parse(token); return true; }
        catch (ExpiredJwtException e) { log.warn("Token已过期"); }
        catch (JwtException e) { log.warn("Token无效: {}", e.getMessage()); }
        return false;
    }
}

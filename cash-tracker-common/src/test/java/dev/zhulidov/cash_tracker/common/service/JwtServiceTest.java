package dev.zhulidov.cash_tracker.common.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    // 32 байта = минимум для HS256; JwtService декодирует секрет как base64
    private static final String SECRET =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final long EXPIRATION_MS = 60_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRATION_MS);
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    @Test
    void generateToken_producesValidTokenWithEmailSubject() {
        String token = jwtService.generateToken("user@test.com", 42L);

        assertFalse(token.isBlank());
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("user@test.com", jwtService.extractEmail(token));
    }

    @Test
    void generateToken_putsUserIdAndExpirationIntoClaims() {
        String token = jwtService.generateToken("user@test.com", 42L);

        Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();

        assertEquals(42L, ((Number) claims.get("userId")).longValue());
        long lifetime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        // issuedAt хранится с точностью до секунды, поэтому допускаем погрешность в секунду
        assertTrue(Math.abs(lifetime - EXPIRATION_MS) <= 1000, "lifetime was " + lifetime);
    }

    @Test
    void isTokenValid_whenTokenIsExpired_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String expired = jwtService.generateToken("user@test.com", 1L);

        assertFalse(jwtService.isTokenValid(expired));
    }

    @Test
    void isTokenValid_whenSignedWithAnotherKey_returnsFalse() {
        SecretKey otherKey = Keys.hmacShaKeyFor("another-secret-key-another-secret-key".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder().subject("user@test.com").signWith(otherKey).compact();

        assertFalse(jwtService.isTokenValid(forged));
    }

    @Test
    void isTokenValid_whenSignatureIsTampered_returnsFalse() {
        String token = jwtService.generateToken("user@test.com", 1L);
        String[] parts = token.split("\\.");
        // меняем первый символ подписи: он целиком влияет на байты подписи
        char first = parts[2].charAt(0);
        String tamperedSignature = (first == 'A' ? 'B' : 'A') + parts[2].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertFalse(jwtService.isTokenValid(tampered));
    }

    @Test
    void isTokenValid_whenTokenIsGarbage_returnsFalse() {
        assertFalse(jwtService.isTokenValid("not-a-jwt"));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid(null));
    }

    @Test
    void extractEmail_whenTokenIsInvalid_throws() {
        assertThrows(RuntimeException.class, () -> jwtService.extractEmail("not-a-jwt"));
    }

    @Test
    void getExpirationSeconds_convertsMillisecondsToSeconds() {
        assertEquals(60L, jwtService.getExpirationSeconds());
    }

    @Test
    void generateToken_whenSecretIsShorterThan256Bits_throwsWeakKeyException() {
        String weak = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtService, "secretKey", weak);

        assertThrows(WeakKeyException.class, () -> jwtService.generateToken("user@test.com", 1L));
    }
}

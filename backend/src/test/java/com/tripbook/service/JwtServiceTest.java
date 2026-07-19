package com.tripbook.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No Spring context, no mocks — JwtService has no repository dependencies, so
 * these are true unit tests of the signing/parsing logic itself.
 */
class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-needs-32-bytes-minimum-0123456789";

    @Test
    void aGeneratedTokenValidates() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken("user@tripbook.com", "USER");

        assertThat(jwtService.isTokenValid(token, "user@tripbook.com")).isTrue();
    }

    @Test
    void anExpiredTokenIsRejected() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1); // expires in 1ms
        String token = jwtService.generateToken("user@tripbook.com", "USER");
        Thread.sleep(20);

        assertThat(jwtService.isTokenValid(token, "user@tripbook.com")).isFalse();
    }

    @Test
    void aTamperedTokenIsRejected() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken("user@tripbook.com", "USER");
        // Flip a character in the payload segment so the signature no longer matches.
        String[] parts = token.split("\\.");
        char[] payload = parts[1].toCharArray();
        payload[payload.length / 2] = payload[payload.length / 2] == 'a' ? 'b' : 'a';
        String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

        assertThat(jwtService.isTokenValid(tampered, "user@tripbook.com")).isFalse();
    }

    @Test
    void subjectAndRoleClaimsRoundTripCorrectly() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken("admin@tripbook.com", "ADMIN");

        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@tripbook.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValidReturnsFalseWhenSubjectDoesNotMatchExpectedEmail() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken("user@tripbook.com", "USER");

        assertThat(jwtService.isTokenValid(token, "someone-else@tripbook.com")).isFalse();
    }
}

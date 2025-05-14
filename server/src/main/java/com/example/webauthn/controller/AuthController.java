package main.java.com.example.webauthn.controller;

import com.example.webauthn.model.RegisterRequest;
import com.example.webauthn.model.RegisterResponse;
import com.example.webauthn.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpHeaders;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registerRequest")
    public Map<String, Object> registerRequest(@RequestBody RegisterRequest request) {
        return authService.generateRegistrationOptions(request.getUsername());
    }

    @PostMapping("/registerResponse")
    public ResponseEntity<?> registerResponse(@RequestBody RegisterResponse response) {
        boolean success = authService.verifyAndStoreCredential(response);
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PostMapping("/loginRequest")
    public Map<String, Object> loginRequest(@RequestBody Map<String, String> body) {
        return authService.generateLoginOptions(body.get("username"));
    }

    @PostMapping("/loginResponse")
    public ResponseEntity<?> loginResponse(
            @RequestBody Map<String, Object> body,
            HttpServletResponse response) {
        boolean verified = verifyAssertion(body);
        if (!verified) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Assertion verification failed"));
        }

        String username = (String) body.get("username");

        // Access token
        String accessToken = generateAccessToken(username);

        // Refresh token（Redis store jti）
        String jti = UUID.randomUUID().toString();
        String refreshToken = generateRefreshToken(username, jti);
        redis.opsForValue().set("refresh:" + jti, username, Duration.ofDays(7));

        // Set Access Token cookie
        ResponseCookie accessCookie = ResponseCookie.from("access", accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(15 * 60) // 15mins
                .sameSite("Strict")
                .build();

        // Set Refresh Token cookie（Only /auth/refresh Path）
        ResponseCookie refreshCookie = ResponseCookie.from("refresh", refreshToken)
                .httpOnly(true)
                .path("/api/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(Map.of("success", true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh", required = false) String refreshToken) {
        if (refreshToken == null)
            return ResponseEntity.status(401).build();

        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(jwtSecret).build()
                    .parseClaimsJws(refreshToken).getBody();

            String jti = claims.getId();
            String username = claims.getSubject();

            // Verify that the refresh session is valid in Redis
            String stored = redis.opsForValue().get("refresh:" + jti);
            if (stored == null || !stored.equals(username)) {
                return ResponseEntity.status(401).build();
            }

            // Issue Access Token
            String newAccessToken = generateAccessToken(username);
            ResponseCookie newAccessCookie = ResponseCookie.from("access", newAccessToken)
                    .httpOnly(true).path("/").maxAge(15 * 60).sameSite("Strict").build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, newAccessCookie.toString())
                    .body(Map.of("success", true));
        } catch (JwtException e) {
            return ResponseEntity.status(401).build();
        }
    }

}

package main.java.com.example.webauthn.service;

import com.example.webauthn.model.RegisterResponse;
import com.example.webauthn.model.UserCredential;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

@Service
public class AuthService {

    private final Map<String, UserCredential> database = new ConcurrentHashMap<>();

    public Map<String, Object> generateRegistrationOptions(String username) {
        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge));
        options.put("rp", Map.of("name", "Demo App"));
        options.put("user", Map.of(
                "id", Base64.getUrlEncoder().withoutPadding().encodeToString(username.getBytes()),
                "name", username,
                "displayName", username));
        options.put("pubKeyCredParams", List.of(Map.of("type", "public-key", "alg", -7)));
        options.put("timeout", 60000);
        options.put("attestation", "direct");
        return options;
    }

    public boolean verifyAndStoreCredential(RegisterResponse response) {
        UserCredential cred = new UserCredential();
        cred.setCredentialId(response.getId());
        cred.setUsername(response.getUsername());
        database.put(response.getUsername(), cred);
        return true;
    }

    public Map<String, Object> generateLoginOptions(String username) {
        UserCredential cred = database.get(username);
        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", Base64.getUrlEncoder().withoutPadding().encodeToString(challenge));
        options.put("timeout", 60000);
        options.put("userVerification", "preferred");

        if (cred != null) {
            options.put("allowCredentials", List.of(Map.of(
                    "type", "public-key",
                    "id", cred.getCredentialId())));
        }

        return options;
    }

    private static final SecretKey jwtSecret = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateAccessToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 mins
                .signWith(jwtSecret)
                .compact();
    }

    public String generateRefreshToken(String username, String jti) {
        return Jwts.builder()
                .setSubject(username)
                .setId(jti) // jti = refresh id
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1 * 24 * 60 * 60 * 1000)) // 1 day
                .signWith(jwtSecret)
                .compact();
    }

    public boolean verifyAssertion(Map<String, Object> body) {
        // TODO：verify clientDataJSON + authenticatorData + signature
        return true;
    }
}

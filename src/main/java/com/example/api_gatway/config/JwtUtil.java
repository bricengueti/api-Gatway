package com.example.api_gatway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtUtil {

    private PublicKey publicKey;

    public void setPublicKey(String pem) {
        try {
            String publicKeyPEM = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(publicKeyPEM);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Échec du parsing de la clé publique", e);
        }
    }

    public boolean isInvalid(String token) {
        try {
            getAllClaimsFromToken(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

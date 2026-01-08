package kz.chitas.chess.service.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Log4j2
public class JWTService {

    private String secretkey = System.getenv("SECRET_KEY_FOR_JWTS");

    public JWTService() {
        log.info("JWTService initialized");
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        String token = Jwts.builder()
                .claims()
                .add(claims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7))
                .and()
                .signWith(getKey())
                .compact();
        log.info("Generated JWT for user: {}", username);
        return token;
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretkey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUserName(String token) {
        String username = extractClaim(token, Claims::getSubject);
        log.debug("Extracted username from token: {}", username);
        return username;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.debug("Extracted claims from token");
            return claims;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return getDefaultClaims();
        } catch (Exception e) {
            log.error("JWT parsing failed: {}", e.getMessage());
            return getDefaultClaims();
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String userName = extractUserName(token);
        boolean isValid = userName.equals(userDetails.getUsername()) && !isTokenExpired(token);
        log.info("Token validation for user {}: {}", userName, isValid ? "valid" : "invalid");
        return isValid;
    }

    public boolean validateToken(String token) {
        final String userName = extractUserName(token);
        if (userName == null) {
            return false;
        }
        boolean isValid = userName.equals(SecurityContextHolder.getContext().getAuthentication().getName())
                && !isTokenExpired(token);
        log.info("Token validation for user {}: {}", userName, isValid ? "valid" : "invalid");
        return isValid;
    }

    private boolean isTokenExpired(String token) {
        boolean expired = extractExpiration(token).before(new Date());
        log.debug("Token expiration check: {}", expired ? "expired" : "valid");
        return expired;
    }

    private Date extractExpiration(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        log.debug("Extracted expiration: {}", expiration);
        return expiration;
    }

    private Claims getDefaultClaims() {
        Claims defaultClaims = Jwts.claims()
                .add("sub", "")
                .add("exp", 0L)
                .build();
        log.debug("Returning default claims");
        return defaultClaims;
    }
}
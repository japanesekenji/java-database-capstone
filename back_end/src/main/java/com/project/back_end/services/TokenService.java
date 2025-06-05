package com.project.back_end.services;

import com.project.back_end.repositories.AdminRepository;
import com.project.back_end.repositories.DoctorRepository;
import com.project.back_end.repositories.PatientRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service class to handle JWT token generation, extraction, and validation.
 * It uses a secret key defined in application properties for signing and verifying tokens.
 */
@Component // Marks this class as a Spring component to be managed by the Spring container
public class TokenService {

    // Inject repositories to validate user existence during token validation
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    // Secret key for JWT signing, injected from application.properties
    @Value("${jwt.secret}")
    private String secret;

    // Token expiration time in milliseconds (7 days)
    @Value("${jwt.expiration}")
    private long expiration; // Default to 7 days if not specified in properties

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of the repositories.
     *
     * @param adminRepository   Repository for Admin entities.
     * @param doctorRepository  Repository for Doctor entities.
     * @param patientRepository Repository for Patient entities.
     */
    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        // Default expiration if not set via @Value (e.g., in tests)
        this.expiration = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
    }

    /**
     * Generates a JWT token for a given user ID.
     * The user ID is stored as the token's subject.
     *
     * @param userId The unique identifier (ID) of the user for whom the token is generated.
     * @return The generated JWT token string.
     */
    public String generateToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        // You can add more claims here if needed (e.g., user role)
        return createToken(claims, userId.toString()); // Subject is userId as a String
    }

    /**
     * Creates the JWT token with specified claims, subject, issued date, and expiration.
     *
     * @param claims  Additional claims to include in the token payload.
     * @param subject The subject of the token (in our case, the user ID as a String).
     * @return The signed JWT token string.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims) // Set the custom claims
                .subject(subject) // Set the subject (user ID)
                .issuedAt(new Date(System.currentTimeMillis())) // Set token issuance date
                .expiration(new Date(System.currentTimeMillis() + expiration)) // Set token expiration date (e.g., 7 days from now)
                .signWith(getSigningKey(), Jwts.SIG.HS256) // Sign the token with the secret key using HS256 algorithm
                .compact(); // Compact the JWT into its final string form
    }

    /**
     * Extracts the user ID (subject) from a JWT token.
     *
     * @param token The JWT token from which the user ID is to be extracted.
     * @return The user ID as a Long, or null if extraction fails or token is invalid.
     */
    public Long getUserIdFromToken(String token) {
        try {
            // Get the subject (which is the userId as a String) and parse it to Long
            String subject = extractClaim(token, Claims::getSubject);
            return Long.parseLong(subject);
        } catch (Exception e) {
            System.err.println("Error extracting user ID from token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a specific claim from the token's payload.
     * This is a generic helper method to extract any claim.
     *
     * @param token          The JWT token.
     * @param claimsResolver A function to resolve the desired claim from the Claims object.
     * @param <T>            The type of the claim.
     * @return The extracted claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token The JWT token.
     * @return The Claims object containing all payload claims.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // Verify the token with the secret key
                .build()
                .parseSignedClaims(token) // Parse the signed JWT
                .getPayload(); // Get the payload (Claims)
    }

    /**
     * Checks if the token is expired.
     *
     * @param token The JWT token.
     * @return true if the token's expiration date is before the current date, false otherwise.
     */
    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Validates the JWT token for a given user type (admin, doctor, or patient).
     * It checks token expiration and if a corresponding user exists in the database.
     *
     * @param token The JWT token to be validated.
     * @param user  The type of user (e.g., "admin", "doctor", "patient").
     * @return true if the token is valid for the specified user type and not expired,
     * false if the token is invalid, expired, or the user does not exist.
     */
    public boolean validateToken(String token, String user) {
        try {
            Long userId = getUserIdFromToken(token); // Extract user ID from token
            if (userId == null || isTokenExpired(token)) {
                return false; // Token is invalid or expired
            }

            // Check user existence based on role
            switch (user.toLowerCase()) {
                case "admin":
                    return adminRepository.findById(userId).isPresent();
                case "doctor":
                    return doctorRepository.findById(userId).isPresent();
                case "patient":
                    return patientRepository.findById(userId).isPresent();
                default:
                    return false; // Unknown user type
            }
        } catch (Exception e) {
            System.err.println("Token validation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the signing key used for JWT token signing.
     * This key is derived from the secret string configured in application properties.
     *
     * @return The SecretKey used for signing JWTs.
     */
    private SecretKey getSigningKey() {
        // IMPORTANT: For production, ensure your `jwt.secret` is a strong, base64-encoded string
        // (e.g., 256-bit or 512-bit). Using Keys.hmacShaKeyFor directly accepts byte array from any string.
        // A common practice is to generate a strong key once and store it as a base64 string.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

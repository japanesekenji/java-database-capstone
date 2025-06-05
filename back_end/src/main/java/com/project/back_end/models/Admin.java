package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.Data; // From Lombok

@Data // Generates getters, setters, equals, hashCode, and toString
@Entity // Marks this class as a JPA entity, mapped to a database table
public class Admin {
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID for MySQL
    private Long id;

    @NotNull(message = "username cannot be null") // Ensures the username cannot be null
    private String username;

    @NotNull(message = "password cannot be null") // Ensures the password hash cannot be null
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Prevents password_hash from being serialized to JSON on read
    private String password; // Store hashed passwords, never plain text

    private String email;
    private String firstName;
    private String lastName;
    private String role; // e.g., "SUPER_ADMIN", "CLINIC_MANAGER"

    // Lombok handles constructors, getters, and setters.
    // If not using Lombok, you'd add them manually:
    // public Long getId() { return id; }
    // public void setId(Long id) { this.id = id; }
    // public String getUsername() { return username; }
    // public void setUsername(String username) { this.username = username; }
    // public String getPassword() { return password; }
    // public void setPassword(String password) { this.password = password; }
    // etc.
}
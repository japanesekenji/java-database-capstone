package com.project.back_end.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull; // Added this import for @NotNull for dateOfBirth
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data; // From Lombok

import java.time.LocalDate; // Assuming date of birth might be a LocalDate

@Data // Generates getters, setters, equals, hashCode, and toString
@Entity // Marks this class as a JPA entity, mapped to a database table
public class Patient {
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID for MySQL
    private Long id;

    // Patient's full name is broken down into first and last name for better data structure
    // as per the MySQL schema design. If only 'name' is desired as a single field,
    // we would use just a 'name' field here. Sticking to firstName and lastName for consistency.
    @NotBlank(message = "First name cannot be empty")
    @Size(min = 3, max = 100, message = "First name must be between 3 and 100 characters")
    private String firstName; // Using firstName for consistency with Doctor model

    @NotBlank(message = "Last name cannot be empty")
    @Size(min = 3, max = 100, message = "Last name must be between 3 and 100 characters")
    private String lastName; // Using lastName for consistency with Doctor model

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be a valid email format")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    // Note: For a real application, consider using @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    // to hide the password from JSON responses and always hash passwords before saving to DB.
    private String password;

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
    private String phone; // Using 'phone' as per request, 'phoneNumber' is also common

    @NotBlank(message = "Address cannot be empty")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    // Lombok's @Data annotation automatically generates:
    // - Getters for all fields
    // - Setters for all fields
    // - A no-argument constructor
    // - equals(), hashCode(), and toString() methods
    //
    // If Lombok were not used, these methods would need to be manually implemented.
}
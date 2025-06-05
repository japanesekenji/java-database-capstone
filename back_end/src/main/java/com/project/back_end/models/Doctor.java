package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data; // From Lombok

import java.util.List;

@Data // Generates getters, setters, equals, hashCode, and toString
@Entity // Marks this class as a JPA entity, mapped to a database table
public class Doctor {
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID for MySQL
    private Long id;

    // Doctor's full name is broken down into first and last name for better data structure
    // Combine them in application logic if 'full name' is strictly needed.
    @NotBlank(message = "First name cannot be empty")
    @Size(min = 3, max = 100, message = "First name must be between 3 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(min = 3, max = 100, message = "Last name must be between 3 and 100 characters")
    private String lastName;

    @NotBlank(message = "Specialty cannot be empty")
    @Size(min = 3, max = 50, message = "Specialty must be between 3 and 50 characters")
    private String specialty;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be a valid email format")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Hides password from JSON responses
    private String password; // Store hashed passwords in real application, this is just for data transfer

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits")
    private String phone; // Using 'phone' as per request, 'phoneNumber' is also common

    @ElementCollection // Indicates that 'availableTimes' is a collection of basic types
    // This will create a separate table to store the elements of this collection.
    // For example, doctor_available_times with columns doctor_id, available_times
    private List<String> availableTimes; // Example: "09:00 -10:00"

    // Lombok's @Data annotation automatically generates:
    // - Getters for all fields
    // - Setters for all fields
    // - A no-argument constructor
    // - equals(), hashCode(), and toString() methods
    //
    // If Lombok were not used, these methods would need to be manually implemented.
}
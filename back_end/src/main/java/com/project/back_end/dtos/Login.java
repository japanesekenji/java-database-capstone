package com.project.back_end.dtos;

 // Place DTOs in the 'dtos' package

import lombok.Data; // From Lombok for generating getters and setters

@Data // Lombok annotation to automatically generate getter and setter methods for all fields
public class Login { // Renamed to LoginDTO for consistency with other DTOs
    private String email;    // The email address of the user attempting to log in
    private String password; // The password provided by the user

    // Lombok's @Data annotation automatically generates:
    // - Getters for 'email' and 'password'
    // - Setters for 'email' and 'password'
    // - A no-argument constructor
    // - equals(), hashCode(), and toString() methods
    //
    // This DTO is specifically for receiving login credentials from the frontend
    // and should not have any persistence annotations (@Entity, @Id, etc.).
    // It's commonly used as a @RequestBody in Spring Boot controller methods.
}

package com.project.back_end.controllers; // Controllers are typically placed in a 'controllers' package

import com.project.back_end.models.Admin; // Import the Admin model
import com.project.back_end.services.Service; // Import your central Service class
import org.springframework.beans.factory.annotation.Value; // Needed to inject properties like api.path
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for handling admin-related HTTP requests.
 * It primarily handles login functionality for administrators.
 */
@RestController // Designates this class as a REST controller
@RequestMapping("${api.path}" + "admin") // Sets the base URL path for this controller, e.g., /api/v1/admin
public class AdminController {

    private final Service service; // Autowire your central Service class

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject the Service instance.
     *
     * @param service The central Service instance to handle business logic.
     */
    public AdminController(Service service) {
        this.service = service;
    }

    /**
     * Handles admin login requests.
     * This endpoint expects an Admin object in the request body containing username and password.
     * It delegates the validation logic to the central Service class.
     *
     * @param admin The Admin object containing login credentials (username, password).
     * @return A ResponseEntity containing a token on successful login, or an error message on failure.
     */
    @PostMapping("/login") // Maps POST requests to /api/v1/admin/login
    public ResponseEntity<Map<String, String>> adminLogin(@RequestBody Admin admin) {
        // Call the validateAdmin method from the central Service to handle login validation
        return service.validateAdmin(admin);
    }
}

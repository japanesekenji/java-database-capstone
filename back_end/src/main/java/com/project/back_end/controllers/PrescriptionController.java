package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end.services.Service; // Central service for validation
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling prescription-related HTTP requests.
 * Provides endpoints for doctors to save and retrieve prescriptions.
 */
@RestController // Designates this class as a REST controller
@RequestMapping("${api.path}" + "prescription") // Sets the base URL path for this controller, e.g., /api/v1/prescription
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final Service service; // Central service for token validation and other common functionalities

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of PrescriptionService and the central Service.
     *
     * @param prescriptionService Service to handle prescription-specific business logic.
     * @param service             Central service for cross-cutting concerns like token validation.
     */
    public PrescriptionController(PrescriptionService prescriptionService, Service service) {
        this.prescriptionService = prescriptionService;
        this.service = service;
    }

    /**
     * Saves a new prescription to the system. This endpoint is accessible by doctors.
     *
     * @param token        The authentication token for the doctor.
     * @param prescription The Prescription object containing details to be saved, passed in the request body.
     * @return A ResponseEntity indicating success (HTTP 201 Created) or an error message (HTTP 401 Unauthorized, HTTP 500 Internal Server Error).
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> savePrescription(
            @PathVariable("token") String token,
            @RequestBody Prescription prescription) {

        Map<String, String> response = new HashMap<>();

        // 1. Validate token for doctor role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation; // Return UNAUTHORIZED or other token errors directly
        }

        // 2. Delegate to PrescriptionService to save the prescription
        int result = prescriptionService.savePrescription(prescription);
        if (result == 1) {
            response.put("message", "Prescription saved successfully.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } else {
            response.put("message", "Failed to save prescription due to an internal error.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves prescriptions associated with a specific appointment ID. This endpoint is accessible by doctors.
     *
     * @param appointmentId The ID of the appointment to retrieve prescriptions for (from path variable).
     * @param token         The authentication token for the doctor (from path variable).
     * @return A ResponseEntity containing a list of Prescription objects if found, or an error/not found message.
     */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<Map<String, Object>> getPrescriptionByAppointmentId(
            @PathVariable("appointmentId") Long appointmentId,
            @PathVariable("token") String token) {

        Map<String, Object> response = new HashMap<>();

        // 1. Validate token for doctor role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            Map<String, Object> errorResponse = new HashMap<>(tokenValidation.getBody());
            errorResponse.put("status", tokenValidation.getStatusCode().value());
            return new ResponseEntity<>(errorResponse, tokenValidation.getStatusCode());
        }

        // 2. Delegate to PrescriptionService to retrieve prescriptions
        List<Prescription> prescriptions = prescriptionService.getPrescriptionByAppointmentId(appointmentId);

        if (prescriptions.isEmpty()) {
            response.put("message", "No prescriptions found for appointment ID: " + appointmentId + ".");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } else {
            response.put("prescriptions", prescriptions);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }
}

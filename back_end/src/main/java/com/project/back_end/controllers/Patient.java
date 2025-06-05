package com.project.back_end.controllers;

import com.project.back_end.dtos.LoginDTO;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;
import com.project.back_end.services.Service; // Central service for validation
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for handling patient-related HTTP requests.
 * Provides endpoints for patient registration, login, fetching patient details,
 * and managing patient appointments (getting and filtering).
 */
@RestController // Designates this class as a REST controller
@RequestMapping("${api.path}" + "patient") // Sets the base URL path for this controller, e.g., /api/v1/patient
public class PatientController {

    private final PatientService patientService;
    private final Service service; // Central service for validation and common functionalities

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of PatientService and the central Service.
     *
     * @param patientService Service to handle patient-specific business logic.
     * @param service        Central service for cross-cutting concerns like token validation.
     */
    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    /**
     * Fetches the patient's details based on the provided JWT token.
     * This endpoint is accessible by authenticated patients.
     *
     * @param token The authentication token for the patient.
     * @return A ResponseEntity containing the patient's details or an error message.
     */
    @GetMapping("/{token}")
    public ResponseEntity<Map<String, Object>> getPatientDetails(@PathVariable("token") String token) {
        // Validate token for patient role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            Map<String, Object> errorResponse = new HashMap<>(tokenValidation.getBody());
            errorResponse.put("status", tokenValidation.getStatusCode().value());
            return new ResponseEntity<>(errorResponse, tokenValidation.getStatusCode());
        }

        // Delegate to PatientService to get patient details
        return patientService.getPatientDetails(token);
    }

    /**
     * Registers a new patient in the system.
     * This endpoint is publicly accessible for new user registration.
     *
     * @param patient The Patient object containing details for the new patient.
     * @return A ResponseEntity indicating success, conflict (patient exists), or internal error.
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createPatient(@RequestBody Patient patient) {
        Map<String, String> response = new HashMap<>();

        // Validate if the patient already exists by checking email or phone number
        if (!service.validatePatient(patient)) { // service.validatePatient returns true if patient DOES NOT exist
            response.put("message", "Patient with this email or phone number already exists.");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        int result = patientService.createPatient(patient);
        if (result == 1) {
            response.put("message", "Signup successful.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } else {
            response.put("message", "Internal server error during signup.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles patient login requests.
     * This endpoint expects a LoginDTO object in the request body containing email and password.
     * It delegates the validation logic to the central Service class.
     *
     * @param login The LoginDTO object containing patient login credentials.
     * @return A ResponseEntity containing a token on successful login, or an error message on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> patientLogin(@RequestBody LoginDTO login) {
        // Delegate to the central Service's validatePatientLogin method
        return service.validatePatientLogin(login);
    }

    /**
     * Retrieves a list of appointments for a specific patient.
     * This endpoint is accessible by authenticated patients.
     *
     * @param id    The ID of the patient (from path variable).
     * @param token The authentication token for the patient (from path variable).
     * @return A ResponseEntity containing the list of patient appointments or an error message.
     */
    @GetMapping("/{id}/{token}")
    public ResponseEntity<Map<String, Object>> getPatientAppointments(
            @PathVariable("id") Long id,
            @PathVariable("token") String token) {

        // Validate token for patient role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            Map<String, Object> errorResponse = new HashMap<>(tokenValidation.getBody());
            errorResponse.put("status", tokenValidation.getStatusCode().value());
            return new ResponseEntity<>(errorResponse, tokenValidation.getStatusCode());
        }

        // Delegate to PatientService to fetch appointments. PatientService will also internally
        // check if the 'id' path variable matches the user ID from the token for security.
        return patientService.getPatientAppointment(id, token);
    }

    /**
     * Filters patient appointments based on various criteria.
     * Path variables are used, and "all" can be passed for a criteria that should not be filtered.
     *
     * @param condition Optional: The condition to filter appointments by (e.g., "past", "future"). Pass "all" for no condition filter.
     * @param name      Optional: The name of the doctor to filter by (partial match). Pass "all" for no name filter.
     * @param token     The authentication token for the patient.
     * @return A ResponseEntity containing the filtered list of patient appointments or an error message.
     */
    @GetMapping("/filter/{condition}/{name}/{token}")
    public ResponseEntity<Map<String, Object>> filterPatientAppointments(
            @PathVariable("condition") String condition,
            @PathVariable("name") String name,
            @PathVariable("token") String token) {

        // Validate token for patient role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            Map<String, Object> errorResponse = new HashMap<>(tokenValidation.getBody());
            errorResponse.put("status", tokenValidation.getStatusCode().value());
            return new ResponseEntity<>(errorResponse, tokenValidation.getStatusCode());
        }

        // Normalize "all" or empty string to null for service layer
        String actualCondition = "all".equalsIgnoreCase(condition) || condition.trim().isEmpty() ? null : condition;
        String actualName = "all".equalsIgnoreCase(name) || name.trim().isEmpty() ? null : name;

        // Delegate to the central Service's filterPatient method
        // The central service handles getting the patientId from the token
        return service.filterPatient(actualCondition, actualName, token);
    }
}

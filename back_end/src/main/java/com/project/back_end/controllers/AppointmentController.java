package com.project.back_end.controllers;

import com.project.back_end.dtos.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service; // Central service for validation
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling appointment-related HTTP requests.
 * Provides endpoints for booking, retrieving, updating, and canceling appointments.
 */
@RestController // Designates this class as a REST controller
@RequestMapping("${api.path}" + "appointments") // Sets the base URL path for this controller, e.g., /api/v1/appointments
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service; // Central service for validation and token management

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of AppointmentService and the central Service.
     *
     * @param appointmentService Service to handle appointment-specific business logic.
     * @param service            Central service for cross-cutting concerns like token validation.
     */
    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    /**
     * Retrieves a list of appointments for a specific doctor on a given date,
     * with optional filtering by patient name. This endpoint is accessible by doctors.
     *
     * @param date        The date for which to retrieve appointments (formatted as YYYY-MM-DD in path).
     * @param patientName The name of the patient to filter by. Use "all" or an empty string
     * in the path variable if no patient name filter is desired.
     * @param token       The authorization token of the doctor.
     * @return A ResponseEntity containing a map with appointment data (list of AppointmentDTOs)
     * or an error message if validation or fetching fails.
     */
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<Map<String, Object>> getAppointments(
            @PathVariable("date") LocalDate date,
            @PathVariable("patientName") String patientName,
            @PathVariable("token") String token) {

        // Validate token for doctor role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "doctor");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            // If token validation fails, return the error response from the service.
            Map<String, Object> errorResponse = new HashMap<>(tokenValidation.getBody());
            errorResponse.put("status", tokenValidation.getStatusCode().value());
            return new ResponseEntity<>(errorResponse, tokenValidation.getStatusCode());
        }

        // Adjust patientName for the service method: if it's "all" or empty, treat as no filter (null).
        String actualPatientName = (patientName != null && ("all".equalsIgnoreCase(patientName.trim()) || patientName.trim().isEmpty()))
                ? null
                : patientName;

        // Fetch appointments using AppointmentService. The service returns a Map with data and status.
        Map<String, Object> serviceResponse = appointmentService.getAppointment(actualPatientName, date, token);

        // Extract the HTTP status code from the service response map.
        int statusCode = (int) serviceResponse.getOrDefault("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

        return new ResponseEntity<>(serviceResponse, httpStatus);
    }

    /**
     * Books a new appointment. This endpoint is accessible by patients.
     *
     * @param token       The authorization token of the patient.
     * @param appointment The Appointment object containing details for the new appointment
     * (doctor ID, patient ID, appointment time, etc.) in the request body.
     * @return A ResponseEntity indicating success (HTTP 201 Created) or various error statuses
     * (e.g., 401 Unauthorized, 404 Not Found, 409 Conflict, 400 Bad Request).
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> bookAppointment(
            @PathVariable("token") String token,
            @RequestBody Appointment appointment) {

        Map<String, String> response = new HashMap<>();

        // 1. Validate token for patient role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation; // Return UNAUTHORIZED or other token errors directly
        }

        // 2. Validate if the appointment time is available for the doctor and patient, and doctor exists.
        // The central Service's validateAppointment method returns:
        // - 1: if the appointment time is valid (doctor available, no patient conflict)
        // - 0: if the time is unavailable (doctor booked or patient has overlapping appointment)
        // - -1: if the doctor doesn't exist
        int validationResult = service.validateAppointment(appointment);

        if (validationResult == -1) { // Doctor doesn't exist
            response.put("message", "Doctor not found for the specified appointment.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } else if (validationResult == 0) { // Time unavailable (doctor booked or patient conflict)
            response.put("message", "Appointment time is unavailable for the selected doctor or you already have an overlapping appointment.");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        // 3. If validationResult is 1 (valid), proceed to book the appointment.
        int bookingResult = appointmentService.bookAppointment(appointment);
        if (bookingResult == 1) {
            response.put("message", "Appointment booked successfully.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } else {
            // Generic error during booking, typically due to an unexpected issue in the service.
            response.put("message", "Failed to book appointment due to an internal error or unhandled conflict.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // Or HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    /**
     * Updates an existing appointment. This endpoint is accessible by patients.
     * The patient identified by the token must be the one who booked the appointment.
     *
     * @param token       The authorization token of the patient.
     * @param appointment The Appointment object with updated details in the request body.
     * @return A ResponseEntity indicating success or failure of the update operation.
     * The AppointmentService will handle specific error messages and HTTP statuses (e.g., NOT_FOUND, CONFLICT).
     */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateAppointment(
            @PathVariable("token") String token,
            @RequestBody Appointment appointment) {

        // 1. Validate token for patient role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation; // Return UNAUTHORIZED or other token errors directly
        }

        // 2. Delegate the update logic to AppointmentService.
        // The service method is responsible for checking if the patient owns the appointment
        // and handling other business logic errors like time conflicts.
        return appointmentService.updateAppointment(appointment);
    }

    /**
     * Cancels an existing appointment. This endpoint is accessible by patients.
     * The patient identified by the token must be the one who booked the appointment.
     *
     * @param id    The ID of the appointment to cancel (from path variable).
     * @param token The authorization token of the patient (from path variable).
     * @return A ResponseEntity indicating success or failure of the cancellation operation.
     * The AppointmentService will handle specific error messages and HTTP statuses.
     */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> cancelAppointment(
            @PathVariable("id") long id,
            @PathVariable("token") String token) {

        // 1. Validate token for patient role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "patient");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation; // Return UNAUTHORIZED or other token errors directly
        }

        // 2. Delegate the cancellation logic to AppointmentService.
        // The service method is responsible for checking if the patient owns the appointment
        // and performing the deletion.
        return appointmentService.cancelAppointment(id, token);
    }
}

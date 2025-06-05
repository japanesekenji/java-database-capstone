package com.project.back_end.controllers;

import com.project.back_end.dtos.LoginDTO; // Assuming LoginDTO is defined
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service; // Central service for validation
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for handling doctor-related HTTP requests.
 * Provides endpoints for managing doctor information, including CRUD operations,
 * login, availability, and filtering.
 */
@RestController // Designates this class as a REST controller
@RequestMapping("${api.path}" + "doctor") // Sets the base URL path for this controller, e.g., /api/v1/doctor
public class DoctorController {

    private final DoctorService doctorService;
    private final Service service; // Central service for validation and filtering coordination

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of DoctorService and the central Service.
     *
     * @param doctorService Service to handle doctor-specific business logic.
     * @param service       Central service for cross-cutting concerns like token validation.
     */
    public DoctorController(DoctorService doctorService, Service service) {
        this.doctorService = doctorService;
        this.service = service;
    }

    /**
     * Retrieves the available time slots for a specific doctor on a given date.
     * Accessible by various user roles (doctor, patient, admin) for viewing availability.
     *
     * @param user      The role of the user requesting availability (e.g., "doctor", "patient", "admin").
     * @param doctorId  The unique ID of the doctor whose availability is being fetched.
     * @param date      The date for which the availability needs to be fetched (formatted as YYYY-MM-DD).
     * @param token     The authentication token for validating the user.
     * @return A ResponseEntity containing a map with the doctor's availability or an error message.
     */
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable("user") String user,
            @PathVariable("doctorId") Long doctorId,
            @PathVariable("date") LocalDate date,
            @PathVariable("token") String token) {

        // Validate token for the specified user role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, user);
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            Map<String, Object> errorResponse = new HashMap<>(tokenValidation.getBody());
            errorResponse.put("status", tokenValidation.getStatusCode().value());
            return new ResponseEntity<>(errorResponse, tokenValidation.getStatusCode());
        }

        List<String> availableSlots = doctorService.getDoctorAvailability(doctorId, date);
        Map<String, Object> response = new HashMap<>();
        response.put("availableTimes", availableSlots);
        response.put("status", HttpStatus.OK.value());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Retrieves a list of all doctors. This endpoint is generally public or accessible by all roles.
     *
     * @return A ResponseEntity containing a map with a list of all doctors.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDoctors() {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorService.getDoctors();
        response.put("doctors", doctors);
        response.put("status", HttpStatus.OK.value());
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Adds a new doctor to the system. This endpoint is accessible by administrators.
     *
     * @param token  The authentication token of the admin.
     * @param doctor The Doctor object containing the details of the doctor to be added.
     * @return A ResponseEntity indicating success, conflict (doctor exists), or internal error.
     */
    @PostMapping("/{token}")
    public ResponseEntity<Map<String, String>> addDoctor(
            @PathVariable("token") String token,
            @RequestBody Doctor doctor) {

        Map<String, String> response = new HashMap<>();

        // Validate token for admin role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "admin");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation; // Return UNAUTHORIZED or other token errors directly
        }

        int result = doctorService.saveDoctor(doctor);
        if (result == 1) {
            response.put("message", "Doctor added to db successfully.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } else if (result == -1) {
            response.put("message", "Doctor with this email already exists.");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        } else {
            response.put("message", "Some internal error occurred while adding doctor.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles doctor login requests.
     *
     * @param login The LoginDTO object containing the doctor's email and password.
     * @return A ResponseEntity containing a token on successful login, or an error message on failure.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> doctorLogin(@RequestBody LoginDTO login) {
        return doctorService.validateDoctor(login); // Delegate to DoctorService for login validation
    }

    /**
     * Updates the details of an existing doctor. This endpoint is accessible by administrators.
     *
     * @param token  The authentication token of the admin.
     * @param doctor The Doctor object with updated details in the request body.
     * @return A ResponseEntity indicating success, not found, or internal error.
     */
    @PutMapping("/{token}")
    public ResponseEntity<Map<String, String>> updateDoctor(
            @PathVariable("token") String token,
            @RequestBody Doctor doctor) {

        Map<String, String> response = new HashMap<>();

        // Validate token for admin role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "admin");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation; // Return UNAUTHORIZED or other token errors directly
        }

        int result = doctorService.updateDoctor(doctor);
        if (result == 1) {
            response.put("message", "Doctor updated successfully.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else if (result == -1) {
            response.put("message", "Doctor not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } else {
            response.put("message", "Some internal error occurred while updating doctor.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Deletes a doctor by their ID. This endpoint is accessible by administrators.
     *
     * @param id    The ID of the doctor to be deleted.
     * @param token The authentication token of the admin.
     * @return A ResponseEntity indicating success, not found, or internal error.
     */
    @DeleteMapping("/{id}/{token}")
    public ResponseEntity<Map<String, String>> deleteDoctor(
            @PathVariable("id") long id,
            @PathVariable("token") String token) {

        Map<String, String> response = new HashMap<>();

        // Validate token for admin role
        ResponseEntity<Map<String, String>> tokenValidation = service.validateToken(token, "admin");
        if (tokenValidation.getStatusCode() != HttpStatus.OK) {
            return tokenValidation; // Return UNAUTHORIZED or other token errors directly
        }

        int result = doctorService.deleteDoctor(id);
        if (result == 1) {
            response.put("message", "Doctor deleted successfully.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else if (result == -1) {
            response.put("message", "Doctor not found with id: " + id + ".");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        } else {
            response.put("message", "Some internal error occurred while deleting doctor.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Filters doctors based on optional criteria: name, available time (AM/PM), and specialty.
     * Path variables are used, and "all" can be passed for a criteria that should not be filtered.
     *
     * @param name       Optional: The name of the doctor (partial match). Pass "all" for no name filter.
     * @param time       Optional: The available time of the doctor ("AM" or "PM"). Pass "all" for no time filter.
     * @param speciality Optional: The specialty of the doctor. Pass "all" for no specialty filter.
     * @return A ResponseEntity containing a map with filtered doctor data.
     */
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filterDoctors(
            @PathVariable("name") String name,
            @PathVariable("time") String time,
            @PathVariable("speciality") String speciality) {

        // Normalize "all" or empty string to null for service layer
        String actualName = "all".equalsIgnoreCase(name) || name.trim().isEmpty() ? null : name;
        String actualTime = "all".equalsIgnoreCase(time) || time.trim().isEmpty() ? null : time;
        String actualSpeciality = "all".equalsIgnoreCase(speciality) || speciality.trim().isEmpty() ? null : speciality;

        // Delegate to the central Service's filterDoctor method
        Map<String, Object> serviceResponse = service.filterDoctor(actualName, actualSpeciality, actualTime);

        // Extract the HTTP status code from the service response map.
        int statusCode = (int) serviceResponse.getOrDefault("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

        return new ResponseEntity<>(serviceResponse, httpStatus);
    }
}

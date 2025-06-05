package com.project.back_end.services;

import com.project.back_end.dtos.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repositories.AppointmentRepository;
import com.project.back_end.repositories.PatientRepository;
import com.project.back_end.repositories.DoctorRepository; // Needed for populating DTOs
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository; // Needed for DTO mapping
    private final TokenService tokenService;

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of PatientRepository, AppointmentRepository,
     * DoctorRepository, and TokenService.
     *
     * @param patientRepository     Repository for Patient entities.
     * @param appointmentRepository Repository for Appointment entities.
     * @param doctorRepository      Repository for Doctor entities (to fetch details for DTOs).
     * @param tokenService          Service for handling user tokens.
     */
    public PatientService(PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          DoctorRepository doctorRepository,
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    /**
     * Saves a new patient to the database.
     *
     * @param patient The patient object to be saved.
     * @return 1 on success, and 0 on failure (e.g., database exception).
     */
    public int createPatient(Patient patient) {
        try {
            // Optional: Add logic to check if patient with same email/phone already exists
            patientRepository.save(patient);
            return 1; // Success
        } catch (Exception e) {
            System.err.println("Error creating patient: " + e.getMessage());
            return 0; // Failure
        }
    }

    /**
     * Retrieves a list of appointments for a specific patient.
     * Ensures the provided patient ID matches the one decoded from the token.
     *
     * @param id    The patient's ID.
     * @param token The JWT token containing the email/user ID.
     * @return A ResponseEntity containing a list of AppointmentDTOs or an error message.
     */
    public ResponseEntity<Map<String, Object>> getPatientAppointment(Long id, String token) {
        Map<String, Object> response = new HashMap<>();

        // 1. Validate token and get authenticated user ID
        Long authenticatedId = tokenService.getUserIdFromToken(token);
        if (authenticatedId == null) {
            response.put("message", "Invalid or expired token.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // 2. Check if the requested ID matches the authenticated ID
        if (!id.equals(authenticatedId)) {
            response.put("message", "Unauthorized access to appointments for this patient ID.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        // 3. Retrieve appointments for the patient
        List<Appointment> appointments = appointmentRepository.findByPatientId(id);

        // 4. Convert Appointment entities to AppointmentDTOs for frontend consumption
        List<AppointmentDTO> appointmentDTOs = appointments.stream().map(appointment ->
                new AppointmentDTO(
                        appointment.getId(),
                        appointment.getDoctor().getId(),
                        // Concatenate first and last name for doctorName
                        appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                        appointment.getPatient().getId(),
                        // Concatenate first and last name for patientName
                        appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                        appointment.getPatient().getEmail(),
                        appointment.getPatient().getPhone(), // Assuming 'phone' field in Patient model
                        appointment.getPatient().getAddress(),
                        appointment.getAppointmentTime(),
                        appointment.getStatus()
                )
        ).collect(Collectors.toList());

        response.put("appointments", appointmentDTOs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Filters appointments by condition (past or future) for a specific patient.
     * Uses appointment status (0 for future/scheduled, 1 for past/completed).
     *
     * @param condition The condition to filter by ("past" or "future").
     * @param id        The patient’s ID.
     * @return ResponseEntity containing the filtered appointments or an error message.
     */
    public ResponseEntity<Map<String, Object>> filterByCondition(String condition, Long id) {
        Map<String, Object> response = new HashMap<>();
        List<Appointment> appointments;

        // Determine status based on condition: 0 for scheduled (future), 1 for completed (past)
        int status;
        if ("past".equalsIgnoreCase(condition)) {
            status = 1; // Assuming 1 means 'Completed' and thus past
            // Or you could filter dynamically based on LocalDateTime.now()
            // appointments = appointmentRepository.findByPatientIdAndAppointmentTimeBeforeAndStatus(id, LocalDateTime.now(), 1);
        } else if ("future".equalsIgnoreCase(condition)) {
            status = 0; // Assuming 0 means 'Scheduled' and thus future
            // Or dynamically: appointmentRepository.findByPatientIdAndAppointmentTimeAfterAndStatus(id, LocalDateTime.now(), 0);
        } else {
            response.put("message", "Invalid condition. Please specify 'past' or 'future'.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        appointments = appointmentRepository.findByPatient_IdAndStatusOrderByAppointmentTimeAsc(id, status);

        List<AppointmentDTO> appointmentDTOs = appointments.stream().map(appointment ->
                new AppointmentDTO(
                        appointment.getId(),
                        appointment.getDoctor().getId(),
                        appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                        appointment.getPatient().getId(),
                        appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                        appointment.getPatient().getEmail(),
                        appointment.getPatient().getPhone(),
                        appointment.getPatient().getAddress(),
                        appointment.getAppointmentTime(),
                        appointment.getStatus()
                )
        ).collect(Collectors.toList());

        response.put("appointments", appointmentDTOs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Filters the patient's appointments by doctor's name.
     *
     * @param name      The name of the doctor (partial match).
     * @param patientId The ID of the patient.
     * @return ResponseEntity containing the filtered appointments or an error message.
     */
    public ResponseEntity<Map<String, Object>> filterByDoctor(String name, Long patientId) {
        Map<String, Object> response = new HashMap<>();
        List<Appointment> appointments = appointmentRepository.filterByDoctorNameAndPatientId(name, patientId);

        List<AppointmentDTO> appointmentDTOs = appointments.stream().map(appointment ->
                new AppointmentDTO(
                        appointment.getId(),
                        appointment.getDoctor().getId(),
                        appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                        appointment.getPatient().getId(),
                        appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                        appointment.getPatient().getEmail(),
                        appointment.getPatient().getPhone(),
                        appointment.getPatient().getAddress(),
                        appointment.getAppointmentTime(),
                        appointment.getStatus()
                )
        ).collect(Collectors.toList());

        response.put("appointments", appointmentDTOs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Filters the patient's appointments by doctor's name and appointment condition (past or future).
     *
     * @param condition The condition to filter by ("past" or "future").
     * @param name      The name of the doctor (partial match).
     * @param patientId The ID of the patient.
     * @return ResponseEntity containing the filtered appointments or an error message.
     */
    public ResponseEntity<Map<String, Object>> filterByDoctorAndCondition(String condition, String name, long patientId) {
        Map<String, Object> response = new HashMap<>();
        List<Appointment> appointments;

        int status;
        if ("past".equalsIgnoreCase(condition)) {
            status = 1; // Assuming 1 means 'Completed'
        } else if ("future".equalsIgnoreCase(condition)) {
            status = 0; // Assuming 0 means 'Scheduled'
        } else {
            response.put("message", "Invalid condition. Please specify 'past' or 'future'.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        appointments = appointmentRepository.filterByDoctorNameAndPatientIdAndStatus(name, patientId, status);

        List<AppointmentDTO> appointmentDTOs = appointments.stream().map(appointment ->
                new AppointmentDTO(
                        appointment.getId(),
                        appointment.getDoctor().getId(),
                        appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                        appointment.getPatient().getId(),
                        appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                        appointment.getPatient().getEmail(),
                        appointment.getPatient().getPhone(),
                        appointment.getPatient().getAddress(),
                        appointment.getAppointmentTime(),
                        appointment.getStatus()
                )
        ).collect(Collectors.toList());

        response.put("appointments", appointmentDTOs);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Fetches the patient's details based on the provided JWT token.
     *
     * @param token The JWT token containing the email/user ID.
     * @return ResponseEntity containing the patient's details or an error message.
     */
    public ResponseEntity<Map<String, Object>> getPatientDetails(String token) {
        Map<String, Object> response = new HashMap<>();

        // 1. Validate token and get authenticated user ID
        Long patientId = tokenService.getUserIdFromToken(token);
        if (patientId == null) {
            response.put("message", "Invalid or expired token.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // 2. Retrieve patient details from the repository
        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (!patientOptional.isPresent()) {
            response.put("message", "Patient not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Patient patient = patientOptional.get();
        // You might want to return a PatientDTO here if you want to hide sensitive fields like password
        // For now, returning the Patient object itself.
        response.put("patient", patient);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
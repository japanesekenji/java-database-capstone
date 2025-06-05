package com.project.back_end.services;

import com.project.back_end.dtos.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repositories.AppointmentRepository;
import com.project.back_end.repositories.DoctorRepository;
import com.project.back_end.repositories.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // For transactional operations

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors; // For mapping to DTOs

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TokenService tokenService; // Assuming TokenService exists for token validation

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of the repositories and TokenService.
     *
     * @param appointmentRepository Repository for Appointment entities.
     * @param patientRepository Repository for Patient entities.
     * @param doctorRepository Repository for Doctor entities.
     * @param tokenService Service for handling user tokens and extracting IDs.
     */
    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              TokenService tokenService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.tokenService = tokenService;
    }

    /**
     * Books a new appointment.
     * Performs validation to ensure doctor and patient exist, and no time conflicts.
     *
     * @param appointment The appointment object to book.
     * @return 1 if successful, 0 if there's an error (e.g., doctor/patient not found, conflict).
     */
    @Transactional // Ensures atomicity of the operation
    public int bookAppointment(Appointment appointment) {
        // 1. Validate Doctor and Patient existence
        Optional<Doctor> doctorOptional = doctorRepository.findById(appointment.getDoctor().getId());
        Optional<Patient> patientOptional = patientRepository.findById(appointment.getPatient().getId());

        if (!doctorOptional.isPresent()) {
            System.err.println("Error: Doctor not found for ID: " + appointment.getDoctor().getId());
            return 0; // Doctor not found
        }
        if (!patientOptional.isPresent()) {
            System.err.println("Error: Patient not found for ID: " + appointment.getPatient().getId());
            return 0; // Patient not found
        }

        // Set managed Doctor and Patient entities to the appointment to avoid detached entity issues
        appointment.setDoctor(doctorOptional.get());
        appointment.setPatient(patientOptional.get());

        // 2. Validate appointment time conflicts
        // An appointment is typically for 1 hour, so check if any existing appointment overlaps.
        // Get end time based on Appointment model's helper
        LocalDateTime proposedEndTime = appointment.getEndTime();

        // Check for doctor's existing appointments during the proposed time slot
        List<Appointment> doctorAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(
                        appointment.getDoctor().getId(),
                        appointment.getAppointmentTime().minusMinutes(59), // Start check slightly before
                        proposedEndTime.plusMinutes(59) // End check slightly after
                );

        boolean doctorConflict = doctorAppointments.stream().anyMatch(existingAppt ->
                // Check for overlap: (start1 < end2) and (end1 > start2)
                (appointment.getAppointmentTime().isBefore(existingAppt.getEndTime()) &&
                        proposedEndTime.isAfter(existingAppt.getAppointmentTime())) &&
                        !existingAppt.getId().equals(appointment.getId()) // Exclude itself if it's an update (though this is bookAppointment)
        );

        if (doctorConflict) {
            System.err.println("Error: Doctor is not available at the requested time.");
            return 0; // Doctor conflict
        }

        // Check for patient's existing appointments during the proposed time slot
        List<Appointment> patientAppointments = appointmentRepository
                .findByPatientIdAndStatusOrderByAppointmentTimeAsc(
                        appointment.getPatient().getId(), 0 // Assuming 0 means 'Scheduled'
                ).stream().filter(existingAppt ->
                        (appointment.getAppointmentTime().isBefore(existingAppt.getEndTime()) &&
                                proposedEndTime.isAfter(existingAppt.getAppointmentTime())) &&
                                !existingAppt.getId().equals(appointment.getId())
                ).collect(Collectors.toList());


        if (!patientAppointments.isEmpty()) {
            System.err.println("Error: Patient already has an appointment during the requested time.");
            return 0; // Patient conflict
        }

        try {
            appointmentRepository.save(appointment);
            return 1; // Success
        } catch (Exception e) {
            System.err.println("Error booking appointment: " + e.getMessage());
            return 0; // Generic save error
        }
    }

    /**
     * Updates an existing appointment.
     * Validates the appointment's existence and checks for time conflicts.
     *
     * @param appointment The appointment object with updated details.
     * @return A ResponseEntity indicating success or failure.
     */
    @Transactional
    public ResponseEntity<Map<String, String>> updateAppointment(Appointment appointment) {
        Map<String, String> response = new HashMap<>();

        // 1. Check if the appointment exists
        Optional<Appointment> existingAppointmentOptional = appointmentRepository.findById(appointment.getId());
        if (!existingAppointmentOptional.isPresent()) {
            response.put("message", "Appointment not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Appointment existingAppointment = existingAppointmentOptional.get();

        // 2. Validate Doctor and Patient existence if their IDs are changing
        // If IDs are not changing, ensure the objects are managed
        if (appointment.getDoctor() != null && appointment.getDoctor().getId() != null &&
                !existingAppointment.getDoctor().getId().equals(appointment.getDoctor().getId())) {
            Optional<Doctor> newDoctorOptional = doctorRepository.findById(appointment.getDoctor().getId());
            if (!newDoctorOptional.isPresent()) {
                response.put("message", "New doctor not found for ID: " + appointment.getDoctor().getId());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            existingAppointment.setDoctor(newDoctorOptional.get());
        } else {
            // Ensure existing doctor entity is managed
            existingAppointment.setDoctor(doctorRepository.findById(existingAppointment.getDoctor().getId()).orElse(null));
        }

        if (appointment.getPatient() != null && appointment.getPatient().getId() != null &&
                !existingAppointment.getPatient().getId().equals(appointment.getPatient().getId())) {
            Optional<Patient> newPatientOptional = patientRepository.findById(appointment.getPatient().getId());
            if (!newPatientOptional.isPresent()) {
                response.put("message", "New patient not found for ID: " + appointment.getPatient().getId());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            existingAppointment.setPatient(newPatientOptional.get());
        } else {
            // Ensure existing patient entity is managed
            existingAppointment.setPatient(patientRepository.findById(existingAppointment.getPatient().getId()).orElse(null));
        }

        // Update changeable fields (assuming ID, Doctor, Patient are handled above)
        if (appointment.getAppointmentTime() != null) {
            existingAppointment.setAppointmentTime(appointment.getAppointmentTime());
        }
        if (appointment.getStatus() != 0 || appointment.getStatus() == 0 && existingAppointment.getStatus() != 0) { // Allow updating to 0
            existingAppointment.setStatus(appointment.getStatus());
        }


        // 3. Validate for time conflicts after updating time
        LocalDateTime proposedStartTime = existingAppointment.getAppointmentTime();
        LocalDateTime proposedEndTime = existingAppointment.getEndTime(); // Uses helper to get end time

        // Check for doctor's existing appointments during the proposed time slot (excluding current appointment)
        List<Appointment> doctorAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(
                        existingAppointment.getDoctor().getId(),
                        proposedStartTime.minusMinutes(59), // Look slightly before for overlaps
                        proposedEndTime.plusMinutes(59)    // Look slightly after for overlaps
                ).stream().filter(existingAppt ->
                        !existingAppt.getId().equals(existingAppointment.getId()) && // Exclude the current appointment itself
                                (proposedStartTime.isBefore(existingAppt.getEndTime()) &&
                                        proposedEndTime.isAfter(existingAppt.getAppointmentTime()))
                ).collect(Collectors.toList());

        if (!doctorAppointments.isEmpty()) {
            response.put("message", "Doctor is not available at the requested updated time.");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        // Check for patient's existing appointments during the proposed time slot (excluding current appointment)
        List<Appointment> patientAppointments = appointmentRepository
                .findByPatientIdAndStatusOrderByAppointmentTimeAsc(
                        existingAppointment.getPatient().getId(), 0 // Assuming 0 means 'Scheduled'
                ).stream().filter(existingAppt ->
                        !existingAppt.getId().equals(existingAppointment.getId()) && // Exclude the current appointment itself
                                (proposedStartTime.isBefore(existingAppt.getEndTime()) &&
                                        proposedEndTime.isAfter(existingAppt.getAppointmentTime()))
                ).collect(Collectors.toList());

        if (!patientAppointments.isEmpty()) {
            response.put("message", "Patient already has an appointment during the requested updated time.");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        try {
            appointmentRepository.save(existingAppointment);
            response.put("message", "Appointment updated successfully.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("message", "Error updating appointment: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Cancels an existing appointment.
     * Ensures that the user attempting to cancel is the patient who booked it.
     *
     * @param id The ID of the appointment to cancel.
     * @param token The authorization token of the user attempting to cancel.
     * @return A ResponseEntity indicating success or failure.
     */
    @Transactional
    public ResponseEntity<Map<String, String>> cancelAppointment(long id, String token) {
        Map<String, String> response = new HashMap<>();

        // 1. Validate token and get user ID
        Long userId = tokenService.getUserIdFromToken(token);
        if (userId == null) {
            response.put("message", "Invalid or expired token.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // 2. Find the appointment
        Optional<Appointment> appointmentOptional = appointmentRepository.findById(id);
        if (!appointmentOptional.isPresent()) {
            response.put("message", "Appointment not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        Appointment appointment = appointmentOptional.get();

        // 3. Ensure the patient (from token) is the one who booked the appointment
        if (!appointment.getPatient().getId().equals(userId)) {
            response.put("message", "You are not authorized to cancel this appointment.");
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        try {
            appointmentRepository.delete(appointment);
            response.put("message", "Appointment cancelled successfully.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            response.put("message", "Error cancelling appointment: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves a list of appointments for a specific doctor on a specific date,
     * with optional filtering by patient name.
     *
     * @param patientName Patient name to filter by (can be null or empty for no filter).
     * @param date The specific date for which to retrieve appointments.
     * @param token The authorization token of the doctor.
     * @return A map containing the list of appointments (converted to DTOs) or an error message.
     */
    public Map<String, Object> getAppointment(String patientName, LocalDate date, String token) {
        Map<String, Object> response = new HashMap<>();

        // 1. Validate token and get doctor ID
        Long doctorId = tokenService.getUserIdFromToken(token); // Assuming token represents doctor's ID
        if (doctorId == null) {
            response.put("message", "Invalid or expired token.");
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            return response;
        }

        // Define the start and end of the day for the given date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX); // End of the day (23:59:59.999999999)

        List<Appointment> appointments;

        // 2. Fetch appointments based on patient name filter
        if (patientName != null && !patientName.trim().isEmpty()) {
            appointments = appointmentRepository
                    .findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                            doctorId, patientName.trim(), startOfDay, endOfDay);
        } else {
            appointments = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
        }

        // 3. Convert Appointment entities to AppointmentDTOs
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
        response.put("status", HttpStatus.OK.value());
        return response;
    }
}
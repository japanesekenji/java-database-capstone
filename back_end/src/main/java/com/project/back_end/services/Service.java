package com.project.back_end.services;

import com.project.back_end.dtos.LoginDTO;
import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repositories.AdminRepository;
import com.project.back_end.repositories.DoctorRepository;
import com.project.back_end.repositories.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service // Marks this class as a Spring Service component
public class Service { // Renamed from 'Service' to avoid conflicts with Java's Service class if any, though 'Service' is generally acceptable for a main service facade.

    // Declare necessary services and repositories as private final
    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DoctorService doctorService;
    private final PatientService patientService;

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of all required components.
     *
     * @param tokenService      Service for token generation and validation.
     * @param adminRepository   Repository for Admin entities.
     * @param doctorRepository  Repository for Doctor entities.
     * @param patientRepository Repository for Patient entities.
     * @param doctorService     Service for Doctor-related business logic.
     * @param patientService    Service for Patient-related business logic.
     */
    public Service(TokenService tokenService,
                   AdminRepository adminRepository,
                   DoctorRepository doctorRepository,
                   PatientRepository patientRepository,
                   DoctorService doctorService,
                   PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.doctorService = doctorService;
        this.patientService = patientService;
    }

    /**
     * Validates the authenticity and expiration of a given token for a specific user role.
     *
     * @param token The JWT token to be validated.
     * @param user  The expected user role (e.g., "admin", "doctor", "patient").
     * @return A ResponseEntity indicating success (OK) if the token is valid,
     * or an error message (UNAUTHORIZED) if invalid or expired.
     */
    public ResponseEntity<Map<String, String>> validateToken(String token, String user) {
        Map<String, String> response = new HashMap<>();
        // Assuming TokenService.validateToken returns true if valid, false otherwise.
        // And optionally, it might check the 'user' role within the token.
        if (tokenService.validateToken(token, user)) {
            response.put("message", "Token is valid.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("message", "Invalid or expired token.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Validates the login credentials for an administrator.
     *
     * @param receivedAdmin The Admin object containing username and password for validation.
     * @return A ResponseEntity with a generated JWT token if credentials are valid,
     * or an UNAUTHORIZED status with an error message otherwise.
     */
    public ResponseEntity<Map<String, String>> validateAdmin(Admin receivedAdmin) {
        Map<String, String> response = new HashMap<>();
        Admin foundAdmin = adminRepository.findByUsername(receivedAdmin.getUsername());

        if (foundAdmin == null) {
            response.put("message", "Invalid username or password.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // IMPORTANT: In a production application, you MUST hash and salt passwords.
        // Compare hashed password here, e.g., passwordEncoder.matches(receivedAdmin.getPassword(), foundAdmin.getPassword())
        if (!foundAdmin.getPassword().equals(receivedAdmin.getPassword())) {
            response.put("message", "Invalid username or password.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // Generate a token for the authenticated admin. Assuming ID is used for token generation.
        String token = tokenService.generateToken(foundAdmin.getId());
        response.put("message", "Admin login successful.");
        response.put("token", token);
        response.put("id", foundAdmin.getId().toString()); // Return admin's ID
        response.put("role", "admin"); // Indicate the role for frontend
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Filters doctors based on various criteria: name, specialty, and available time (AM/PM).
     * Delegates filtering logic to the DoctorService.
     *
     * @param name      Optional: The name of the doctor (partial match).
     * @param specialty Optional: The specialty of the doctor.
     * @param time      Optional: The available time of the doctor ("AM" or "PM").
     * @return A Map containing the filtered list of doctors and their status.
     */
    public Map<String, Object> filterDoctor(String name, String specialty, String time) {
        // Prioritize filters: name, specialty, then time
        if (name != null && !name.isEmpty()) {
            if (specialty != null && !specialty.isEmpty()) {
                if (time != null && !time.isEmpty()) {
                    // Filter by name, specialty, and time
                    return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
                }
                // Filter by name and specialty
                return doctorService.filterDoctorByNameAndSpecility(name, specialty);
            }
            if (time != null && !time.isEmpty()) {
                // Filter by name and time
                return doctorService.filterDoctorByNameAndTime(name, time);
            }
            // Filter by name only
            return doctorService.findDoctorByName(name);
        } else if (specialty != null && !specialty.isEmpty()) {
            if (time != null && !time.isEmpty()) {
                // Filter by specialty and time
                return doctorService.filterDoctorByTimeAndSpecility(specialty, time);
            }
            // Filter by specialty only
            return doctorService.filterDoctorBySpecility(specialty);
        } else if (time != null && !time.isEmpty()) {
            // Filter by time only
            return doctorService.filterDoctorsByTime(time);
        } else {
            // No filters provided, return all doctors
            Map<String, Object> response = new HashMap<>();
            response.put("doctors", doctorService.getDoctors());
            response.put("status", HttpStatus.OK.value());
            return response;
        }
    }

    /**
     * Validates whether a proposed appointment time is available based on the doctor's schedule.
     *
     * @param appointment The Appointment object to validate, containing doctor ID and proposed time.
     * @return 1 if the appointment time is valid (doctor is available),
     * 0 if the time is unavailable (doctor is booked),
     * -1 if the doctor doesn't exist.
     */
    public int validateAppointment(Appointment appointment) {
        if (appointment == null || appointment.getDoctor() == null || appointment.getDoctor().getId() == null ||
                appointment.getAppointmentTime() == null) {
            System.err.println("Validation Error: Appointment, Doctor, Doctor ID, or Appointment Time cannot be null.");
            return 0; // Invalid input
        }

        Optional<Doctor> doctorOptional = doctorRepository.findById(appointment.getDoctor().getId());
        if (!doctorOptional.isPresent()) {
            System.err.println("Validation Error: Doctor with ID " + appointment.getDoctor().getId() + " not found.");
            return -1; // Doctor doesn't exist
        }

        // Get available time slots for the doctor on the appointment's date
        // Note: doctorService.getDoctorAvailability returns slots like "HH:mm - HH:mm"
        List<String> availableSlots = doctorService.getDoctorAvailability(
                appointment.getDoctor().getId(),
                appointment.getAppointmentTime().toLocalDate()
        );

        LocalTime appointmentTimeOnly = appointment.getAppointmentTime().toLocalTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // Check if the exact appointment time (start time) falls within any available slot
        boolean isTimeAvailable = availableSlots.stream().anyMatch(slot -> {
            try {
                String[] times = slot.split(" - ");
                if (times.length == 2) {
                    LocalTime slotStart = LocalTime.parse(times[0], formatter);
                    LocalTime slotEnd = LocalTime.parse(times[1], formatter);

                    // An appointment time slot is typically 1 hour, so we check if the appointment's start time
                    // falls within any of the doctor's *remaining* available 1-hour blocks.
                    // For example, if a doctor has 09:00 - 10:00 available, an appointment at 09:00 fits.
                    // If the appointment duration is fixed (e.g., 1 hour), then:
                    // Check if appointmentTimeOnly is exactly slotStart and slotEnd is slotStart + 1 hour.
                    // This assumes getDoctorAvailability already accounts for the 1-hour blocks.
                    // For this simple check, we just see if the appointment starts exactly at an available slot start.
                    // A more robust check might consider the full duration of the new appointment relative to available slots.
                    return appointmentTimeOnly.equals(slotStart);
                }
            } catch (Exception e) {
                System.err.println("Error parsing available slot: " + slot + " - " + e.getMessage());
            }
            return false;
        });

        return isTimeAvailable ? 1 : 0; // 1 if available, 0 if unavailable
    }


    /**
     * Checks whether a patient already exists in the database based on their email or phone number.
     *
     * @param patient The Patient object to validate, typically containing email or phone.
     * @return true if the patient does NOT exist (meaning they can be created/registered),
     * false if the patient ALREADY exists (email or phone is already in use).
     */
    public boolean validatePatient(Patient patient) {
        // Attempt to find a patient by email or phone.
        // findByEmailOrPhone combines these checks using OR.
        Patient foundPatient = patientRepository.findByEmailOrPhone(patient.getEmail(), patient.getPhone());
        return foundPatient == null; // Returns true if no patient found (i.e., patient does not exist)
    }

    /**
     * Validates a patient's login credentials (email and password).
     *
     * @param login The LoginDTO containing the patient's email and password.
     * @return A ResponseEntity with a generated JWT token if login is valid,
     * or an UNAUTHORIZED status with an error message otherwise.
     */
    public ResponseEntity<Map<String, String>> validatePatientLogin(LoginDTO login) {
        Map<String, String> response = new HashMap<>();
        Patient patient = patientRepository.findByEmail(login.getEmail());

        if (patient == null) {
            response.put("message", "Invalid email or password.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // IMPORTANT: In a production application, you MUST hash and salt passwords.
        // Compare hashed password here, e.g., passwordEncoder.matches(login.getPassword(), patient.getPassword())
        if (!patient.getPassword().equals(login.getPassword())) {
            response.put("message", "Invalid email or password.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // Generate a token for the authenticated patient. Assuming ID is used for token generation.
        String token = tokenService.generateToken(patient.getId());
        response.put("message", "Patient login successful.");
        response.put("token", token);
        response.put("id", patient.getId().toString()); // Return patient's ID
        response.put("role", "patient"); // Indicate the role for frontend
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Filters patient appointments based on specified criteria (condition, doctor name).
     *
     * @param condition Optional: The medical condition to filter appointments by ("past", "future").
     * @param name      Optional: The doctor's name to filter appointments by (partial match).
     * @param token     The authentication token to identify the patient.
     * @return A ResponseEntity containing the filtered list of patient appointments (DTOs).
     */
    public ResponseEntity<Map<String, Object>> filterPatient(String condition, String name, String token) {
        // Extract patient ID from the token first
        Long patientId = tokenService.getUserIdFromToken(token);
        if (patientId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Invalid or expired token.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // Delegate to PatientService methods based on provided filters
        if (condition != null && !condition.isEmpty()) {
            if (name != null && !name.isEmpty()) {
                // Filter by doctor name and condition
                return patientService.filterByDoctorAndCondition(condition, name, patientId);
            }
            // Filter by condition only
            return patientService.filterByCondition(condition, patientId);
        } else if (name != null && !name.isEmpty()) {
            // Filter by doctor name only
            return patientService.filterByDoctor(name, patientId);
        } else {
            // No specific filters, return all appointments for the patient
            // patientService.getPatientAppointment already handles token validation internally for the requested ID
            return patientService.getPatientAppointment(patientId, token);
        }
    }
}

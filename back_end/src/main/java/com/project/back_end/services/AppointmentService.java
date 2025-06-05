package com.project.back_end.services;

import com.project.back_end.dtos.LoginDTO; // Assuming LoginDTO is used for login requests
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repositories.AppointmentRepository;
import com.project.back_end.repositories.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService; // Assuming TokenService for token generation/validation

    /**
     * Constructor for dependency injection.
     * Spring will automatically inject instances of DoctorRepository, AppointmentRepository,
     * and TokenService.
     *
     * @param doctorRepository      Repository for Doctor entities.
     * @param appointmentRepository Repository for Appointment entities.
     * @param tokenService          Service for handling tokens.
     */
    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * Fetches the available time slots for a specific doctor on a given date.
     * It filters out the booked slots from the doctor's general available slots.
     *
     * @param doctorId The ID of the doctor.
     * @param date     The date for which availability is needed.
     * @return A list of available time slots (e.g., "HH:mm - HH:mm") for the doctor on the specified date.
     */
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
        if (!doctorOptional.isPresent()) {
            return new ArrayList<>(); // Return empty list if doctor not found
        }
        Doctor doctor = doctorOptional.get();

        // Get all general available times for the doctor (assuming these are fixed patterns, e.g., "09:00 - 10:00")
        List<String> allAvailableSlots = doctor.getAvailableTimes();
        if (allAvailableSlots == null || allAvailableSlots.isEmpty()) {
            return new ArrayList<>(); // No general availability defined
        }

        // Get booked appointments for the doctor on the given date
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<Appointment> bookedAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);

        List<String> trulyAvailableSlots = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (String slotString : allAvailableSlots) {
            // Parse the general availability slot, e.g., "09:00 - 10:00"
            String[] times = slotString.split(" - ");
            if (times.length != 2) {
                continue; // Invalid slot format
            }
            LocalTime slotStart = LocalTime.parse(times[0], timeFormatter);
            LocalTime slotEnd = LocalTime.parse(times[1], timeFormatter);

            // Convert to LocalDateTime for comparison with appointmentTime
            LocalDateTime slotStartDateTime = date.atTime(slotStart);
            LocalDateTime slotEndDateTime = date.atTime(slotEnd);

            // Check if this general slot overlaps with any booked appointment
            boolean isBooked = bookedAppointments.stream().anyMatch(bookedAppt -> {
                LocalDateTime appointmentStart = bookedAppt.getAppointmentTime();
                LocalDateTime appointmentEnd = bookedAppt.getEndTime(); // Uses helper in Appointment model

                // Check for overlap: (start1 < end2) and (end1 > start2)
                return (slotStartDateTime.isBefore(appointmentEnd) && slotEndDateTime.isAfter(appointmentStart));
            });

            if (!isBooked) {
                trulyAvailableSlots.add(slotString);
            }
        }

        return trulyAvailableSlots;
    }


    /**
     * Saves a new doctor to the database.
     * Ensures that no duplicate doctor entries exist by email before saving.
     *
     * @param doctor The doctor object to save.
     * @return 1 for success, -1 if the doctor already exists (by email), 0 for internal errors.
     */
    @Transactional
    public int saveDoctor(Doctor doctor) {
        // Check if a doctor with the same email already exists
        if (doctorRepository.findByEmail(doctor.getEmail()) != null) {
            return -1; // Doctor with this email already exists
        }
        try {
            doctorRepository.save(doctor);
            return 1; // Success
        } catch (Exception e) {
            System.err.println("Error saving doctor: " + e.getMessage());
            return 0; // Internal error
        }
    }

    /**
     * Updates the details of an existing doctor.
     *
     * @param doctor The doctor object with updated details.
     * @return 1 for success, -1 if doctor not found, 0 for internal errors.
     */
    @Transactional
    public int updateDoctor(Doctor doctor) {
        Optional<Doctor> existingDoctorOptional = doctorRepository.findById(doctor.getId());
        if (!existingDoctorOptional.isPresent()) {
            return -1; // Doctor not found
        }

        Doctor existingDoctor = existingDoctorOptional.get();
        // Update fields: only update the ones that are provided and valid
        if (doctor.getFirstName() != null && !doctor.getFirstName().isEmpty()) {
            existingDoctor.setFirstName(doctor.getFirstName());
        }
        if (doctor.getLastName() != null && !doctor.getLastName().isEmpty()) {
            existingDoctor.setLastName(doctor.getLastName());
        }
        if (doctor.getSpecialty() != null && !doctor.getSpecialty().isEmpty()) {
            existingDoctor.setSpecialty(doctor.getSpecialty());
        }
        if (doctor.getEmail() != null && !doctor.getEmail().isEmpty()) {
            // Optional: Add logic to check if new email already exists for another doctor
            existingDoctor.setEmail(doctor.getEmail());
        }
        if (doctor.getPhone() != null && !doctor.getPhone().isEmpty()) {
            existingDoctor.setPhone(doctor.getPhone());
        }
        if (doctor.getPassword() != null && !doctor.getPassword().isEmpty()) {
            // In a real app, hash this password before saving
            existingDoctor.setPassword(doctor.getPassword());
        }
        if (doctor.getAvailableTimes() != null) {
            existingDoctor.setAvailableTimes(doctor.getAvailableTimes());
        }

        try {
            doctorRepository.save(existingDoctor);
            return 1; // Success
        } catch (Exception e) {
            System.err.println("Error updating doctor: " + e.getMessage());
            return 0; // Internal error
        }
    }

    /**
     * Retrieves a list of all doctors from the database.
     *
     * @return A list of all Doctor entities.
     */
    public List<Doctor> getDoctors() {
        return doctorRepository.findAll();
    }

    /**
     * Deletes a doctor by their ID.
     * All associated appointments for this doctor will also be deleted.
     *
     * @param id The ID of the doctor to be deleted.
     * @return 1 for success, -1 if doctor not found, 0 for internal errors.
     */
    @Transactional // Ensures atomicity for deleting doctor and their appointments
    public int deleteDoctor(long id) {
        Optional<Doctor> doctorOptional = doctorRepository.findById(id);
        if (!doctorOptional.isPresent()) {
            return -1; // Doctor not found
        }
        try {
            // Delete all appointments associated with this doctor first
            appointmentRepository.deleteAllByDoctorId(id);
            // Then delete the doctor
            doctorRepository.deleteById(id);
            return 1; // Success
        } catch (Exception e) {
            System.err.println("Error deleting doctor or associated appointments: " + e.getMessage());
            return 0; // Internal error
        }
    }

    /**
     * Validates a doctor's login credentials.
     *
     * @param login The LoginDTO object containing email and password.
     * @return A ResponseEntity with a token (on success) or an error message (on failure).
     */
    public ResponseEntity<Map<String, String>> validateDoctor(LoginDTO login) {
        Map<String, String> response = new HashMap<>();
        Doctor doctor = doctorRepository.findByEmail(login.getEmail());

        if (doctor == null) {
            response.put("message", "Invalid email or password.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // In a real application, compare hashed passwords using a BCryptPasswordEncoder or similar
        // For this example, we're doing a direct string comparison.
        if (doctor.getPassword() == null || !doctor.getPassword().equals(login.getPassword())) {
            response.put("message", "Invalid email or password.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        // Assuming tokenService can generate a token for the doctor's ID
        String token = tokenService.generateToken(doctor.getId());
        response.put("message", "Login successful.");
        response.put("token", token);
        response.put("id", doctor.getId().toString()); // Return doctor's ID
        response.put("role", "doctor"); // Indicate the role for frontend
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Finds doctors by their full name (first name + last name) partial match.
     *
     * @param name The partial name of the doctor to search for.
     * @return A map containing the list of doctors matching the name.
     */
    public Map<String, Object> findDoctorByName(String name) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        response.put("doctors", doctors);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    /**
     * Filters doctors by name, specialty, and availability during AM/PM.
     *
     * @param name      Doctor's name (partial).
     * @param specialty Doctor's specialty.
     * @param amOrPm    Time of day: "AM" or "PM".
     * @return A map with the filtered list of doctors.
     */
    public Map<String, Object> filterDoctorsByNameSpecilityandTime(String name, String specialty, String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        List<Doctor> filteredDoctors = filterDoctorByTime(doctors, amOrPm);
        response.put("doctors", filteredDoctors);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    /**
     * Filters doctors by name and their availability during AM/PM.
     *
     * @param name   Doctor's name (partial).
     * @param amOrPm Time of day: "AM" or "PM".
     * @return A map with the filtered list of doctors.
     */
    public Map<String, Object> filterDoctorByNameAndTime(String name, String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameLike(name);
        List<Doctor> filteredDoctors = filterDoctorByTime(doctors, amOrPm);
        response.put("doctors", filteredDoctors);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    /**
     * Filters doctors by name and specialty.
     *
     * @param name      Doctor's name (partial).
     * @param specialty Doctor's specialty.
     * @return A map with the filtered list of doctors.
     */
    public Map<String, Object> filterDoctorByNameAndSpecility(String name, String specialty) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        response.put("doctors", doctors);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    /**
     * Filters doctors by specialty and their availability during AM/PM.
     *
     * @param specialty Doctor's specialty.
     * @param amOrPm    Time of day: "AM" or "PM".
     * @return A map with the filtered list of doctors.
     */
    public Map<String, Object> filterDoctorByTimeAndSpecility(String specialty, String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        List<Doctor> filteredDoctors = filterDoctorByTime(doctors, amOrPm);
        response.put("doctors", filteredDoctors);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    /**
     * Filters doctors by specialty.
     *
     * @param specialty Doctor's specialty.
     * @return A map with the filtered list of doctors.
     */
    public Map<String, Object> filterDoctorBySpecility(String specialty) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        response.put("doctors", doctors);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    /**
     * Filters doctors by their availability during AM/PM (across all doctors).
     *
     * @param amOrPm Time of day: "AM" or "PM".
     * @return A map with the filtered list of doctors.
     */
    public Map<String, Object> filterDoctorsByTime(String amOrPm) {
        Map<String, Object> response = new HashMap<>();
        List<Doctor> allDoctors = doctorRepository.findAll();
        List<Doctor> filteredDoctors = filterDoctorByTime(allDoctors, amOrPm);
        response.put("doctors", filteredDoctors);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    /**
     * Private helper method to filter a list of doctors by their available times (AM/PM).
     * A doctor is included if at least one of their available slots falls within the specified AM/PM period.
     *
     * @param doctors The list of doctors to filter.
     * @param amOrPm  Time of day: "AM" or "PM". "AM" is 00:00 to 11:59, "PM" is 12:00 to 23:59.
     * @return A filtered list of doctors.
     */
    private List<Doctor> filterDoctorByTime(List<Doctor> doctors, String amOrPm) {
        if (amOrPm == null || doctors == null) {
            return doctors;
        }

        LocalTime amCutoff = LocalTime.of(12, 0); // 12:00 PM
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return doctors.stream().filter(doctor -> {
            if (doctor.getAvailableTimes() == null || doctor.getAvailableTimes().isEmpty()) {
                return false; // Doctor has no defined available times
            }
            return doctor.getAvailableTimes().stream().anyMatch(slotString -> {
                try {
                    String[] times = slotString.split(" - ");
                    if (times.length != 2) {
                        return false; // Invalid slot format
                    }
                    LocalTime slotStart = LocalTime.parse(times[0], timeFormatter);

                    if ("AM".equalsIgnoreCase(amOrPm)) {
                        return slotStart.isBefore(amCutoff); // Slot starts before 12 PM
                    } else if ("PM".equalsIgnoreCase(amOrPm)) {
                        return !slotStart.isBefore(amCutoff); // Slot starts at or after 12 PM
                    }
                    return false;
                } catch (Exception e) {
                    System.err.println("Error parsing time slot for doctor " + doctor.getId() + ": " + slotString + " - " + e.getMessage());
                    return false; // Skip invalid time slots
                }
            });
        }).collect(Collectors.toList());
    }
}

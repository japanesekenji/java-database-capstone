package com.project.back_end.dtos;

import lombok.Getter; // From Lombok for generating getters
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter // Lombok annotation to automatically generate getter methods for all fields
public class AppointmentDTO {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private String patientAddress;
    private LocalDateTime appointmentTime;
    private int status;
    private LocalDate appointmentDate;     // Derived from appointmentTime
    private LocalTime appointmentTimeOnly; // Derived from appointmentTime
    private LocalDateTime endTime;         // Derived from appointmentTime

    /**
     * Constructor to initialize core appointment fields and automatically compute
     * derived fields like appointmentDate, appointmentTimeOnly, and endTime.
     *
     * @param id The unique identifier for the appointment.
     * @param doctorId The ID of the doctor assigned to the appointment.
     * @param doctorName The full name of the doctor.
     * @param patientId The ID of the patient.
     * @param patientName The full name of the patient.
     * @param patientEmail The email address of the patient.
     * @param patientPhone The contact number of the patient.
     * @param patientAddress The residential address of the patient.
     * @param appointmentTime The full date and time of the appointment.
     * @param status The appointment status (e.g., scheduled, completed).
     */
    public AppointmentDTO(
            Long id,
            Long doctorId,
            String doctorName,
            Long patientId,
            String patientName,
            String patientEmail,
            String patientPhone,
            String patientAddress,
            LocalDateTime appointmentTime,
            int status) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.patientAddress = patientAddress;
        this.appointmentTime = appointmentTime;
        this.status = status;

        // Automatically compute derived fields
        if (appointmentTime != null) {
            this.appointmentDate = appointmentTime.toLocalDate();
            this.appointmentTimeOnly = appointmentTime.toLocalTime();
            this.endTime = appointmentTime.plusHours(1); // Assuming 1 hour appointment duration
        } else {
            this.appointmentDate = null;
            this.appointmentTimeOnly = null;
            this.endTime = null;
        }
    }
}

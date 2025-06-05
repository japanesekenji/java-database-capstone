package com.project.back_end.models;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data; // From Lombok

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data // Generates getters, setters, equals, hashCode, and toString
@Entity // Marks this class as a JPA entity, mapped to a database table
public class Appointment {
    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-generates ID for MySQL
    private Long id;

    @ManyToOne // Many appointments can be for one doctor
    @JoinColumn(name = "doctor_id", nullable = false) // Foreign key column in the database
    @NotNull(message = "Doctor cannot be null") // Ensures a doctor is assigned
    private Doctor doctor; // Links to Doctor entity

    @ManyToOne // Many appointments can be for one patient
    @JoinColumn(name = "patient_id", nullable = false) // Foreign key column in the database
    @NotNull(message = "Patient cannot be null") // Ensures a patient is assigned
    private Patient patient; // Links to Patient entity

    @NotNull(message = "Appointment time cannot be null") // Ensures the appointment has a time
    @Future(message = "Appointment time must be in the future") // Ensures the appointment is set for a future date/time
    private LocalDateTime appointmentTime; // The date and time of the appointment

    @NotNull(message = "Status cannot be null") // Ensures the appointment has a status
    private int status; // Status of the appointment (0 for Scheduled, 1 for Completed, etc.)

    // Helper Methods

    @Transient // Marks this method as not persistent in the database
    public LocalDateTime getEndTime() {
        // Returns the end time of the appointment (1 hour after start time)
        if (this.appointmentTime != null) {
            return this.appointmentTime.plusHours(1);
        }
        return null;
    }

    @Transient // Marks this method as not persistent in the database
    public LocalDate getAppointmentDate() {
        // Returns only the date portion of the appointment
        if (this.appointmentTime != null) {
            return this.appointmentTime.toLocalDate();
        }
        return null;
    }

    @Transient // Marks this method as not persistent in the database
    public LocalTime getAppointmentTimeOnly() {
        // Returns only the time portion of the appointment
        if (this.appointmentTime != null) {
            return this.appointmentTime.toLocalTime();
        }
        return null;
    }
}
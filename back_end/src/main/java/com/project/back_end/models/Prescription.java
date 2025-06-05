package com.project.back_end.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor; // For default constructor
import lombok.AllArgsConstructor; // For constructor with all fields
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data // Generates getters, setters, equals, hashCode, and toString
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields (useful for initialization)
@Document(collection = "prescriptions") // Maps this class to the 'prescriptions' MongoDB collection
public class Prescription {

    @Id // Marks this field as the primary key for MongoDB documents (_id)
    private String id; // MongoDB typically uses String or ObjectId for IDs

    @NotBlank(message = "Patient name cannot be empty")
    @Size(min = 3, max = 100, message = "Patient name must be between 3 and 100 characters")
    private String patientName;

    @NotNull(message = "Appointment ID cannot be null") // Required reference to a MySQL appointment
    @Field("appointment_id") // Maps Java field to MongoDB document field name if different
    private Long appointmentId; // Reference to the appointment entity's ID from MySQL

    @NotBlank(message = "Medication name cannot be empty")
    @Size(min = 3, max = 100, message = "Medication name must be between 3 and 100 characters")
    private String medication;

    @NotBlank(message = "Dosage details cannot be empty")
    @Size(min = 3, max = 20, message = "Dosage must be between 3 and 20 characters")
    private String dosage;

    @Size(max = 200, message = "Doctor notes cannot exceed 200 characters")
    @Field("doctor_notes") // Maps Java field to MongoDB document field name if different
    private String doctorNotes; // Optional field for any notes from the doctor

    // Constructor to initialize essential fields (as per task "Implement a constructor to initialize the most important fields")
    // Note: With @AllArgsConstructor and @NoArgsConstructor from Lombok, you get these automatically.
    // However, if you need a specific subset constructor, you'd define it manually.
    // For example:
    // public Prescription(String patientName, Long appointmentId, String medication, String dosage) {
    //     this.patientName = patientName;
    //     this.appointmentId = appointmentId;
    //     this.medication = medication;
    //     this.dosage = dosage;
    // }

    // Lombok's @Data annotation automatically generates:
    // - Getters for all fields
    // - Setters for all fields
    // - equals(), hashCode(), and toString() methods
}
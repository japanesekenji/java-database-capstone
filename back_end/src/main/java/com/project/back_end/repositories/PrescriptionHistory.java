package com.project.back_end.repositories;


import com.project.back_end.models.Prescription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for the Prescription MongoDB document.
 * Extends MongoRepository to inherit standard CRUD (Create, Read, Update, Delete)
 * operations for MongoDB collections without needing manual implementation.
 *
 * MongoRepository takes two generic arguments:
 * 1. The Document type it manages (Prescription in this case).
 * 2. The type of the Document's primary key (String for Prescription's 'id').
 */
@Repository // Indicates that this interface is a "Repository" component in the Spring application context
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {

    /**
     * Finds a list of Prescription documents associated with a specific appointment ID.
     * Spring Data MongoDB automatically generates the query based on the method name.
     * The 'appointmentId' field in the Prescription model corresponds to the 'appointment_id'
     * field in the MongoDB document.
     *
     * @param appointmentId The ID of the appointment to search for.
     * @return A list of Prescription documents matching the criteria.
     */
    List<Prescription> findByAppointmentId(Long appointmentId);
}

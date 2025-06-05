package com.project.back_end.repositories;

import com.project.back_end.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for the Patient entity.
 * Extends JpaRepository to inherit standard CRUD operations.
 *
 * JpaRepository takes two generic arguments:
 * 1. The Entity type it manages (Patient in this case).
 * 2. The type of the Entity's primary key (Long for Patient's 'id').
 */
@Repository // Indicates that this interface is a "Repository" component in the Spring application context
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Finds a Patient entity by their email address.
     * Spring Data JPA automatically generates the query based on the method name.
     *
     * @param email The email address to search for.
     * @return The Patient entity if found, or null if not found.
     */
    Patient findByEmail(String email);

    /**
     * Finds a Patient entity using either their email address or phone number.
     * Spring Data JPA automatically generates the query based on the method name.
     *
     * @param email The email address to search for.
     * @param phone The phone number to search for.
     * @return The Patient entity if found, or null if not found.
     */
    Patient findByEmailOrPhone(String email, String phone);
}
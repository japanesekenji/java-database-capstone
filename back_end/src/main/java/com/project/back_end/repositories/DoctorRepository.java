package com.project.back_end.repositories;

import com.project.back_end.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for the Doctor entity.
 * Extends JpaRepository to inherit standard CRUD operations.
 *
 * JpaRepository takes two generic arguments:
 * 1. The Entity type it manages (Doctor in this case).
 * 2. The type of the Entity's primary key (Long for Doctor's 'id').
 */
@Repository // Indicates that this interface is a "Repository" component in the Spring application context
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Finds a Doctor entity by their email address.
     * Spring Data JPA automatically generates the query based on the method name.
     *
     * @param email The email address to search for.
     * @return The Doctor entity if found, or null if not found.
     */
    Doctor findByEmail(String email);

    /**
     * Finds doctors by a partial match in their full name (first name + last name), case-insensitive.
     * Uses a custom JPQL query with CONCAT and LIKE for flexible pattern matching.
     *
     * @param name The partial name to search for.
     * @return A list of doctors matching the partial name.
     */
    @Query("SELECT d FROM Doctor d WHERE LOWER(CONCAT(d.firstName, ' ', d.lastName)) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Doctor> findByNameLike(@Param("name") String name);

    /**
     * Filters doctors by partial name and exact specialty, both case-insensitive.
     * Uses a custom JPQL query with LOWER, CONCAT, and LIKE for case-insensitive matching.
     *
     * @param name The partial name to search for.
     * @param specialty The specialty to filter by.
     * @return A list of doctors matching the name and specialty criteria.
     */
    @Query("SELECT d FROM Doctor d WHERE LOWER(CONCAT(d.firstName, ' ', d.lastName)) LIKE LOWER(CONCAT('%', :name, '%')) AND LOWER(d.specialty) = LOWER(:specialty)")
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(
            @Param("name") String name,
            @Param("specialty") String specialty);

    /**
     * Finds doctors by their specialty, ignoring case.
     * Spring Data JPA automatically generates the query based on the method name.
     *
     * @param specialty The specialty to search for.
     * @return A list of doctors with the specified specialty.
     */
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}
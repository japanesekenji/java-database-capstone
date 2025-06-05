package com.project.back_end.repositories; // Repositories are typically placed in a 'repositories' package

import com.project.back_end.models.Admin; // Import the Admin model
import org.springframework.data.jpa.repository.JpaRepository; // Import JpaRepository
import org.springframework.stereotype.Repository; // Optional, but good practice for clarity

/**
 * Repository interface for the Admin entity.
 * Extends JpaRepository to inherit standard CRUD (Create, Read, Update, Delete)
 * operations without needing manual implementation.
 *
 * JpaRepository takes two generic arguments:
 * 1. The Entity type it manages (Admin in this case).
 * 2. The type of the Entity's primary key (Long for Admin's 'id').
 */
@Repository // Indicates that this interface is a "Repository" component in the Spring application context
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Finds an Admin entity by their username.
     * Spring Data JPA automatically generates the query based on the method name.
     *
     * @param username The username to search for.
     * @return The Admin entity if found, or null if not found.
     */
    Admin findByUsername(String username);
}

package com.project.back_end.repositories;

import com.project.back_end.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Retrieves a list of appointments for a specific doctor within a given time range.
     * Uses LEFT JOIN FETCH to eagerly fetch associated Doctor and Patient entities,
     * which can help prevent N+1 select problems when accessing these relationships later.
     *
     * @param doctorId The ID of the doctor.
     * @param start The start of the time range (inclusive).
     * @param end The end of the time range (exclusive or inclusive depending on business logic).
     * @return A list of appointments matching the criteria.
     */
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH a.patient p WHERE a.doctor.id = :doctorId AND a.appointmentTime BETWEEN :start AND :end ORDER BY a.appointmentTime ASC")
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Filters appointments by doctor ID, partial patient name (case-insensitive), and time range.
     * Also uses LEFT JOIN FETCH to include patient and doctor details.
     *
     * @param doctorId The ID of the doctor.
     * @param patientName The partial patient name to search for (case-insensitive).
     * @param start The start of the time range.
     * @param end The end of the time range.
     * @return A list of appointments matching the criteria.
     */
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.patient p LEFT JOIN FETCH a.doctor d " +
            "WHERE a.doctor.id = :doctorId AND LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :patientName, '%')) " +
            "AND a.appointmentTime BETWEEN :start AND :end ORDER BY a.appointmentTime ASC")
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId,
            @Param("patientName") String patientName,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Deletes all appointments related to a specific doctor.
     * Requires @Modifying to indicate it's a DML (Data Manipulation Language) query
     * and @Transactional to ensure the operation runs within a transaction.
     *
     * @param doctorId The ID of the doctor whose appointments are to be deleted.
     */
    @Modifying // Indicates that the query will modify the database
    @Transactional // Ensures the operation runs within a transaction
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    /**
     * Finds all appointments for a specific patient.
     *
     * @param patientId The ID of the patient.
     * @return A list of appointments for the given patient.
     */
    List<Appointment> findByPatientId(Long patientId);

    /**
     * Retrieves appointments for a patient by their status, ordered by appointment time in ascending order.
     *
     * @param patientId The ID of the patient.
     * @param status The status of the appointments to retrieve.
     * @return A list of appointments matching the criteria, ordered by appointment time.
     */
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    /**
     * Searches appointments by partial doctor name (case-insensitive) and patient ID.
     * Uses LOWER and CONCAT for case-insensitive partial matching on the doctor's full name.
     *
     * @param doctorName The partial doctor name to search for.
     * @param patientId The ID of the patient.
     * @return A list of appointments matching the criteria.
     */
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH a.patient p " +
            "WHERE LOWER(CONCAT(d.firstName, ' ', d.lastName)) LIKE LOWER(CONCAT('%', :doctorName, '%')) " +
            "AND a.patient.id = :patientId ORDER BY a.appointmentTime ASC")
    List<Appointment> filterByDoctorNameAndPatientId(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId);

    /**
     * Filters appointments by partial doctor name (case-insensitive), patient ID, and status.
     *
     * @param doctorName The partial doctor name to search for.
     * @param patientId The ID of the patient.
     * @param status The status of the appointments.
     * @return A list of appointments matching the criteria.
     */
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH a.patient p " +
            "WHERE LOWER(CONCAT(d.firstName, ' ', d.lastName)) LIKE LOWER(CONCAT('%', :doctorName, '%')) " +
            "AND a.patient.id = :patientId AND a.status = :status ORDER BY a.appointmentTime ASC")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(
            @Param("doctorName") String doctorName,
            @Param("patientId") Long patientId,
            @Param("status") int status);
}

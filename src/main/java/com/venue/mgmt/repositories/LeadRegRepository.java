package com.venue.mgmt.repositories;

import com.venue.mgmt.entities.LeadRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRegRepository extends JpaRepository<LeadRegistration, Long> {
    Optional<LeadRegistration> findByFullName(String fullName);

    Optional<LeadRegistration> findByLeadId(Long leadId);

    @Query("SELECT l FROM LeadRegistration l WHERE l.createdBy = :userId")
    Page<LeadRegistration> findAllByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT l FROM LeadRegistration l WHERE l.createdBy = :userId and l.venue.venueId = :venueId")
    Page<LeadRegistration> findAllByUserIdAndVenueId(@Param("userId") String userId, @Param("venueId") Long venueId, Pageable pageable);

    @Query("SELECT l FROM LeadRegistration l WHERE l.createdBy = :userId and l.venue.venueId = :venueId and l.creationDate BETWEEN :startDate AND :endDate")
    Page<LeadRegistration> findAllByUserIdAndVenueIdAndCreationDateBetween(@Param("userId") String userId, @Param("venueId") Long venueId, @Param("startDate") Date startDate, @Param("endDate") Date endDate, Pageable pageable);
    @Query("SELECT l FROM LeadRegistration l WHERE l.createdBy = :userId and l.venue.venueId = :venueId and l.creationDate >= :startDate")
    Page<LeadRegistration> findAllByUserIdAndVenueIdAndCreationDateAfter(@Param("userId") String userId, @Param("venueId") Long venueId, @Param("startDate") Date startDate, Pageable pageable);

    @Query("SELECT l FROM LeadRegistration l WHERE l.createdBy = :userId and l.venue.venueId = :venueId and l.creationDate <= :endDate")
    Page<LeadRegistration> findAllByUserIdAndVenueIdAndCreationDateBefore(@Param("userId") String userId, @Param("venueId") Long venueId, @Param("endDate") Date endDate, Pageable pageable);
    
    @Query(value = "SELECT * FROM lead_registration l " +
           "WHERE l.is_active = true " +
            "AND l.created_by=:userId "+
           "AND (:searchTerm IS NULL OR TRIM(:searchTerm) = '' OR " +
           "     l.full_name ILIKE CONCAT('%', TRIM(:searchTerm), '%') OR " +
           "     l.email ILIKE CONCAT('%', TRIM(:searchTerm), '%') OR " +
           "     l.mobile_number ILIKE CONCAT('%', TRIM(:searchTerm), '%')" +
            ") " +
           "ORDER BY l.creation_date DESC", 
           nativeQuery = true)
    List<LeadRegistration> searchLeads(@Param("searchTerm") String searchTerm, @Param("userId") String userId);
}

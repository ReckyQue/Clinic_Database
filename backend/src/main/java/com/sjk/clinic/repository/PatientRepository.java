package com.sjk.clinic.repository;

import com.sjk.clinic.entity.Patient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    @Query("SELECT p FROM Patient p ORDER BY p.createTime DESC")
    List<Patient> findTop10ByOrderByCreateTimeDesc(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.createTime >= :dayStart AND p.createTime < :dayEnd")
    Long countTodayPatients(@Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd);

    @Query("SELECT p.diseaseType, COUNT(p) FROM Patient p GROUP BY p.diseaseType")
    List<Object[]> countByDiseaseType();

    @Query("SELECT "
            + "CASE "
            + "  WHEN p.age IS NULL OR p.age < 21 THEN '0-20岁' "
            + "  WHEN p.age BETWEEN 21 AND 40 THEN '21-40岁' "
            + "  WHEN p.age BETWEEN 41 AND 60 THEN '41-60岁' "
            + "  WHEN p.age BETWEEN 61 AND 80 THEN '61-80岁' "
            + "  ELSE '80岁以上' "
            + "END, COUNT(p) "
            + "FROM Patient p GROUP BY "
            + "CASE "
            + "  WHEN p.age IS NULL OR p.age < 21 THEN '0-20岁' "
            + "  WHEN p.age BETWEEN 21 AND 40 THEN '21-40岁' "
            + "  WHEN p.age BETWEEN 41 AND 60 THEN '41-60岁' "
            + "  WHEN p.age BETWEEN 61 AND 80 THEN '61-80岁' "
            + "  ELSE '80岁以上' "
            + "END")
    List<Object[]> countByAgeRange();
}

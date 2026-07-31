package com.sjk.clinic.repository;

import com.sjk.clinic.entity.DiagnosisRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisRecordRepository extends JpaRepository<DiagnosisRecord, Long> {

    @Query(value = "SELECT d FROM DiagnosisRecord d JOIN FETCH d.patient p WHERE "
            + "(:diseaseType IS NULL OR :diseaseType = '' OR d.diseaseType = :diseaseType) AND "
            + "(:startDate IS NULL OR d.diagnosisDate >= :startDate) AND "
            + "(:endDate IS NULL OR d.diagnosisDate <= :endDate) AND "
            + "(:keyword IS NULL OR :keyword = '' OR p.name LIKE CONCAT('%', :keyword, '%') "
            + "OR p.phone LIKE CONCAT('%', :keyword, '%')) AND "
            + "(:onlyOverdue = false OR (d.nextFollowUpDate IS NOT NULL AND d.nextFollowUpDate < CURRENT_DATE)) "
            + "ORDER BY d.diagnosisDate DESC, d.id DESC",
            countQuery = "SELECT COUNT(d) FROM DiagnosisRecord d JOIN d.patient p WHERE "
                    + "(:diseaseType IS NULL OR :diseaseType = '' OR d.diseaseType = :diseaseType) AND "
                    + "(:startDate IS NULL OR d.diagnosisDate >= :startDate) AND "
                    + "(:endDate IS NULL OR d.diagnosisDate <= :endDate) AND "
                    + "(:keyword IS NULL OR :keyword = '' OR p.name LIKE CONCAT('%', :keyword, '%') "
                    + "OR p.phone LIKE CONCAT('%', :keyword, '%')) AND "
                    + "(:onlyOverdue = false OR (d.nextFollowUpDate IS NOT NULL AND d.nextFollowUpDate < CURRENT_DATE))")
    Page<DiagnosisRecord> findBySearchCriteria(
            @Param("diseaseType") String diseaseType,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("keyword") String keyword,
            @Param("onlyOverdue") boolean onlyOverdue,
            Pageable pageable);

    @Query("SELECT d FROM DiagnosisRecord d JOIN FETCH d.patient p WHERE d.id = :id")
    Optional<DiagnosisRecord> findByIdWithPatient(@Param("id") Long id);

    @Query("SELECT d FROM DiagnosisRecord d JOIN FETCH d.patient p WHERE "
            + "(:patientName IS NULL OR :patientName = '' OR p.name LIKE CONCAT('%', :patientName, '%')) AND "
            + "(:diseaseType IS NULL OR :diseaseType = '' OR d.diseaseType = :diseaseType) "
            + "ORDER BY d.diagnosisDate DESC")
    List<DiagnosisRecord> findForExport(
            @Param("patientName") String patientName,
            @Param("diseaseType") String diseaseType);

    @Query("SELECT d FROM DiagnosisRecord d JOIN FETCH d.patient p WHERE p.id IN :patientIds "
            + "ORDER BY d.diagnosisDate DESC")
    List<DiagnosisRecord> findByPatientIds(@Param("patientIds") List<Long> patientIds);

    long countByDiagnosisDate(LocalDate diagnosisDate);

    long countByDiagnosisDateBetween(LocalDate startDate, LocalDate endDate);

    /** 每名患者仅取最新一条诊断（按日期，同日取最大 id），供首页/统计控制级别计算。 */
    @Query("SELECT d FROM DiagnosisRecord d JOIN FETCH d.patient p WHERE d.id IN ("
            + "SELECT MAX(d2.id) FROM DiagnosisRecord d2 WHERE d2.diagnosisDate = ("
            + "SELECT MAX(d3.diagnosisDate) FROM DiagnosisRecord d3 WHERE d3.patient = d2.patient"
            + ") GROUP BY d2.patient)")
    List<DiagnosisRecord> findLatestPerPatient();

    /** 仅投影诊断日期，供趋势计数，避免加载完整实体。 */
    @Query("SELECT d.diagnosisDate FROM DiagnosisRecord d WHERE d.diagnosisDate >= :start AND d.diagnosisDate <= :end")
    List<LocalDate> findDatesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 仅投影指标列，供异常计数，与 DiagnosisDto.buildAbnormalMetrics 阈值一致。 */
    @Query("SELECT d.systolicBp, d.diastolicBp, d.fastingGlucose, d.postprandialGlucose, "
            + "d.hba1c, d.heartRate, d.height, d.weight FROM DiagnosisRecord d")
    List<Object[]> findAllMetricColumns();

    @Query("SELECT d FROM DiagnosisRecord d JOIN FETCH d.patient p WHERE p.id IN :patientIds "
            + "AND d.diagnosisDate = ("
            + "SELECT MAX(d2.diagnosisDate) FROM DiagnosisRecord d2 WHERE d2.patient = d.patient"
            + ") AND d.id = ("
            + "SELECT MAX(d3.id) FROM DiagnosisRecord d3 WHERE d3.patient = d.patient "
            + "AND d3.diagnosisDate = d.diagnosisDate"
            + ")")
    List<DiagnosisRecord> findLatestByPatientIds(@Param("patientIds") List<Long> patientIds);
}

package com.sjk.clinic.repository;

import com.sjk.clinic.dto.PatientQuery;
import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PatientSpecifications {

    private PatientSpecifications() {
    }

    public static Specification<Patient> fromQuery(PatientQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(query.getName())) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + query.getName().toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(query.getPhone())) {
                predicates.add(cb.like(root.get("phone"), "%" + query.getPhone() + "%"));
            }
            if (StringUtils.hasText(query.getIdCard())) {
                predicates.add(cb.like(root.get("idCard"), "%" + query.getIdCard() + "%"));
            }
            if (StringUtils.hasText(query.getAddress())) {
                predicates.add(cb.like(root.get("address"), "%" + query.getAddress() + "%"));
            }
            if (StringUtils.hasText(query.getDiseaseType())) {
                predicates.add(cb.equal(root.get("diseaseType"), query.getDiseaseType()));
            }
            if (query.getCreateStart() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), query.getCreateStart()));
            }
            if (query.getCreateEnd() != null) {
                predicates.add(cb.lessThan(root.get("createTime"), query.getCreateEnd()));
            }

            if (query.isOnlyWithDiagnosis()) {
                Subquery<Long> existsSub = criteriaQuery.subquery(Long.class);
                Root<DiagnosisRecord> dr = existsSub.from(DiagnosisRecord.class);
                existsSub.select(cb.literal(1L));
                existsSub.where(cb.equal(dr.get("patient").get("id"), root.get("id")));
                predicates.add(cb.exists(existsSub));
            }

            if (query.getIds() != null && !query.getIds().isEmpty()) {
                predicates.add(root.get("id").in(query.getIds()));
            }

            if (query.getDiagnosisStart() != null || query.getDiagnosisEnd() != null) {
                Subquery<java.time.LocalDate> maxDateSub = criteriaQuery.subquery(java.time.LocalDate.class);
                Root<DiagnosisRecord> dr = maxDateSub.from(DiagnosisRecord.class);
                maxDateSub.select(cb.greatest(dr.<java.time.LocalDate>get("diagnosisDate")));
                maxDateSub.where(cb.equal(dr.get("patient").get("id"), root.get("id")));

                if (query.getDiagnosisStart() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(maxDateSub, query.getDiagnosisStart()));
                }
                if (query.getDiagnosisEnd() != null) {
                    predicates.add(cb.lessThanOrEqualTo(maxDateSub, query.getDiagnosisEnd()));
                }
            }

            criteriaQuery.orderBy(cb.desc(root.get("createTime")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}

package com.sjk.clinic.service.impl;

import com.sjk.clinic.dto.DiagnosisDto;
import com.sjk.clinic.dto.ExportFileResult;
import com.sjk.clinic.dto.PatientExportRequest;
import com.sjk.clinic.dto.PatientQuery;
import com.sjk.clinic.entity.DiagnosisRecord;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.repository.DiagnosisRecordRepository;
import com.sjk.clinic.repository.PatientRepository;
import com.sjk.clinic.repository.PatientSpecifications;
import com.sjk.clinic.service.ExportService;
import com.sjk.clinic.util.ControlLevelResolver;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<String> DEFAULT_FIELDS = List.of(
            "name", "gender", "age", "phone", "idCard", "address",
            "diseaseType", "lastDiagnosisDate", "symptoms", "medication",
            "healthGuidance", "bpGlucose",
            "createTime", "nextFollowUpDate", "doctor",
            "controlStatus");

    private static final Map<String, String> FIELD_LABELS = buildFieldLabels();

    private final PatientRepository patientRepository;
    private final DiagnosisRecordRepository diagnosisRecordRepository;

    @Override
    public byte[] exportPatients(String name, String phone, String idCard, String diseaseType, List<Long> ids)
            throws IOException {
        PatientExportRequest request = new PatientExportRequest();
        request.setName(name);
        request.setPhone(phone);
        request.setIdCard(idCard);
        request.setDiseaseType(diseaseType);
        request.setIds(ids != null ? ids : List.of());
        request.setFields(List.of(
                "name", "gender", "age", "phone", "idCard", "address", "diseaseType", "createTime"));
        request.setFormat("xlsx");
        return exportByFilter(request).getContent();
    }

    @Override
    public byte[] exportDiagnoses(String patientName, String diseaseType, List<Long> patientIds)
            throws IOException {
        List<DiagnosisRecord> records;
        if (patientIds != null && !patientIds.isEmpty()) {
            records = diagnosisRecordRepository.findByPatientIds(patientIds);
        } else {
            records = diagnosisRecordRepository.findForExport(patientName, diseaseType);
        }

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("诊断记录");
            Row header = sheet.createRow(0);
            String[] titles = {"ID", "患者姓名", "联系电话", "疾病类型", "诊断日期", "症状",
                    "诊断结果", "治疗方案", "医生", "备注", "创建时间"};
            for (int i = 0; i < titles.length; i++) {
                header.createCell(i).setCellValue(titles[i]);
            }

            int rowIndex = 1;
            for (DiagnosisRecord record : records) {
                Patient patient = record.getPatient();
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(record.getId() != null ? record.getId() : 0);
                row.createCell(1).setCellValue(patient != null ? defaultString(patient.getName()) : "");
                row.createCell(2).setCellValue(patient != null ? defaultString(patient.getPhone()) : "");
                row.createCell(3).setCellValue(defaultString(record.getDiseaseType()));
                row.createCell(4).setCellValue(formatDate(record.getDiagnosisDate()));
                row.createCell(5).setCellValue(defaultString(record.getSymptoms()));
                row.createCell(6).setCellValue(defaultString(record.getDiagnosis()));
                row.createCell(7).setCellValue(defaultString(record.getTreatment()));
                row.createCell(8).setCellValue(defaultString(record.getDoctor()));
                row.createCell(9).setCellValue(defaultString(record.getRemark()));
                row.createCell(10).setCellValue(formatDateTime(record.getCreateTime()));
            }
            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        }
    }

    @Override
    public ExportFileResult exportByFilter(PatientExportRequest request) throws IOException {
        List<String> fields = normalizeFields(request.getFields());
        List<Patient> patients = loadPatients(request);
        Map<Long, List<DiagnosisRecord>> diagnosisMap = loadDiagnoses(patients, request.isExportAllHistory());

        List<List<String>> rows = new ArrayList<>();
        rows.add(fields.stream().map(FIELD_LABELS::get).toList());

        for (Patient patient : patients) {
            List<DiagnosisRecord> diagnoses = diagnosisMap.getOrDefault(patient.getId(), List.of());
            if (request.isExportAllHistory() && !diagnoses.isEmpty()) {
                for (DiagnosisRecord diagnosis : diagnoses) {
                    rows.add(buildRow(patient, diagnosis, fields, request.isMaskSensitive()));
                }
            } else {
                DiagnosisRecord latest = diagnoses.isEmpty() ? null : diagnoses.get(0);
                rows.add(buildRow(patient, latest, fields, request.isMaskSensitive()));
            }
        }

        String format = "csv".equalsIgnoreCase(request.getFormat()) ? "csv" : "xlsx";
        String baseName = StringUtils.hasText(request.getFilename())
                ? request.getFilename().replaceAll("[\\\\/:*?\"<>|]", "_")
                : "乡村慢病_患者列表_" + LocalDate.now().format(FILE_DATE);

        if ("csv".equals(format)) {
            return new ExportFileResult(
                    toCsv(rows),
                    ensureExt(baseName, ".csv"),
                    "text/csv;charset=UTF-8");
        }
        return new ExportFileResult(
                toXlsx(rows),
                ensureExt(baseName, ".xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private List<Patient> loadPatients(PatientExportRequest request) {
        if (!CollectionUtils.isEmpty(request.getIds())) {
            return new ArrayList<>(patientRepository.findAllById(request.getIds()));
        }
        PatientQuery query = PatientQuery.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .idCard(request.getIdCard())
                .address(request.getAddress())
                .diseaseType(request.getDiseaseType())
                .createStart(request.getCreateStart() != null ? request.getCreateStart().atStartOfDay() : null)
                .createEnd(request.getCreateEnd() != null
                        ? request.getCreateEnd().plusDays(1).atStartOfDay() : null)
                .diagnosisStart(request.getDiagnosisStart())
                .diagnosisEnd(request.getDiagnosisEnd())
                .onlyWithDiagnosis(request.isOnlyWithDiagnosis())
                .page(1)
                .size(10000)
                .build();
        return patientRepository.findAll(PatientSpecifications.fromQuery(query), PageRequest.of(0, 10000))
                .getContent();
    }

    private Map<Long, List<DiagnosisRecord>> loadDiagnoses(List<Patient> patients, boolean exportAllHistory) {
        if (patients.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = patients.stream().map(Patient::getId).toList();
        // 分批查询，避免超大 IN 列表
        Map<Long, List<DiagnosisRecord>> result = new LinkedHashMap<>();
        final int batchSize = 200;
        for (int i = 0; i < ids.size(); i += batchSize) {
            List<Long> batch = ids.subList(i, Math.min(i + batchSize, ids.size()));
            List<DiagnosisRecord> records = exportAllHistory
                    ? diagnosisRecordRepository.findByPatientIds(batch)
                    : diagnosisRecordRepository.findLatestByPatientIds(batch);
            for (DiagnosisRecord record : records) {
                if (record.getPatient() == null || record.getPatient().getId() == null) {
                    continue;
                }
                result.computeIfAbsent(record.getPatient().getId(), k -> new ArrayList<>()).add(record);
            }
        }
        if (exportAllHistory) {
            for (List<DiagnosisRecord> list : result.values()) {
                list.sort(Comparator.comparing(DiagnosisRecord::getDiagnosisDate,
                        Comparator.nullsLast(Comparator.reverseOrder())));
            }
        }
        return result;
    }

    private List<String> buildRow(
            Patient patient, DiagnosisRecord diagnosis, List<String> fields, boolean maskSensitive) {
        List<String> row = new ArrayList<>();
        for (String field : fields) {
            row.add(resolveField(patient, diagnosis, field, maskSensitive));
        }
        return row;
    }

    private String resolveField(
            Patient patient, DiagnosisRecord diagnosis, String field, boolean maskSensitive) {
        return switch (field) {
            case "name" -> defaultString(patient.getName());
            case "gender" -> patient.getGender() != null ? patient.getGender().getDisplayName() : "";
            case "age" -> patient.getAge() != null ? String.valueOf(patient.getAge()) : "";
            case "phone" -> maskSensitive ? maskPhone(patient.getPhone()) : defaultString(patient.getPhone());
            case "idCard" -> maskSensitive ? maskIdCard(patient.getIdCard()) : defaultString(patient.getIdCard());
            case "address" -> defaultString(patient.getAddress());
            case "diseaseType" -> diagnosis != null && StringUtils.hasText(diagnosis.getDiseaseType())
                    ? diagnosis.getDiseaseType() : defaultString(patient.getDiseaseType());
            case "lastDiagnosisDate" -> diagnosis != null ? formatDate(diagnosis.getDiagnosisDate()) : "";
            case "symptoms" -> diagnosis != null ? defaultString(diagnosis.getSymptoms()) : "";
            case "medication" -> diagnosis != null ? defaultString(diagnosis.getMedication()) : "";
            case "healthGuidance" -> diagnosis != null ? defaultString(diagnosis.getHealthGuidance()) : "";
            case "bpGlucose" -> buildMetrics(diagnosis);
            case "createTime" -> formatDateTime(patient.getCreateTime());
            case "nextFollowUpDate" -> diagnosis != null ? formatDate(diagnosis.getNextFollowUpDate()) : "";
            case "doctor" -> diagnosis != null ? defaultString(diagnosis.getDoctor()) : "";
            case "controlStatus" -> resolveControlLabel(diagnosis);
            default -> "";
        };
    }

    private String buildMetrics(DiagnosisRecord diagnosis) {
        if (diagnosis == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (diagnosis.getSystolicBp() != null && diagnosis.getDiastolicBp() != null) {
            parts.add("血压" + diagnosis.getSystolicBp() + "/" + diagnosis.getDiastolicBp());
        }
        if (diagnosis.getFastingGlucose() != null) {
            parts.add("空腹血糖" + diagnosis.getFastingGlucose());
        }
        if (diagnosis.getPostprandialGlucose() != null) {
            parts.add("餐后" + diagnosis.getPostprandialGlucose());
        }
        if (diagnosis.getHba1c() != null) {
            parts.add("HbA1c" + diagnosis.getHba1c() + "%");
        }
        return String.join("；", parts);
    }

    private String resolveControlLabel(DiagnosisRecord diagnosis) {
        if (diagnosis == null) {
            return "未诊断";
        }
        return ControlLevelResolver.toLabel(
                ControlLevelResolver.resolveFinalLevel(DiagnosisDto.fromEntity(diagnosis)));
    }

    private List<String> normalizeFields(List<String> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return new ArrayList<>(DEFAULT_FIELDS);
        }
        return fields.stream().filter(FIELD_LABELS::containsKey).distinct().toList();
    }

    private byte[] toXlsx(List<List<String>> rows) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("导出数据");
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i);
                List<String> values = rows.get(i);
                for (int j = 0; j < values.size(); j++) {
                    row.createCell(j).setCellValue(Objects.toString(values.get(j), ""));
                }
            }
            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        }
    }

    private byte[] toCsv(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        for (List<String> row : rows) {
            builder.append(row.stream().map(this::csvEscape).collect(Collectors.joining(",")))
                    .append("\r\n");
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvEscape(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String ensureExt(String name, String ext) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".xlsx") || lower.endsWith(".csv")) {
            return name.substring(0, name.lastIndexOf('.')) + ext;
        }
        return name + ext;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return defaultString(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || idCard.length() < 8) {
            return defaultString(idCard);
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private String defaultString(String value) {
        return value != null ? value : "";
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }

    private static Map<String, String> buildFieldLabels() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("name", "姓名");
        map.put("gender", "性别");
        map.put("age", "年龄");
        map.put("phone", "联系电话");
        map.put("idCard", "身份证号");
        map.put("address", "家庭住址");
        map.put("diseaseType", "疾病类型");
        map.put("lastDiagnosisDate", "最近诊断日期");
        map.put("symptoms", "当前症状");
        map.put("medication", "用药情况");
        map.put("healthGuidance", "健康指导");
        map.put("bpGlucose", "血压/血糖值");
        map.put("createTime", "收录时间");
        map.put("nextFollowUpDate", "下次随访日期");
        map.put("doctor", "主治村医");
        map.put("controlStatus", "控制情况");
        return map;
    }
}

package com.sjk.clinic.converter;

import com.sjk.clinic.entity.Patient;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Patient.Gender, String> {
    
    @Override
    public String convertToDatabaseColumn(Patient.Gender gender) {
        if (gender == null) {
            return null;
        }
        return gender.getDisplayName();
    }
    
    @Override
    public Patient.Gender convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        for (Patient.Gender gender : Patient.Gender.values()) {
            // 兼容库中「男/女」与历史枚举名 MALE/FEMALE
            if (gender.getDisplayName().equals(dbData) || gender.name().equalsIgnoreCase(dbData)) {
                return gender;
            }
        }
        return null;
    }
}

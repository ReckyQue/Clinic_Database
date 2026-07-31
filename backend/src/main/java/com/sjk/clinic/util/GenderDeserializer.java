package com.sjk.clinic.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sjk.clinic.entity.Patient;

import java.io.IOException;

public class GenderDeserializer extends JsonDeserializer<Patient.Gender> {
    
    @Override
    public Patient.Gender deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String value = p.getText();
        if (value == null || value.isEmpty()) {
            return null;
        }
        for (Patient.Gender gender : Patient.Gender.values()) {
            if (gender.getDisplayName().equals(value) || gender.name().equals(value)) {
                return gender;
            }
        }
        return null;
    }
}

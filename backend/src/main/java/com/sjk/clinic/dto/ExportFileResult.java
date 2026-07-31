package com.sjk.clinic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExportFileResult {
    private byte[] content;
    private String filename;
    private String contentType;
}

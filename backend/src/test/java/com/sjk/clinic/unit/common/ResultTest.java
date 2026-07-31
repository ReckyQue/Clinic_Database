package com.sjk.clinic.unit.common;

import com.sjk.clinic.common.Result;
import com.sjk.clinic.common.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResultTest {

    @Test
    void successWrapsData() {
        Result<String> result = Result.success("ok");
        assertEquals(ResultCode.SUCCESS.getCode(), result.getCode());
        assertEquals("ok", result.getData());
    }

    @Test
    void errorUsesCode() {
        Result<Void> result = Result.error(ResultCode.BAD_REQUEST);
        assertEquals(ResultCode.BAD_REQUEST.getCode(), result.getCode());
        assertNull(result.getData());
    }

    @Test
    void errorMessageOnly() {
        Result<Void> result = Result.error("boom");
        assertEquals("boom", result.getMessage());
    }
}

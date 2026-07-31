package com.sjk.clinic.unit.service;

import com.sjk.clinic.dto.PatientQuery;
import com.sjk.clinic.entity.Patient;
import com.sjk.clinic.repository.PatientRepository;
import com.sjk.clinic.service.impl.PatientServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientServiceImpl patientService;

    @Test
    void getPatientsClampsPageSize() {
        when(patientRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PatientQuery query = PatientQuery.builder().page(1).size(5000).build();
        patientService.getPatients(query);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientRepository).findAll(any(Specification.class), captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
        assertEquals(100, query.getSize());
    }

    @Test
    void getTotalPatientsDelegatesToRepository() {
        when(patientRepository.count()).thenReturn(12L);
        assertEquals(12L, patientService.getTotalPatients());
    }
}

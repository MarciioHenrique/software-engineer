package com.mirna.hospitalmanagementapi.domain.dtos.doctor;

import java.time.LocalDateTime;
import com.mirna.hospitalmanagementapi.domain.enums.Specialty;


public record DoctorVerificationDTO(
    Long doctorId,
    LocalDateTime consultationDate,
    Specialty specialty
){}
package com.course.registration.enrollment;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class CreateEnrollmentRequest {
    @NotNull
    private Long studentId;
}
// src/main/java/com/course/registration/course/UpdateCourseRequest.java
package com.course.registration.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCourseRequest {
    @NotNull
    private Long termId;

    @NotBlank @Size(max = 100)
    private String courseName;

    @NotNull @Min(0)
    private Integer capacity;

    @NotNull
    private Long professorId;
}

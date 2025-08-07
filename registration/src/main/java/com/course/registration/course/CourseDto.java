package com.course.registration.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDto {
    private Long id;

    @NotNull
    private Long termId;

    @NotBlank
    @Size(max = 50)
    private String courseCode;

    @NotBlank @Size(max = 100)
    private String courseName;

    @NotNull @Min(0)
    private Integer capacity;

    private Integer enrolledCount;

    @NotNull
    private Long professorId;

    private LocalDateTime createdAt;

    private List<LectureTimeDto> lectureTimes;
}

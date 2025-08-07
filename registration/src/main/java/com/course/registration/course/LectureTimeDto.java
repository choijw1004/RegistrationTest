// src/main/java/com/course/registration/course/LectureTimeDto.java
package com.course.registration.course;

import lombok.*;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureTimeDto {
    private Long id;
    private Long courseId;
    private int dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
}

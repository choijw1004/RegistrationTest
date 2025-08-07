// src/main/java/com/course/registration/waitlist/WaitlistDto.java
package com.course.registration.waitlist;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WaitlistDto {
    private Long id;

    @NotNull
    private Long studentId;

    @NotNull
    private Long courseId;

    private Integer waitOrder;

    private LocalDateTime createdAt;
}

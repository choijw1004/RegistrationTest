// src/main/java/com/course/registration/enrollment/EnrollmentController.java
package com.course.registration.enrollment;

import com.course.registration.waitlist.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class EnrollmentController {
    private final EnrollmentService service;
    private final WaitlistService waitlistService;

    @PostMapping("/courses/{courseId}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public Object enrollOrWait(
            @PathVariable Long courseId,
            @RequestBody @Valid CreateEnrollmentRequest req
    ) {
        try {
            return service.enroll(courseId, req.getStudentId());
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT
                    && "Course is full".equals(ex.getReason())) {
                return waitlistService.addToWaitlist(courseId, req.getStudentId());
            }
            throw ex;
        }
    }

    @DeleteMapping("/enrollments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id) {
        service.cancel(id);
    }

    // GET 메서드(listByStudent, listByCourse) 추가 구현 가능
}

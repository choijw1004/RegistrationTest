// src/main/java/com/course/registration/enrollment/EnrollmentService.java
package com.course.registration.enrollment;

import com.course.registration.course.Course;
import com.course.registration.course.CourseRepository;
import com.course.registration.user.User;
import com.course.registration.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EnrollmentService {
    private final EnrollmentRepository enrollRepo;
    private final CourseRepository courseRepo;
    private final UserRepository userRepo;

    /** 수강신청 (DB 원자 업데이트) */
    @Transactional
    public EnrollmentDto enroll(Long courseId, Long studentId) {
        // 1) 중복 신청 체크
        if (enrollRepo.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already enrolled");
        }

        // 2) Atomic update: capacity 한도 내에서만 count 증가
        int updated = courseRepo.incrementEnrolledCount(courseId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Course is full");
        }

        // 3) 실제 Enrollment 생성
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        Enrollment en = Enrollment.builder()
                .student(student)
                .course(course)
                .enrolledAt(LocalDateTime.now())
                .build();
        Enrollment saved = enrollRepo.save(en);

        return EnrollmentDto.builder()
                .id(saved.getId())
                .studentId(studentId)
                .courseId(courseId)
                .enrolledAt(saved.getEnrolledAt())
                .build();
    }

    /** 수강취소 */
    @Transactional
    public void cancel(Long enrollmentId) {
        Enrollment en = enrollRepo.findById(enrollmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

        Long courseId = en.getCourse().getId();
        // 1) Enrollment 삭제
        enrollRepo.delete(en);
        // 2) Atomic update: count 감소
        courseRepo.decrementEnrolledCount(courseId);
    }

    // 기타 조회 메서드(listByStudent, listByCourse) 생략…
}

// src/main/java/com/course/registration/waitlist/WaitlistRepository.java
package com.course.registration.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    List<Waitlist> findByCourseIdOrderByWaitOrderAsc(Long courseId);
    Optional<Waitlist> findTopByCourseIdOrderByWaitOrderDesc(Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    long countByCourseId(Long courseId);
}

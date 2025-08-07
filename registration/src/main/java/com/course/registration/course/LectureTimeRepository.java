package com.course.registration.course;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LectureTimeRepository extends JpaRepository<LectureTime, Long> {
    List<LectureTime> findByCourseIdOrderByDayOfWeekAscStartTimeAsc(Long courseId);
}

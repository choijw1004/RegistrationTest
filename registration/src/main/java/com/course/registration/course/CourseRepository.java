package com.course.registration.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("SELECT distinct c FROM Course c LEFT JOIN FETCH c.lectureTimes")
    List<Course> findAllWithLectureTimes();

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.lectureTimes WHERE c.term.id = :termId")
    List<Course> findByTermIdWithLectureTimes(@Param("termId") Long termId);

    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByTermId(Long termId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE course
       SET enrolled_count = enrolled_count + 1
     WHERE id = :courseId
       AND enrolled_count < capacity
    """, nativeQuery = true)
    int incrementEnrolledCount(@Param("courseId") Long courseId);

    // (취소 시 차감용)
    @Modifying
    @Query(value = """
        UPDATE course
           SET enrolled_count = enrolled_count - 1
         WHERE id = :courseId
           AND enrolled_count > 0
        """, nativeQuery = true)
    int decrementEnrolledCount(@Param("courseId") Long courseId);

}
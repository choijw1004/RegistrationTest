package com.course.registration.course;

import com.course.registration.term.Term;
import com.course.registration.term.TermRepository;
import com.course.registration.user.User;
import com.course.registration.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepo;
    private final TermRepository termRepo;
    private final UserRepository userRepo;
    private final LectureTimeRepository timeRepo;


    @Transactional
    public CourseDto createCourse(CourseDto dto) {
        Term term = termRepo.findById(dto.getTermId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found"));
        User prof = userRepo.findById(dto.getProfessorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor not found"));

        Course course = Course.builder()
                .term(term)
                .courseCode(dto.getCourseCode())
                .courseName(dto.getCourseName())
                .capacity(dto.getCapacity())
                .enrolledCount(0)
                .professor(prof)
                .createdAt(LocalDateTime.now())
                .build();

        return toDto(courseRepo.save(course));
    }

//    @Transactional(readOnly = true)
//    public List<CourseDto> listCourses(Long termId) {
//        List<Course> courses = (termId == null)
//                ? courseRepo.findAll()
//                : courseRepo.findByTermId(termId);
//        return courses.stream()
//                .map(this::toDto)
//                .collect(Collectors.toList());
//    }

    public List<CourseDto> listCourses(Long termId) {
        List<Course> courses = (termId == null)
                ? courseRepo.findAllWithLectureTimes()
                : courseRepo.findByTermIdWithLectureTimes(termId);
        return courses.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public CourseDto getCourse(Long id) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return toDto(course);
    }

    @Transactional
    public CourseDto updateCourse(Long id, CourseDto dto) {
        Course course = courseRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        Term term = termRepo.findById(dto.getTermId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found"));
        User prof = userRepo.findById(dto.getProfessorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Professor not found"));

        course.setTerm(term);
        course.updateDetails(dto.getCourseName(), dto.getCapacity(), prof);
        // courseCode 변경 허용 시 아래 추가
        // course.setCourseCode(dto.getCourseCode());

        return toDto(courseRepo.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        courseRepo.deleteById(id);
    }

    // --- LectureTime 통합 ---

    @Transactional
    public LectureTimeDto addLectureTime(Long courseId, LectureTimeDto dto) {
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        LectureTime lt = LectureTime.builder()
                .course(course)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .location(dto.getLocation())
                .build();
        return toLectureTimeDto(timeRepo.save(lt));
    }

    @Transactional(readOnly = true)
    public List<LectureTimeDto> getLectureTimes(Long courseId) {
        if (!courseRepo.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        return timeRepo.findByCourseIdOrderByDayOfWeekAscStartTimeAsc(courseId).stream()
                .map(this::toLectureTimeDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LectureTimeDto updateLectureTime(Long courseId, Long timeId, LectureTimeDto dto) {
        // courseId 존재 여부만 체크
        courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        LectureTime lt = timeRepo.findById(timeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LectureTime not found"));
        lt.setDayOfWeek(dto.getDayOfWeek());
        lt.setStartTime(dto.getStartTime());
        lt.setEndTime(dto.getEndTime());
        lt.setLocation(dto.getLocation());
        return toLectureTimeDto(lt);
    }

    @Transactional
    public void removeLectureTime(Long courseId, Long timeId) {
        courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        if (!timeRepo.existsById(timeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "LectureTime not found");
        }
        timeRepo.deleteById(timeId);
    }

    // --- Mapper helpers ---

    private CourseDto toDto(Course course) {
        return CourseDto.builder()
                .id(course.getId())
                .termId(course.getTerm().getId())
                .courseCode(course.getCourseCode())
                .courseName(course.getCourseName())
                .capacity(course.getCapacity())
                .enrolledCount(course.getEnrolledCount())
                .professorId(course.getProfessor().getId())
                .createdAt(course.getCreatedAt())
                .lectureTimes(course.getLectureTimes().stream()
                        .map(this::toLectureTimeDto)
                        .collect(Collectors.toList()))
                .build();
    }

    private LectureTimeDto toLectureTimeDto(LectureTime lt) {
        return LectureTimeDto.builder()
                .id(lt.getId())
                .courseId(lt.getCourse().getId())
                .dayOfWeek(lt.getDayOfWeek())
                .startTime(lt.getStartTime())
                .endTime(lt.getEndTime())
                .location(lt.getLocation())
                .build();
    }
}

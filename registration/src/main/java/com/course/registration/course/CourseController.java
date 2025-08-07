package com.course.registration.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.*;
import java.util.List;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {
    private final CourseService courseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseDto create(@RequestBody @Valid CourseDto dto) {
        return courseService.createCourse(dto);
    }

    @GetMapping
    public List<CourseDto> list(@RequestParam(required = false) Long termId) {
        return courseService.listCourses(termId);
    }

    @GetMapping("/{id}")
    public CourseDto get(@PathVariable Long id) {
        return courseService.getCourse(id);
    }

    @PutMapping("/{id}")
    public CourseDto update(
            @PathVariable Long id,
            @RequestBody @Valid CourseDto dto
    ) {
        return courseService.updateCourse(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
    }

    @PostMapping("/{courseId}/times")
    @ResponseStatus(HttpStatus.CREATED)
    public LectureTimeDto addTime(
            @PathVariable Long courseId,
            @RequestBody @Valid LectureTimeDto dto
    ) {
        return courseService.addLectureTime(courseId, dto);
    }

    @GetMapping("/{courseId}/times")
    public List<LectureTimeDto> listTimes(@PathVariable Long courseId) {
        return courseService.getLectureTimes(courseId);
    }

    @PutMapping("/{courseId}/times/{timeId}")
    public LectureTimeDto updateTime(
            @PathVariable Long courseId,
            @PathVariable Long timeId,
            @RequestBody @Valid LectureTimeDto dto
    ) {
        return courseService.updateLectureTime(courseId, timeId, dto);
    }

    @DeleteMapping("/{courseId}/times/{timeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTime(
            @PathVariable Long courseId,
            @PathVariable Long timeId
    ) {
        courseService.removeLectureTime(courseId, timeId);
    }
}

package com.course.registration.course;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LectureTimeService {

    private final LectureTimeRepository timeRepo;
    private final CourseRepository courseRepo;

    @Transactional
    public LectureTimeDto create(Long courseId, LectureTimeDto dto) {
        var course = courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        var lt = LectureTime.builder()
                .course(course)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .location(dto.getLocation())
                .build();
        return toDto(timeRepo.save(lt));
    }

    @Transactional(readOnly = true)
    public List<LectureTimeDto> listByCourse(Long courseId) {
        if (!courseRepo.existsById(courseId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        return timeRepo.findByCourseIdOrderByDayOfWeekAscStartTimeAsc(courseId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public LectureTimeDto update(Long id, LectureTimeDto dto) {
        var lt = timeRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "LectureTime not found"));
        lt.setDayOfWeek(dto.getDayOfWeek());
        lt.setStartTime(dto.getStartTime());
        lt.setEndTime(dto.getEndTime());
        lt.setLocation(dto.getLocation());
        return toDto(lt);
    }

    @Transactional
    public void delete(Long id) {
        if (!timeRepo.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "LectureTime not found");
        timeRepo.deleteById(id);
    }

    private LectureTimeDto toDto(LectureTime lt) {
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

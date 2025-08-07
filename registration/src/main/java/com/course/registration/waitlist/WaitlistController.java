// src/main/java/com/course/registration/waitlist/WaitlistController.java
package com.course.registration.waitlist;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/courses/{courseId}/waitlist")
@RequiredArgsConstructor
public class WaitlistController {
    private final WaitlistService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaitlistDto add(
            @PathVariable Long courseId,
            @RequestBody @Valid WaitlistDto dto
    ) {
        return service.addToWaitlist(courseId, dto.getStudentId());
    }

    @GetMapping
    public List<WaitlistDto> list(@PathVariable Long courseId) {
        return service.listByCourse(courseId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long courseId, @PathVariable Long id) {
        service.removeFromWaitlist(id);
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@PathVariable Long courseId) {
        return service.createEmitter(courseId);
    }
}

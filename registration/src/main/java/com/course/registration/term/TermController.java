// src/main/java/com/course/registration/term/TermController.java
package com.course.registration.term;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermController {
    private final TermService termService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermDto create(@RequestBody @Valid TermDto dto) {
        return termService.createTerm(dto);
    }

    @GetMapping
    public List<TermDto> list() {
        return termService.getAllTerms();
    }

    @GetMapping("/{id}")
    public TermDto get(@PathVariable Long id) {
        return termService.getTerm(id);
    }

    @PutMapping("/{id}")
    public TermDto update(@PathVariable Long id, @RequestBody @Valid TermDto dto) {
        return termService.updateTerm(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        termService.deleteTerm(id);
    }
}

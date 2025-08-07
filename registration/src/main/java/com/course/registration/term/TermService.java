// src/main/java/com/course/registration/term/TermService.java
package com.course.registration.term;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermService {
    private final TermRepository termRepository;

    public TermDto createTerm(TermDto dto) {
        Term term = Term.builder()
                .name(dto.getName())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        Term saved = termRepository.save(term);
        return toDto(saved);
    }

    public List<TermDto> getAllTerms() {
        return termRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public TermDto getTerm(Long id) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found"));
        return toDto(term);
    }

    public TermDto updateTerm(Long id, TermDto dto) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found"));
        term.setName(dto.getName());
        term.setStartDate(dto.getStartDate());
        term.setEndDate(dto.getEndDate());
        Term updated = termRepository.save(term);
        return toDto(updated);
    }

    public void deleteTerm(Long id) {
        if (!termRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Term not found");
        }
        termRepository.deleteById(id);
    }

    private TermDto toDto(Term term) {
        return TermDto.builder()
                .id(term.getId())
                .name(term.getName())
                .startDate(term.getStartDate())
                .endDate(term.getEndDate())
                .build();
    }
}

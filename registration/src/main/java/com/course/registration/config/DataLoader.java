package com.course.registration.config;

import com.course.registration.course.Course;
import com.course.registration.course.CourseRepository;
import com.course.registration.term.Term;
import com.course.registration.term.TermRepository;
import com.course.registration.user.User;
import com.course.registration.user.UserRepository;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Component
public class DataLoader implements ApplicationRunner {
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final TermRepository termRepo;
    private final UserRepository profRepo;
    private final Faker kFaker = new Faker(new Locale("ko"));

    @Override
    public void run(ApplicationArguments args) {
        // 이미 데이터가 있으면 스킵
        if (userRepo.count() > 0 || courseRepo.count() > 0) {
            return;
        }

        // 1) Term 생성 및 저장
        Term term = Term.builder()
                .name("23-2학기")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .build();
        final Term savedTerm = termRepo.save(term);

        // 2) 교수 20명 생성 및 저장
        List<User> initialProfs = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> User.builder()
                        .email("prof" + i + "@school.edu")
                        .name(kFaker.name().fullName())
                        .role(User.Role.PROFESSOR)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
        final List<User> savedProfs = profRepo.saveAll(initialProfs);

        // 3) 학생 30,000명 생성 및 저장
        List<User> students = IntStream.rangeClosed(1, 30000)
                .mapToObj(i -> User.builder()
                        .email("student" + i + "@school.edu")
                        .name(kFaker.name().fullName())
                        .role(User.Role.STUDENT)
                        .createdAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());
        userRepo.saveAll(students);

        // 4) 강의 200개 생성 및 저장
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Course> courses = IntStream.rangeClosed(1, 200)
                .mapToObj(i -> {
                    User prof = savedProfs.get(rnd.nextInt(savedProfs.size()));
                    return Course.builder()
                            .term(savedTerm)
                            .courseCode("C" + String.format("%03d", i))
                            .courseName("Course " + i)
                            .capacity(30 + rnd.nextInt(71))  // 30~100명
                            .enrolledCount(0)
                            .professor(prof)
                            .createdAt(LocalDateTime.now())
                            .build();
                })
                .collect(Collectors.toList());
        courseRepo.saveAll(courses);
    }
}

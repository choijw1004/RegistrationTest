package com.course.registration.course;

import com.course.registration.enrollment.EnrollmentService;
import com.course.registration.enrollment.EnrollmentDto;
import com.course.registration.waitlist.WaitlistService;
import com.course.registration.waitlist.WaitlistRepository;
import com.course.registration.waitlist.WaitlistDto;
import com.course.registration.user.User;
import com.course.registration.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=200",
        "spring.datasource.hikari.connection-timeout=30000"
})
class EnrollmentServiceConcurrencyTest {

    @Autowired EnrollmentService enrollService;
    @Autowired WaitlistService waitService;
    @Autowired WaitlistRepository waitlistRepository;
    @Autowired CourseRepository courseRepo;
    @Autowired UserRepository userRepo;
    private Long courseId;
    private List<Long> studentIds;

    @BeforeEach
    void setUp() {
        Course c = courseRepo.findById(119L).orElseThrow();
        c.setEnrolledCount(0);
        courseRepo.save(c);
        courseId = c.getId();
        studentIds = userRepo.findAll().stream()
                .map(User::getId)
                .limit(200)
                .toList();
        assertEquals(200, studentIds.size(), "학생이 200명 있어야 합니다");
    }

    @Test
    void concurrentServiceEnrollThenWaitlist() throws InterruptedException {
        int N = studentIds.size();
        var ready = new CountDownLatch(N);
        var start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger wl = new AtomicInteger();

        for (Long sid : studentIds) {
            new Thread(() -> {
                ready.countDown();
                try {
                    start.await();
                    try {
                        EnrollmentDto dto = enrollService.enroll(courseId, sid);
                        ok.incrementAndGet();
                    } catch (ResponseStatusException ex) {
                        if (ex.getStatusCode() == HttpStatus.CONFLICT &&
                                "Course is full".equals(ex.getReason())) {
                            waitService.addToWaitlist(courseId, sid);
                            wl.incrementAndGet();
                        } else {
                            fail("예상치 못한 예외: " + ex.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            }).start();
        }

        ready.await();
        start.countDown();
        Thread.sleep(3000);

        assertEquals(100, ok.get(), "등록 성공 수");
        assertEquals(100, wl.get(), "대기열 수");

        Course c = courseRepo.findById(courseId).orElseThrow();
        assertEquals(100, c.getEnrolledCount());
        assertEquals(100, waitlistRepository.countByCourseId(courseId));
    }
}

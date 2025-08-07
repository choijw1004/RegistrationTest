// src/main/java/com/course/registration/waitlist/WaitlistService.java
package com.course.registration.waitlist;

import com.course.registration.course.CourseRepository;
import com.course.registration.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WaitlistService {
    private final WaitlistRepository repo;
    private final CourseRepository courseRepo;
    private final UserRepository userRepo;
    private final RedissonClient redisson;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long courseId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        // remove on timeout/completion
        emitter.onCompletion(() -> removeEmitter(courseId, emitter));
        emitter.onTimeout(   () -> removeEmitter(courseId, emitter));

        emitters
                .computeIfAbsent(courseId, id -> Collections.synchronizedList(new ArrayList<>()))
                .add(emitter);

        return emitter;
    }

    private void removeEmitter(Long courseId, SseEmitter e) {
        List<SseEmitter> list = emitters.get(courseId);
        if (list != null) list.remove(e);
    }

    private void sendEvent(Long courseId, String event, Object data) {
        List<SseEmitter> list = emitters.get(courseId);
        if (list == null) return;
        list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (Exception ex) {
                removeEmitter(courseId, emitter);
            }
        });
    }

    //    @Transactional
//    public WaitlistDto addToWaitlist(Long courseId, Long studentId) {
//        if (repo.existsByStudentIdAndCourseId(studentId, courseId)) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in waitlist");
//        }
//        courseRepo.findById(courseId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
//        userRepo.findById(studentId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
//
//        Integer nextOrder = repo.findTopByCourseIdOrderByWaitOrderDesc(courseId)
//                .map(w -> w.getWaitOrder() + 1)
//                .orElse(1);
//
//        Waitlist wl = Waitlist.builder()
//                .course(courseRepo.getReferenceById(courseId))
//                .student(userRepo.getReferenceById(studentId))
//                .waitOrder(nextOrder)
//                .createdAt(LocalDateTime.now())
//                .build();
//        Waitlist saved = repo.save(wl);
//
//        WaitlistDto dto = toDto(saved);
//        sendEvent(courseId, "ADDED", dto);
//        return dto;
//    }
    @Transactional
    public WaitlistDto addToWaitlist(Long courseId, Long studentId) {

        // 1) 기본 검증
        if (repo.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in waitlist");
        }
        courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        userRepo.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        // 2) Redis 락 획득 (최대 대기 5초, 락 유지 60초)
        String lockKey = "waitlist:lock:" + courseId;
        RLock lock = redisson.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(5, 60, TimeUnit.SECONDS);
            if (!acquired) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not acquire waitlist lock");
            }

            // 3) 크리티컬 섹션: 가장 높은 순번 +1 계산 및 저장
            Integer nextOrder = repo.findTopByCourseIdOrderByWaitOrderDesc(courseId)
                    .map(w -> w.getWaitOrder() + 1)
                    .orElse(1);

            Waitlist saved = repo.save(Waitlist.builder()
                    .course(courseRepo.getReferenceById(courseId))
                    .student(userRepo.getReferenceById(studentId))
                    .waitOrder(nextOrder)
                    .createdAt(LocalDateTime.now())
                    .build()
            );

            WaitlistDto dto = toDto(saved);
            sendEvent(courseId, "ADDED", dto);

            // 4) 트랜잭션 커밋 후에만 락 해제
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            lock.unlock();
                        }

                        @Override
                        public void afterCompletion(int status) {
                            // 예외 발생 등으로 커밋이 안 됐더라도 락 해제
                            if (lock.isHeldByCurrentThread()) {
                                lock.unlock();
                            }
                        }
                    }
            );

            return dto;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Interrupted while waiting for lock", e);
        }
    }
//    public WaitlistDto addToWaitlist(Long courseId, Long studentId) {
//        // 1) 검증
//        if (repo.existsByStudentIdAndCourseId(studentId, courseId)) {
//            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in waitlist");
//        }
//        courseRepo.findById(courseId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
//        userRepo.findById(studentId)
//                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
//
//        // 2) Redis 락 획득 (대기 5초, 유지 60초)
//        String lockKey = "waitlist:lock:" + courseId;
//        RLock lock = redisson.getLock(lockKey);
//        try {
//            boolean acquired = lock.tryLock(5, 60, TimeUnit.SECONDS);
//            if (!acquired) {
//                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not acquire waitlist lock");
//            }
//
//            // **3) 락을 잡은 상태에서** 실제 DB작업(트랜잭션)을 호출
//            return addToWaitlistTransactional(courseId, studentId);
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Interrupted while waiting for lock", e);
//        } finally {
//            // inner 트랜잭션이 커밋된 뒤 이 finally가 실행되므로,
//            // 커밋 시점 이후에 락이 해제됩니다.
//            if (lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//    }
//
//    @Transactional
//    protected WaitlistDto addToWaitlistTransactional(Long courseId, Long studentId) {
//        // (이 메소드 안에서만 트랜잭션이 걸립니다)
//
//        // 1) 가장 높은 순번 +1
//        Integer nextOrder = repo.findTopByCourseIdOrderByWaitOrderDesc(courseId)
//                .map(w -> w.getWaitOrder() + 1)
//                .orElse(1);
//
//        // 2) 저장
//        Waitlist saved = repo.save(Waitlist.builder()
//                .course(courseRepo.getReferenceById(courseId))
//                .student(userRepo.getReferenceById(studentId))
//                .waitOrder(nextOrder)
//                .createdAt(LocalDateTime.now())
//                .build()
//        );
//
//        // 3) 이벤트 발행
//        WaitlistDto dto = toDto(saved);
//        sendEvent(courseId, "ADDED", dto);
//
//        return dto;
//    }

    /** 대기열 전체 조회 */
    @Transactional(readOnly = true)
    public List<WaitlistDto> listByCourse(Long courseId) {
        courseRepo.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        return repo.findByCourseIdOrderByWaitOrderAsc(courseId)
                .stream().map(this::toDto).toList();
    }

    /** 대기열에서 제거(취소) */
    @Transactional
    public void removeFromWaitlist(Long id) {
        Waitlist wl = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waitlist entry not found"));
        Long courseId = wl.getCourse().getId();
        int removedOrder = wl.getWaitOrder();
        repo.delete(wl);

        // shift up others
        List<Waitlist> rest = repo.findByCourseIdOrderByWaitOrderAsc(courseId);
        for (Waitlist w : rest) {
            if (w.getWaitOrder() > removedOrder) {
                w.setWaitOrder(w.getWaitOrder() - 1);
                repo.save(w);
                sendEvent(courseId, "UPDATED", toDto(w));
            }
        }
        sendEvent(courseId, "REMOVED", Map.of("id", id));
    }

    private WaitlistDto toDto(Waitlist wl) {
        return WaitlistDto.builder()
                .id(wl.getId())
                .studentId(wl.getStudent().getId())
                .courseId(wl.getCourse().getId())
                .waitOrder(wl.getWaitOrder())
                .createdAt(wl.getCreatedAt())
                .build();
    }
}

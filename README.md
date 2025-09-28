# 단일 인스턴스로 개발하는 수강 신청 백엔드 시스템

담당 역할: BE, 데이터베이스 설계
GitHub: https://github.com/choijw1004/RegistrationTest

Contents

---

### 프로젝트 개요

- 목적: 학부 시절 수강신청시 서버가 매 번 장애가 발생하는 것을 목격하고 단일 **안정성과** **확장성을** 확보하기 위해 단일 서버 환경에서 동시성 제어와 대기열 관리 기능을 백엔드 레벨에서 직접 구현하고자 하였습니다.

---

### 담당 역할

- 백엔드 아키텍쳐 설계(Spring Boot + MySQL + Redis), 데이터 베이스 설계(MySQL)
- `Course(강의)` , `Enrollment(수강신청)`, `Term(학기)`, `User(학생 + 교수)`` , `Waitist대기열)` 도메인 RestAPI 개발
- 대기열 순번 관리 및 SSE 기반 실시간 알림 기능 개발
- 부하 테스트(JUnit, K6) 및 성능 개선

---

### 기술 스택

- **BackEnd**: JDK 21, Gradle, Spring Boot
- **Database:** MySQL 8.0, Redis
- **Protocol: HTTP(**REST API, Server-Sent Events (SSE))

---

### 프로젝트 ERD(course_db)

![course_db.png](%EB%8B%A8%EC%9D%BC%20%EC%9D%B8%EC%8A%A4%ED%84%B4%EC%8A%A4%EB%A1%9C%20%EA%B0%9C%EB%B0%9C%ED%95%98%EB%8A%94%20%EC%88%98%EA%B0%95%20%EC%8B%A0%EC%B2%AD%20%EB%B0%B1%EC%97%94%EB%93%9C%20%EC%8B%9C%EC%8A%A4%ED%85%9C%2024838be78204806c94cad064e4a7e7bc/course_db.png)

---

### 주요 기능

- **회원 조회 기능**
- **학기 생성·수정·삭제·조회**
- **강의 CRUD 및 학기별 필터링**
- **강의 시간 CRUD**
- **수강신청 (정원 관리 + 대기열 자동 연계)**
- **수강 취소 및 대기열 관리**
- **SSE 기반 실시간 대기열 알림**

---

### 테스트 준비

구현을 마치고 프로젝트의 목적에 맞게 실제 트래픽을 발생시켜 보고자 하였습니다. 이때 실제로 수강 신청 인원의 트래픽을 접할 수 없어 일종의 dummy 데이터를 만들어야 했는데요. 이를 위해 Faker 라이브러리를 사용하였습니다. 

<aside>

Faker는 이름, 이메일, 날짜 등 다양한 가짜 데이터를 손쉽게 생성할 수 있는 테스트용 데이터 생성 라이브러리입니다. 

</aside>

Faker 라이브러리를 사용하여 DataLoader 클래스를 정의하였습니다. 

- DataLoader.java
    
    ```makefile
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
            if (userRepo.count() > 0 || courseRepo.count() > 0) {
                return;
            }
            
            Term term = Term.builder()
                    .name("23-2학기")
                    .startDate(LocalDate.of(2025, 9, 1))
                    .endDate(LocalDate.of(2025, 12, 31))
                    .build();
            final Term savedTerm = termRepo.save(term);
    
            List<User> initialProfs = IntStream.rangeClosed(1, 20)
                    .mapToObj(i -> User.builder()
                            .email("prof" + i + "@school.edu")
                            .name(kFaker.name().fullName())
                            .role(User.Role.PROFESSOR)
                            .createdAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());
            final List<User> savedProfs = profRepo.saveAll(initialProfs);
    
            List<User> students = IntStream.rangeClosed(1, 30000)
                    .mapToObj(i -> User.builder()
                            .email("student" + i + "@school.edu")
                            .name(kFaker.name().fullName())
                            .role(User.Role.STUDENT)
                            .createdAt(LocalDateTime.now())
                            .build())
                    .collect(Collectors.toList());
            userRepo.saveAll(students);
    
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
    
    ```
    

실제 본인 대학교의 학생 수는 약 25,000명에 육박합니다. 따라서 비관적으로 30,000명의 학생 수와, 교수 20명, 강의를 200개 생성을 완료하였습니다. 또한 테스트에 앞서 수강 신청이라는 시스템에 발생하는 트래픽을 예상해보았을때 다음과 같은 특징을 정리하였을때, 서버가 감당해야할 최소한의 요구사항은 아래와 같습니다. 

```makefile
1. 모든 학생 30000명이 전부 수강신청하지는 않을 것.(휴학, 졸업유예 등)
2. 대략 15,000명 ~ 20,000며 수강신청 서버에 같은 날 같은 시점에 진입할 것.
3. 수강신청 시간에 가장 처음 User가 요청하는 API는 /courses 즉 수강신청 목록일 것. 
4. 같은 시점에 같은 수강신청 요청을 처리해야하고 reflict된 요청을 대기열에 정확히 순번과 함께 return.
```

테스트를 위해 `/courses` 의 요청은 k6로, 수강신청과 대기열을 위한 테스트로는 JUnit5로 진행하였습니다. 

---

### **문제 해결 과정 1 (JPA N+1 문제 해결)**

단일 인스턴스 환경에서 20,000개의 같은 시점의 요청은 불가능하여 VU 150, 200, 300 테스트를 진행하였을때의 지표를 정리한 표입니다.

| VU  | 성공률 | 실패률 | 평균 응답시간 | P90 응답시간 | P95 응답시간 |
| --- | --- | --- | --- | --- | --- |
| 300 | 97.57% | 2.42% | 686.05ms | 836.55ms | 843.13ms |
| 200 | 92.32% | 7.67% | 883.93ms | 1.02s | 1.02s |
| 150 | 100.00% | 0.00% | 501.44ms | 554.99ms | 598.27ms |

처음 결과를 확인했을 때, 단순한 HTTP GET 요청임에도 불구하고 VU 300 환경에서는 2%가 넘는 실패가 발생했다는 점이 의아했습니다. 이후 VU 200, VU 150까지 점진적으로 부하를 줄여보았고, VU 150 수준에서만 모든 요청이 정상 처리되는 것을 확인할 수 있었습니다.

이는 단순히 부하량이 많아서라기보다는 **애플리케이션 내부의 병목 또는 비효율적인 쿼리 처리 구조가 원인일 가능성**이 높다고 판단했습니다. 실제로 당시 구조에서 아래와 같은 병목 지점을 의심할 수 있었습니다:

당시 예상할 수 있는 병목 구간은 다음과 같습니다.

```makefile
1. Tomcat의 처리 스레드 수 부족으로 인한 큐잉 및 응답 지연
2. 데이터베이스 커넥션 풀의 설정 값 부족 또는 제한
3. 과도한 DB 접근 및 불필요한 쿼리 실행으로 인한 트랜잭션 지연
```

1. 톰캣의 스레드 수 부족

초기에는 동시에 요청을 처리하지 못하는 원인을 톰캣의 스레드 풀 부족으로 의심하여, 다음과 같이 설정을 조정하였습니다:

```makefile
server.tomcat.threads.max: 300
server.tomcat.accept-count: 200
server.tomcat.threads.min-spare: 50
```

| VU | 성공률 | 실패률 | 평균 응답시간 | P90 응답시간 | P95 응답시간 |
| --- | --- | --- | --- | --- | --- |
| 200 | 97.12% | 2.87% | 684.29ms | 758.28ms | 805.11ms |

응답시간이 조금은 개선되었다는 것을 확인할 수 있었지만 정확한 병목구간이 아닌 것을 확인하였습니다.

1. DB 커넥션 풀 고갈

다음으로 동시에 요청되는 DB 커넥션이 부족하여 병목이 생긴다고 판단하여 `HikariCP` 설정을 다음과 같이 확장하였습니다.

```makefile
spring.datasource.hikari.maximum-pool-size: 100
spring.datasource.hikari.connection-timeout: 2000
```

| VU | 성공률 | 실패률 | 평균 응답시간 | P90 응답시간 | P95 응답시간 |
| --- | --- | --- | --- | --- | --- |
| 200 | 97.75% | 2.24% | 652.67ms | 709.69ms | 735.20ms |

역시 성능 개선에는 제한적이었습니다.

1. 불필요한 쿼리 실행으로 인한 처리 지연

테스트를 반복하였을 때 쿼리 속도가 매우 떨어지는 것을 확인하였고 쿼리를 처리하는 부분에서 병목이 생긴다는 것을 예상하여 문제가 될 수 있는 부분을 확인해보았습니다. 

코드와 쿼리 로그를 통해 추적한 결과, 다음과 같은 패턴의 쿼리가 연속적으로 반복되고 있는 것을 확인했습니다:

```makefile
select * from lecture_time where course_id = ?
select * from lecture_time where course_id = ?
select * from lecture_time where course_id = ?
```

이는 `Course`와 `LectureTime` 간의 `OneToMany` 관계에서, 모든 Course를 불러온 뒤 각 Course에 대한 `lectureTimes`를 지연로딩(LAZY) 방식으로 추가 쿼리를 실행하면서 발생한 N+1 문제였습니다.

N+1 문제를 해결하기 위해 `CourseRepository`에 다음과 같이 `JOIN FETCH`를 적용하여 한 번의 쿼리로 모든 데이터를 조회하도록 수정했습니다.

```makefile
@Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.lectureTimes")
List<Course> findAllWithLectureTimes();

@Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.lectureTimes WHERE c.term.id = :termId")
List<Course> findByTermIdWithLectureTimes(@Param("termId") Long termId);
```

CourseService도 다음과 같이 변경하였습니다.

```makefile
public List<CourseDto> listCourses(Long termId) {
    List<Course> courses = (termId == null)
            ? courseRepo.findAllWithLectureTimes()
            : courseRepo.findByTermIdWithLectureTimes(termId);
    return courses.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
}
```

| VU | 성공률 | 실패률 | 평균 응답시간 | P90 응답시간 | P95 응답시간 |
| --- | --- | --- | --- | --- | --- |
| 200 | 99.71% | 0.28% | 96.60ms | 134.28ms | 174.73ms |

이후 동일한 조건(VU 200)에서 테스트했을 때, 응답 실패율이 **대폭 감소**하였고 평균 응답 속도도 약 85% 감소하였다는 것을 확인할 수 있었습니다.

---

### 문제 해결 과정 2 (데이터 정합성 처리)

수강 대기열 기능에서 **간헐적으로 같은 순번(waitOrder)을 가진 대기자들이 동시에 등록되는 문제가 발생**하였습니다. 특히 동시에 여러 사용자가 같은 강의에 대기 신청을 할 때 발생 확률이 높았으며 이로 인해 아래와 같이 대기열의 순번이 꼬이는 문제가 발생하였습니다. 

| id | student\_id | course\_id | wait\_order |
| --- | --- | --- | --- |
| 301 | 154 | 119 | 1 |
| 302 | 168 | 119 | 1 |
| 303 | 172 | 119 | 1 |
| 304 | 176 | 119 | 1 |
| 305 | 104 | 119 | 2 |
| 306 | 193 | 119 | 2 |
| 307 | 105 | 119 | 2 |

기존 코드는 아래와 같이 **Redisson 락을 트랜잭션 내부에서 처리하고 있었고,** 락의 해제를   `TransactionSynchronizationManager`로 조절하고 있었습니다.

```java
@Transactional
public WaitlistDto addToWaitlist(...) {
    // 1. Redisson 분산 락 획득 (트랜잭션 내부)
    lock.tryLock(...)

    // 2. 가장 큰 waitOrder 조회
    Integer nextOrder = repo.findTopByCourseIdOrderByWaitOrderDesc(courseId)
                            .map(o -> o.getWaitOrder() + 1)
                            .orElse(1);

    // 3. nextOrder를 저장
    repo.save(...);

    // 4. 락 해제를 트랜잭션 커밋 이후로 지연
    TransactionSynchronizationManager.registerSynchronization(...);
}
```

이 구조에서는 트랜잭션 커밋과 락 해제 시점이 명확하게 일치하지 않아 예외 발생 시 락이 제대로 해제되지 않거나, 순번 중복이 발생하는 위험이 있었습니다. 문제를 분석하였을 때 두 가지 문제점이 발생함을 예상하였습니다.

1. **락 해제 시점의 불일치**
    - 트랜잭션 커밋 이후에야 락을 해제하도록 `TransactionSynchronizationManager`에 콜백을 등록했으나,
    - 커밋 중 예외가 발생하거나 커밋 지연이 길어지면
        1. 등록된 콜백(`afterCommit`/`afterCompletion`)이 실행되지 않거나
        2. 락의 TTL(Lease Time)이 만료 → 자동 해제
    - 위 상황에서 다른 쓰레드가 락을 획득하면, 커밋 전 DB 변경사항을 반영하지 않은 상태로 진입하여 **동일 순번 부여**가 발생할 수 있습니다.
    
2. **순번(read-then-write) 계산 로직에서의 문제점**
    - 현재 `findTopByCourseIdOrderByWaitOrderDesc()` 호출 시점에도 격리 수준(REPEATABLE_READ 등)에 따라 이전 커밋분이 바로 보장되지 않을 수 있고
    - 두 개 이상의 쓰레드가 거의 동시에 최대 `waitOrder` 값을 읽어 오면
        
        ```java
        Integer nextOrder = repo
            .findTopByCourseIdOrderByWaitOrderDesc(courseId)
            .map(w -> w.getWaitOrder() + 1)
            .orElse(1);
        ```
        
        결과적으로 같은 `nextOrder`를 계산해 저장하게 됩니다.
        
    - 검증(`existsBy…`)도 락 외부에서 수행되므로 이미 등록된지 체크 로직이 대기 잠금 안에서 작동하지 않아 중복 등록을 막아주지 못합니다.

따라서 **락 획득 → 트랜잭션 실행** 으로 순서 분리를 분리하였고 검증, 순번 계산, 저장 등 모든 DB의 작업을 락 내부로 이동시켜 `finally`에서 명시적으로 `unlock`을 호출하게끔 변경하여 해결하여 올바른 대기열의 순번을 저장시킬 수 있게끔 해결하였습니다. 

| id | student\_id | course\_id | wait\_order |
| --- | --- | --- | --- |
| 901 | 140 | 119 | 1 |
| 902 | 106 | 119 | 2 |
| 903 | 160 | 119 | 3 |
| 904 | 104 | 119 | 4 |
| 905 | 117 | 119 | 5 |
| 906 | 108 | 119 | 6 |
| 907 | 176 | 119 | 7 |
- 개선 코드
    
    ```java
        public WaitlistDto addToWaitlist(Long courseId, Long studentId) {
            if (repo.existsByStudentIdAndCourseId(studentId, courseId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Already in waitlist");
            }
            courseRepo.findById(courseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
            userRepo.findById(studentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    
            // 2) Redis 락 획득 (대기 5초, 유지 60초)
            String lockKey = "waitlist:lock:" + courseId;
            RLock lock = redisson.getLock(lockKey);
            try {
                boolean acquired = lock.tryLock(5, 60, TimeUnit.SECONDS);
                if (!acquired) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Could not acquire waitlist lock");
                }
    
                return addToWaitlistTransactional(courseId, studentId);
    
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "락을 기다리는 시점에 예외 발생", e);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    
        @Transactional
        protected WaitlistDto addToWaitlistTransactional(Long courseId, Long studentId) {
    
            Integer nextOrder = repo.findTopByCourseIdOrderByWaitOrderDesc(courseId)
                    .map(w -> w.getWaitOrder() + 1)
                    .orElse(1);
    
            // 2) 저장
            Waitlist saved = repo.save(Waitlist.builder()
                    .course(courseRepo.getReferenceById(courseId))
                    .student(userRepo.getReferenceById(studentId))
                    .waitOrder(nextOrder)
                    .createdAt(LocalDateTime.now())
                    .build()
            );
    
            // 3) 이벤트 발행
            WaitlistDto dto = toDto(saved);
            sendEvent(courseId, "ADDED", dto);
    
            return dto;
        }
    
    ```
    

### 문제 해결 과정 3 (SSE 기반 실시간 알림 도입)

도입 배경

- 대기열 상태 변화 정보를 실시간으로 클라이언트에 전달하기 위한 방안을 검토했습니다.

```java
1. HTTP Polling
2. WebSocket
3. Server-Sent Events(SSE)
```

세 가지 방안을 비교해보았을 때

- HTTP 폴링 방식은 잦은 요청으로 인한 지연 시간 및 네트워크·서버 부하가 과도해 부적합하다고 판단하였습니다.
- WebSocket은 양방향 통신이 가능하지만, 구독자 수가 많아질수록 지속적인 커넥션 관리로 서버 리소스 부담이 커질 우려가 있었습니다.
- SSE는 단방향 푸시 방식으로 구현이 간단하고 HTTP 커넥션만으로 충분한 실시간성 확보가 가능하여 최종적으로 SSE를 도입하기로 결정하였습니다.

SSE를 구현한 방식은 다음과 같습니다.

- **SSE 구독 관리**
    - `Map<Long, List<SseEmitter>> emitters` 에 강의별로 구독자(`SseEmitter`) 리스트를 저장
    - `createEmitter(courseId)` 호출 시
        
        ```java
        
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.onCompletion(() -> removeEmitter(courseId, emitter));
        emitter.onTimeout(   () -> removeEmitter(courseId, emitter));
        emitters
          .computeIfAbsent(courseId, id -> Collections.synchronizedList(new ArrayList<>()))
          .add(emitter);
        return emitter;
        ```
        
        을 통해 새 구독자를 등록하고, 타임아웃/커넥션 종료 시 자동 제거
        
- **이벤트 전송 로직**
    - 비즈니스 로직 내부(`addToWaitlist`, `removeFromWaitlist` 등)에서 변경 직후 호출
    - `sendEvent(courseId, event, data)`
        
        ```java
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
        ```
        
    - 이벤트 타입:
        - `"ADDED"`: 대기열 추가
        - `"UPDATED"`: 순번 변경
        - `"REMOVED"`: 대기열 취소

클라이언트는 최초 한 번만 SSE 연결을 맺으면 이후 자동 push로 대기열 변경을 바로 수신할 수 있게 됩니다. 또한 프론트엔드와 연결되었다고 가정하였을때, 지연 없는 실시간 UI로 수강신청의 대기열을 보다 빠르게 확인할 수 있게 됩니다.

---

### 마치며

- 이번 프로젝트를 통해 대기열이라는 단순해 보이는 기능에도 **동시성 제어**와 **실시간 알림**이라는 두 가지 중요한 요구사항이 얽혀 있을 때 얼마나 복잡해지는지를 체감했습니다.
- 이 과정을 통해 Spring 트랜잭션과 Redisson 분산 락의 상호작용, 그리고 SSE의 활용법을 깊이 이해할 수 있었으며, **안정성**과 **확장성**을 모두 만족하는 아키텍쳐를 완성했습니다.
- 실제로 처음에 가정한 25,000명에서 ~ 30,000명의 트래픽을 모두 감당할 수 있는 고 가용성의 서버를 구현한다면 메시지 큐, 장애시 롤백, 재시도 전략 보강 등을 통해 더 강건한 시스템으로 발전시켜나갈 계획입니다.
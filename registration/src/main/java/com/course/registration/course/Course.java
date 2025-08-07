package com.course.registration.course;

import com.course.registration.term.Term;
import com.course.registration.user.User;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "course",
        uniqueConstraints = @UniqueConstraint(columnNames = "course_code"),
        indexes = {
                @Index(name = "idx_term_id", columnList = "term_id"),
                @Index(name = "idx_professor_id", columnList = "professor_id")
        }
)
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Term term;

    @Column(name = "course_code", length = 50, nullable = false)
    private String courseCode;

    @Column(name = "course_name", length = 100, nullable = false)
    private String courseName;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "enrolled_count", nullable = false)
    private Integer enrolledCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "professor_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User professor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LectureTime> lectureTimes = new ArrayList<>();

    // — 도메인 메서드 —

    /** 강의명·정원·교수 수정 */
    public void updateDetails(String courseName, int capacity, User professor) {
        this.courseName = courseName;
        this.capacity = capacity;
        this.professor = professor;
    }

    /** 잔여 슬롯 존재 여부 */
    public boolean hasAvailableSlot() {
        return this.enrolledCount < this.capacity;
    }

    /** 수강 등록 시 count 증가 */
    public void incrementEnrolled() {
        this.enrolledCount++;
    }

    /** 수강 취소 시 count 감소 */
    public void decrementEnrolled() {
        if (this.enrolledCount > 0) {
            this.enrolledCount--;
        }
    }

}

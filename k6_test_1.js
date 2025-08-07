import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 30,
  duration: '15s',
  thresholds: {
    http_req_duration: ['p(95)<500'], // 현실적인 목표로 조정
    http_reqs: ['count>=30'],
  },
};

export function setup() {
  // 테스트 전에 서버 초기화 및 학생/강의 ID 목록 로드
  const resetRes = http.post('http://localhost:8080/internal/reset');
  check(resetRes, { 'reset 200': (r) => r.status === 200 });

  const usersRes = http.get('http://localhost:8080/users');
  const coursesRes = http.get('http://localhost:8080/courses');

  const students = JSON.parse(usersRes.body).map(u => u.id);
  const courses = JSON.parse(coursesRes.body).map(c => c.id);

  return { students, courses };
}

export default function (data) {
  const { students, courses } = data;

  // 1) 수강 신청
  const studentId = students[__VU % students.length];
  const courseId = courses[Math.floor(Math.random() * courses.length)];

  const enrollRes = http.post(
    `http://localhost:8080/courses/${courseId}/enrollments`,
    JSON.stringify({ studentId }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(enrollRes, { 'enroll: 201 or 409': (r) => r.status === 201 || r.status === 409 });

  // 2) 등록 성공 시 취소
  if (enrollRes.status === 201) {
    const enrollmentId = JSON.parse(enrollRes.body).id;
    // 대기 후 취소
    sleep(1);
    const cancelRes = http.del(`http://localhost:8080/enrollments/${enrollmentId}`);
    check(cancelRes, { 'cancel: 204': (r) => r.status === 204 });
  }

  // 다음 반복까지 짧게 대기
  sleep(0.05);
}

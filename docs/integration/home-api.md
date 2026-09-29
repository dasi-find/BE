# 홈 요약 및 수색카드 후보 집계 (#65)

갱신: 2026-09-29. 기존 API 명세의 필드는 유지하고 실제 후보 요약을 연결한다. DB schema/환경변수 변경 없음.

## GET /api/v1/home

Access Token 필수. 사용자 ID는 인증 토큰에서만 가져오며 비로그인은 COMMON4011, 토큰 사용자가 없으면 AUTH4012다.

- activeSearchCards: 본인의 ACTIVE이고 searchExpiresAt >= 현재 시각인 카드 전체. createdAt DESC, id DESC.
- daysRemaining: 서버 날짜에서 만료 날짜까지의 차이. 만료 당일 0. 기존 카드 생성/만료 작업과 같은 서버 시간대를 사용하며 테스트에서는 Clock으로 고정한다.
- newCandidates: 활성·만료 전 카드에 속한 최신 평가·미확인·비제외·점수 있는 후보 중 최대 5개. 여러 카드의 후보를 합쳐 totalScore DESC, foundDate DESC(null 마지막), id ASC 정렬.
- GET은 읽음이나 카드 상태를 변경하지 않는다. 만료 스케줄러가 아직 처리하지 않은 ACTIVE 카드도 시각 기준으로 숨긴다.
- unreadNotificationCount: **알림 도메인이 없는 현 단계에서는 0을 반환하는 임시 값**. 다음 알림 API 구현 시 실제 사용자 미확인 알림 수로 연결한다. 홈에 활성 카드가 없더라도 알림 수는 별도로 조회해야 한다.

활성 카드와 새 후보가 없으면 빈 배열을 반환한다. 사진 URL/후보 근거 등 명세에 없는 상세 필드는 추가하지 않는다.

## 기존 수색카드 API 연결

- 목록 bestCandidateScore: 카드의 유효 후보 중 최고 적합도.
- 상세 candidateCount: 상위 5개 제한 전 유효 후보 전체 수. 확인한 후보도 포함한다.
- 상세 bestCandidateScore: 목록과 같은 최고 적합도.
- 유효 후보: 평가 당시 카드 분석 ID·습득물 버전이 현재 값과 같고, NOT_MINE이 아니며, totalScore가 null이 아닌 후보.
- 유효 후보가 없으면 count=0, score=null. 실제 0점이 있으면 count에 포함하고 score=0으로 구분한다.
- NOT_MINE 선택/취소 및 입력 변경은 다음 조회에 반영된다. 과거 NOT_MINE으로 재알림만 금지된 복원 후보는 집계에 포함한다.
- 종료/찾음/만료 카드도 기존 목록·상세에서는 후보 요약을 보존한다. 홈의 활성 필터와 혼동하지 않는다.

CandidateRepository의 공통 CURRENT/INCLUDED 조건을 재사용한다. 소유자 조건을 집계 쿼리에도 적용한다. 카드 ID 묶음별 GROUP BY 한 번으로 집계하며 경찰/카드 정보는 to-one fetch로 조회한다.

## 검증

```bash
./gradlew build --no-daemon
```

홈/집계 통합 테스트는 권한, 빈 상태, 만료 경계, 안정 정렬, 최대 5개, 읽음·제외·복원, null/0, 오래된 평가, 카드 목록·상세 일치, 12개 카드에서도 5개 이하 조회 쿼리를 확인한다.

폐기 가능한 MySQL 준비 방법은 [후보 저장 구조](candidate-storage.md)를 참고한다. 운영/공유 DB로 실행하지 않는다.

```bash
DASI_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:13316/candidate_test?serverTimezone=Asia/Seoul' \
  ./gradlew test --tests '*HomeApiMySqlIntegrationTest' --no-daemon
```

실제 DATA 수집/AI 매칭으로 후보 생성, 알림 생성·발송과 알림 API는 후속 작업이다.

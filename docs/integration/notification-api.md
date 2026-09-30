# 서비스 내 알림 API (#67)

갱신: 2026-09-30. 명세 25~28의 조회·읽음 API와 홈의 실제 미확인 알림 수를 연결한다.

## 경로 및 동작

| Method | 경로 (/api/v1 생략) | 응답 result |
| --- | --- | --- |
| GET | /notifications | content, page, size, totalElements, hasNext |
| POST | /notifications/{notificationId}/read | notificationId, isRead=true |
| POST | /notifications/read-all | updatedCount |
| GET | /notifications/unread-count | unreadCount |

- 모든 API는 Access Token 필수. 비로그인 COMMON4011, 존재하지 않는 사용자 토큰 AUTH4012.
- 목록은 본인 알림만 조회. unreadOnly=false, page=0, size=20 기본값이며 size=1~100, page>=0. 정수 범위를 넘는 offset이나 잘못된 쿼리는 COMMON4001.
- 정렬은 createdAt DESC, id DESC. 읽지 않은 알림 필터도 같은 정렬/페이지를 따른다.
- 개별 읽음은 타인 알림이면 COMMON4031, 없으면 COMMON4041. 잘못된 ID는 COMMON4001.
- readAt이 null일 때만 갱신하는 조건부 SQL을 사용한다. 이미 읽은 알림의 최초 시간은 보존하며 반복 요청도 200이다.
- 전체 읽음은 본인의 미확인 행만 한 번의 UPDATE로 변경한다. updatedCount는 실제 변경 건수이며 반복하면 0이다. 해당 UPDATE 후 생성된 알림은 자동으로 읽음 처리하지 않는다.
- GET은 상태를 변경하지 않는다. 알림의 읽음과 후보 viewedAt·피드백·재알림 금지는 독립적이다.
- 홈 unreadNotificationCount는 동일한 사용자별 미확인 알림 집계를 사용한다. 활성 카드가 없어도 정상 집계한다.

## 저장 구조 및 연결

Flyway V10에서 notification을 추가한다. user_id FK, 선택 candidate_id/search_card_id FK, type/title/message/created_at/read_at을 저장한다. isRead는 readAt 존재 여부로 계산하고 중복 플래그를 저장하지 않는다.

| type | referenceType | referenceId |
| --- | --- | --- |
| NEW_CANDIDATE | CANDIDATE | 후보 ID |
| SEARCH_EXPIRING | SEARCH_CARD | 수색카드 ID |
| SEARCH_EXPIRED | SEARCH_CARD | 수색카드 ID |
| SYSTEM | null | null |

- 수신자/후보/수색카드는 저장된 엔티티여야 한다. 생성 팩토리는 후보·카드 소유자가 수신자와 같은지 확인한다.
- DB는 타입과 참조 조합을 CHECK로 검증한다. 소유권 일치 검증은 생성 팩토리의 책임이며 후속 생성 코드는 이 팩토리를 사용해야 한다.
- referenceType/referenceId는 실제 FK 연결에서 도출한다. 임의 URL을 받아 저장하지 않는다.
- 사용자/참조 후보/참조 카드 삭제 시 관련 알림은 FK CASCADE로 삭제한다. 카드 삭제는 후보를 거쳐 후보 알림까지 삭제한다. 시스템 알림과 다른 카드/사용자 알림은 유지된다.
- 오래된 평가·제외된 후보의 과거 알림은 카드가 삭제되지 않았다면 기록으로 남는다. 이동 대상 상세가 404이면 FE는 현재 조회할 수 없음을 안내하고 목록으로 돌아갈 수 있어야 한다.
- title은 1~200자, message는 1~2000자(공백만 있는 값 불가).
- 사용자/생성시각 정렬 인덱스와 사용자/readAt 인덱스를 추가한다.

## 이번 범위와 후속 작업

이번 작업은 **이미 저장된 알림의 조회와 읽음 관리**다. 외부 공개 알림 생성 API는 없다. 시스템 알림도 저장된 경우에만 조회된다.

후속 작업:

- 최초/신규 후보 및 수색 만료 이벤트에서 알림을 자동 생성.
- 후보 최소 근거·상위 후보 진입·notificationSuppressed·ACTIVE/만료 정책 검사.
- 트랜잭션/발송 이력 기반 중복 생성·중복 이메일 방지, 재시도.
- 사용자 이메일 수신 동의 확인 후 메일 발송.

생성 팩토리 자체는 위 발송 정책이나 중복 방지를 수행하지 않는다. 읽음 상태를 발송 이력으로 사용하면 안 된다.

## 검증

```bash
./gradlew build --no-daemon
```

폐기 가능한 독립 MySQL을 준비한 뒤 아래 검증을 실행한다. 준비 방법은 [후보 저장 구조](candidate-storage.md) 참조. 운영/공유 DB는 사용하지 않는다.

```bash
DASI_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:13316/candidate_test?serverTimezone=Asia/Seoul' \
  ./gradlew test --tests '*NotificationApiMySqlIntegrationTest' --no-daemon
```

권한, 정렬·페이지·필터, 반복 읽음과 실제 변경 수, null 참조, 후보 읽음과의 독립성, 홈 집계, 삭제 연계, MySQL V1~V10 및 CHECK/FK 검증을 포함한다.

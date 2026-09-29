# 후보 조회·읽음·피드백 API (#63)

기준: 2026-09-15 제출 기획서 및 자료실 API 상세명세 20~24. 갱신: 2026-09-29.

## 구현 경로

모든 요청은 Access Token이 필요하다. 본인 카드/후보만 허용하며 다른 사용자 소유이면 403, 리소스가 없으면 404, 존재하지 않는 사용자 토큰은 401이다. 성공 응답은 기존 ApiResDTO 형식이다.

| Method | 경로 | 결과 |
| --- | --- | --- |
| GET | /api/v1/search-cards/{searchCardId}/candidates | 목록·페이지·순위 |
| GET | /api/v1/candidates/{candidateId} | 습득물·6개 점수·일치/충돌/부족 근거 |
| POST | /api/v1/candidates/{candidateId}/view | 최초 viewedAt |
| PUT | /api/v1/candidates/{candidateId}/feedback | feedback, isExcluded |
| DELETE | /api/v1/candidates/{candidateId}/feedback | feedback=null, isExcluded=false |

## 목록과 순위

- page 기본 0, size 기본 5(1~100), minScore 선택(0~100), includeExcluded 기본 false.
- page는 0 이상이며 DB 페이지 offset이 정수 범위를 넘으면 400이다.
- 고정 점수 하한은 없다. totalScore=null만 목록에서 제외하며 실제 0점은 남긴다.
- 정렬: totalScore DESC → foundDate DESC(null 마지막) → candidateId ASC.
- rank는 해당 카드의 최신·점수 있음·비제외 후보 집합 전체에서 1부터 시작한다. 페이지를 바꾸거나 minScore를 적용해도 순번을 다시 1로 시작하지 않는다.
- includeExcluded=true는 제외 후보도 함께 정렬한다. 제외 후보의 rank는 null이고 다른 후보의 순번을 차지하지 않는다.
- totalElements/hasNext는 요청 필터를 적용한 목록 기준이다. 기본 화면은 최대 5개이며 추가 페이지 조회는 유지한다.
- 상세의 미계산 후보는 totalScore와 rank가 null이다. 제외 후보의 상세도 열람할 수 있으나 rank는 null이다.
- 조회만으로 읽음이나 피드백을 변경하지 않는다. isNew는 viewedAt이 없는지를 뜻한다.
- 경찰 항목은 to-one fetch로, 추천 근거는 batch fetch로 로드한다. 컬렉션 fetch join으로 DB 페이지가 메모리 페이지로 바뀌는 것을 피한다. 순위 쿼리는 페이지의 첫 비제외 후보에서 한 번만 실행한다.

## 읽음·피드백

- 읽음은 최초 시간만 저장한다. 반복 POST도 같은 viewedAt으로 200을 반환한다(기존 문서의 409 중복 오류를 제거).
- PUT 본문은 `{"feedback":"NOT_MINE"}` 형식. VERY_SIMILAR/UNSURE/NOT_MINE 문자열만 허용한다.
- 필드 누락/null은 COMMON4004, 알 수 없는 문자열·숫자·객체 등은 COMMON4001이다.
- NOT_MINE은 기본 목록에서 제외하고 notificationSuppressed=true를 영구적으로 기록한다.
- 피드백 변경/삭제 시 목록 제외는 해제될 수 있지만 재알림 금지 이력은 지우지 않는다. 삭제는 반복해도 성공한다.
- 재평가 시 후보 ID·읽음·피드백·재알림 금지를 보존한다.
- 읽음과 피드백 쓰기는 후보 행 잠금으로 직렬화한다. 재평가에는 기존 @Version도 적용된다.
- 종료/찾음/만료 카드도 보존된 최신 후보의 열람·피드백은 허용한다. 자동 매칭/알림 대상 여부와는 별개다.

## 입력 변경과 오래된 점수

V9에서 candidate에 feedback, viewed_at, notification_suppressed, assessed_analysis_id, assessed_police_item_version을 추가한다.

카드의 입력 수정은 기존 서비스가 새 analysisId를 사용하고 이전 분석을 삭제한다. 평가 당시 analysisId와 습득물 @Version을 후보에 기록한다. 둘 중 하나라도 현재 값과 다르면 목록에서 숨기고 상세·읽음·피드백은 404를 반환한다. FE는 목록을 새로 조회하고 재분석을 기다리도록 안내할 수 있다. 상태 종료만으로 후보가 오래된 것으로 처리되지는 않는다.

입력 버전이 없는 기존 후보는 V9에서 값을 임의로 채우지 않는다. 재평가 전까지 숨긴다. assessed_analysis_id는 삭제된 이전 분석 ID도 유지해야 하므로 FK가 아니다.

**후속 AI 연동 시 필수:** 비동기 요청에 입력 버전을 함께 보내고, 응답 저장 시 요청 버전과 현재 버전이 같은지 트랜잭션 안에서 검증해야 한다. 현재 create/replaceAssessment는 전달받은 평가가 연결된 카드/습득물 상태에 대한 것임을 전제로 버전을 기록한다. 습득물 변경은 먼저 flush하여 실제 버전이 올라간 뒤 평가를 저장한다. 버전 확인 없이 지연된 AI 결과를 현재 결과로 저장하면 안 된다. 사진·장소를 별도로 바꾸는 API를 추가할 경우 동일하게 입력 버전을 갱신해야 한다.

## 검증 및 남은 범위

`./gradlew build --no-daemon`으로 전체 테스트를 실행한다.

선택 MySQL 테스트는 [저장 구조 문서](candidate-storage.md)의 폐기 가능한 독립 DB를 사용하고 다음처럼 두 클래스를 함께 실행한다.

```bash
DASI_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:13316/candidate_test?serverTimezone=Asia/Seoul' \
  ./gradlew test --tests '*Candidate*MySqlIntegrationTest'
```

일반 CI에는 외부 MySQL이 없어 MySQL 전용 테스트를 건너뛴다. H2 테스트만으로 SQL migration이 검증되는 것은 아니다.

실제 AI 매칭/수집, 알림 발송, 홈·수색카드 요약(candidateCount/bestCandidateScore) 연결은 후속 작업이다. 이 API는 저장된 최신 후보를 조회하며 후보를 자동 생성하지 않는다. 점수는 소유권 확률이 아니다.

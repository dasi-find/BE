# 후보·습득물 저장 계약 (#61)

기준: 2026-09-15 제출 기획서 / 갱신: 2026-09-29. 이전 로컬 기획의 동일 카테고리 강제 필터, 고정 50/65/80점, 미확인 24시간 재알림은 폐기한다.

## 변경된 제품 정책

- 출처는 경찰관서(POLICE)와 포털기관(PORTAL). 초기 수집 주기는 하루 1회.
- 카테고리는 속성 근거이며 다른 분류라는 이유로 후보를 제거하지 않는다.
- 6개 비교 요소: imageScore, textScore, imageTextScore, attributeScore, dateScore, locationScore.
- 비교 불가 null과 실제 불일치 0을 구분한다. 전부 비교 불가이면 최종 점수 null.
- 항목별 저장/API 점수는 **보정 후 0~1**, 최종 적합도 0~100, 근거 충족도 0~1.
- 모델 고유 원출력(음수가 가능한 코사인 등)은 보정 점수와 다르다. 이번 범위는 보정 점수 저장이며 원출력 상세는 후속 AI 계약에서 정의한다.
- 가중치·보정·신뢰도 규칙과 최소 근거 기준은 검증 후 버전으로 고정한다. 모델 종류나 가중치 수치를 Java에 고정하지 않는다.
- 최종 계산: 100 × Σ(보정 점수 × 가중치 × 신뢰도) / Σ(가중치 × 신뢰도). 시각 요소의 중복 반영을 막는 공통 상한을 검증 후 정한다.
- 순위는 제외되지 않은 계산 가능한 후보의 상위 5개. 신규·변경 후보가 상위 5개에 진입하고 최소 근거·정보 품질을 충족할 때 이력을 확인해 알림한다.
- ACTIVE·만료 전 카드만 매칭/알림하며 NOT_MINE과 동일 후보의 중복 알림을 막는다. 읽지 않았다는 이유로 재알림하지 않는다.
- 실제 습득 위치와 보관기관을 구분하고, 불명확한 위치 점수는 null. 보관기관은 낮은 신뢰도 참고값이며 거리가 멀다고 자동 탈락시키지 않는다.

## 저장 구조

| 테이블 | 역할 | 식별/삭제 |
| --- | --- | --- |
| police_item | 정규화된 공식 표시 정보·원문 설명 | source + management_no + item_sequence 유일 |
| candidate | 카드/습득물 연결, 6개 보정 점수·최종점수·근거 충족도·버전 | 카드/습득물 쌍 유일, 카드 삭제 시 CASCADE |
| candidate_evidence | 순서 있는 일치·충돌·정보 부족 근거 | candidate_id + sort_order, 후보 삭제 시 CASCADE |

- 공개 습득물은 카드 삭제 시 보존한다. 참조 중인 습득물 삭제는 FK가 거절한다.
- 원본 관리번호는 대소문자를 구분한다. 순번은 1 이상의 정규화 정수다. 숫자가 아닌 순번이나 누락은 DATA 정규화 단계에서 처리하고 임의 기본값으로 덮지 않는다.
- 출처가 다르면 같은 관리번호라도 별도 항목이다. 출처 간 중복 그룹은 후속 작업이다.
- 표시 데이터는 PoliceItemDetails로 전달하며 외부 응답을 Entity로 직접 사용하지 않는다. foundPlace와 storagePlace를 구분한다.
- 점수는 소수 6자리, 최종 점수는 소수 4자리까지 저장한다. 초과 정밀도는 암묵 반올림하지 않고 거절한다. 계산기가 명시적으로 반올림한 결과를 전달한다.
- 전체 비교 불가이면 coverage=0, total=null. total이 있으면 하나 이상의 비교 가능 항목과 양수 coverage가 필요하다.
- 근거는 MATCH/CONFLICT/MISSING 유형 및 IMAGE/TEXT/IMAGE_TEXT/ATTRIBUTE/DATE/LOCATION 요소를 기록한다. 한 요소의 일부 속성 누락과 충돌은 함께 존재할 수 있다.
- modelVersion은 모델 조합 manifest 식별자, preprocessingVersion은 전처리 manifest, scorePolicyVersion은 보정·가중치·신뢰도·근거 충족도 규칙 식별자다. 각 manifest의 내용/보관은 AI·랭킹 연동에서 정의한다.
- 순위는 이 테이블에 고정 저장하지 않는다. 후속 조회에서 제외/현재 데이터에 따라 계산한다.
- 재분석은 기존 후보 ID를 유지하여 점수·근거·버전을 한 트랜잭션에서 교체한다. @Version은 동시 갱신 유실을 방지한다.
- #63 조회 API는 카드 분석 ID/습득물 버전을 비교하여 변경 전 평가를 숨긴다. 실제 재매칭과 비동기 결과 저장 시 입력 버전 검증은 후속 매칭 통합에서 연결한다.

## FE·AI·DATA 계약 변경

기존 후보 상세 예시의 0~100 항목 점수를 0~1로 변경한다. timeScore → dateScore, stationProximityScore → locationScore, imageTextScore 추가. evidenceCoverage 및 scorePolicyVersion을 표시하며, evidenceDetails로 일치·충돌·정보 부족을 표현한다. reasons 문자열 목록은 표시 호환용으로 유지한다. 최종 totalScore는 0~100을 유지한다. 기존 인증·수색카드 API는 변경하지 않는다.

후보 목록은 기본 page=0, size=5이며 고정 minScore 하한을 두지 않는다. 정렬은 totalScore DESC, foundDate DESC(null 마지막), id ASC다. 상세 점수 null을 0으로 바꾸지 않는다.

## #61 범위와 후속 작업

#61은 Entity/Repository, Flyway V8, 저장 제약 및 카드 삭제 연계 테스트만 구현한다. 공공 API 수집, 좌표·출처별 원본 응답/전처리/임베딩 저장, 계산 알고리즘, 후보 조회·피드백·읽음, 알림, 수색 연장은 후속 기능이다. 기존 수색카드 응답의 candidateCount 등은 연결 전까지 기존 동작을 유지한다.

자료실의 기획안·API 상세명세·처리 흐름 MD도 갱신했으나 자료실은 BE Git 저장소 밖이다. 이 문서를 PR에서 추적할 정책·저장 계약으로 사용한다.

#63에서 후보 조회·읽음·피드백과 입력 버전 검사를 추가했다. 현재 동작은 [후보 API 계약](candidate-api.md)을 따른다. 아래 MySQL 테스트는 현재 Flyway V1~V10을 검증한다.

## 검증

기본 H2 통합 테스트는 null/0 보존, 출처별 중복, 카드/습득물 중복, 재분석 교체, 카드 삭제 및 공유 데이터 보존을 확인한다. MySQL 전용 테스트는 Flyway V1~V10 실행과 실제 DB CHECK 제약을 검증한다.

```bash
./gradlew build
```

MySQL 테스트는 환경변수가 없으면 건너뛴다. 반드시 운영/개발 공유 DB가 아닌 **폐기 가능한 독립 테스트 DB**를 사용한다. Flyway가 전체 migration을 적용한다.

```bash
docker run -d --name dasi-candidate-test -p 127.0.0.1:13316:3306 \
  -e MYSQL_RANDOM_ROOT_PASSWORD=yes -e MYSQL_DATABASE=candidate_test \
  -e MYSQL_USER=candidate_test -e MYSQL_PASSWORD=candidate_test_only mysql:8.4
# MySQL 초기화가 완료된 뒤 실행한다. 아래 계정은 테스트 전용 고정 계정이다.
DASI_MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:13316/candidate_test?serverTimezone=Asia/Seoul' \
  ./gradlew test --tests '*CandidateMySqlIntegrationTest'
# 위에서 만든 일회성 컨테이너와 테스트 데이터만 제거한다.
docker rm -fv dasi-candidate-test
```

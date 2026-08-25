# 다시찾음 Backend

다시찾음의 인증, 수색카드, 후보, 알림 및 외부 서비스 연동을 담당하는 API 서버입니다.

## 기술 스택

- Java 21
- Spring Boot 4.1
- Gradle
- Spring MVC
- Spring Data JPA
- Spring Security
- MySQL
- Flyway

## 실행 준비

Java 21과 Docker Desktop이 필요합니다. 먼저 로컬 환경변수 파일을 만듭니다.

```bash
cp .env.example .env
```

`.env`의 `DB_PASSWORD`와 `DB_ROOT_PASSWORD`를 로컬에서 사용할 값으로 변경합니다. 실제 비밀번호가 담긴 `.env`는 Git에 커밋하지 않습니다.
로컬의 3306 포트를 다른 MySQL이 사용 중이면 `.env`의 `DB_PORT`를 3307 등 빈 포트로 변경합니다.

로컬 MySQL을 실행합니다.

```bash
docker compose up -d
docker compose ps
```

`dasi-find-mysql`의 상태가 `healthy`가 되면 애플리케이션을 실행할 수 있습니다.

## 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

시작 시 Flyway가 `src/main/resources/db/migration`의 migration을 순서대로 적용합니다. 로컬 DB에서는 `flyway_schema_history` 테이블로 적용 이력을 확인할 수 있습니다.

상태 확인:

```text
GET http://localhost:8080/actuator/health
```

Spring Security가 추가된 상태이므로 인증 설정 구현 전에는 임시 비밀번호가 로그에 출력될 수 있습니다.

## 테스트 및 빌드

```bash
./gradlew test
./gradlew build
```

테스트는 `test` 프로필과 In-Memory H2 Database를 사용하여 로컬 MySQL 없이 실행됩니다.

## 프로필

| Profile | 용도 | Database |
|---|---|---|
| `local` | 로컬 개발 | MySQL |
| `test` | 자동 테스트 | H2 In-Memory |

배포 환경이 결정되면 `prod` 프로필을 추가합니다.

## Git Flow

모든 작업은 Issue 생성 후 `develop`에서 작업 브랜치를 생성해 진행합니다.

```text
feat/{Issue 번호}-{작업 내용} → develop → main
```

자세한 규칙은 Organization의 `.github/CONVENTION.md`를 확인해 주세요.

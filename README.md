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

Java 21과 MySQL이 필요합니다. 로컬 환경변수는 `.env.example`을 참고하여 개인 환경에 설정합니다.

```text
DB_HOST=localhost
DB_PORT=3306
DB_NAME=dasi_find
DB_USERNAME=dasi_find
DB_PASSWORD=
```

실제 비밀번호는 Git에 커밋하지 않습니다.

## 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버는 기본적으로 `http://localhost:8080`에서 실행됩니다.

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

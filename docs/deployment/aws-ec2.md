# AWS EC2 운영 배포

## 구성

- EC2: Ubuntu, Docker Engine, Docker Compose
- 외부 공개: Nginx `80` 포트만 공개
- 내부 통신: Spring Boot `8080`, MySQL `3306`, Redis `6379`
- 영속 데이터: Docker named volume
- HTTPS: CloudFront를 Nginx 앞에 연결
- 이미지: 서울 리전의 비공개 S3 버킷과 1시간 Presigned URL 사용

## EC2 최초 준비

Docker 설치 후 메모리 부족에 대비해 2GB swap을 설정합니다.

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

EC2 보안 그룹은 다음 인바운드 규칙만 허용합니다.

- `22`: 관리자 현재 IP만 허용
- `80`: CloudFront 연결 전 임시로 IPv4 전체 허용
- `443`: EC2에서 직접 TLS를 종료하지 않으므로 사용하지 않음

`8080`, `3306`, `6379`는 외부에 공개하지 않습니다.

MySQL은 IntelliJ SSH 터널 접속을 위해 EC2 loopback 주소
`127.0.0.1:3306`에만 연결합니다. 인터넷에서 접근할 수 있는
`0.0.0.0:3306`으로 변경하거나 보안 그룹에 `3306` 인바운드 규칙을
추가하지 않습니다.

## 최초 배포

```bash
git clone --branch main --single-branch https://github.com/dasi-find/BE.git
cd BE
chmod +x deploy/create-prod-env.sh deploy/deploy.sh deploy/update-from-main.sh
./deploy/create-prod-env.sh
./deploy/deploy.sh
```

환경변수 생성 스크립트는 DB, Redis, JWT 비밀값을 임의 생성하고 `.env.prod`를 권한 `600`으로 저장합니다. Google 앱 비밀번호 입력값은 화면에 표시되지 않습니다.

`.env.prod`는 Git에 커밋하거나 메신저로 공유하지 않습니다.

## 비공개 S3 이미지 저장소 연결

S3 콘솔에서 **범용 버킷 만들기**를 선택하고 다음 값으로 생성합니다.

| 항목 | 설정값 |
|---|---|
| AWS 리전 | 아시아 태평양(서울) `ap-northeast-2` |
| 버킷 이름 | 전 세계에서 고유한 이름 |
| 객체 소유권 | ACL 비활성화(버킷 소유자 적용) |
| 퍼블릭 액세스 차단 | 네 항목 모두 활성화 |
| 버킷 버전 관리 | 비활성화 |
| 기본 암호화 | SSE-S3 |

버킷 정책으로 공개 권한을 추가하지 않습니다. 애플리케이션은 비공개 객체를
업로드하고, 조회할 때만 1시간 유효한 Presigned URL을 발급합니다.

EC2의 `dasifind-ec2-ssm-role` 역할에 다음 인라인 정책을 추가합니다.
`<BUCKET_NAME>`은 생성한 버킷 이름으로 바꿉니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SearchCardImageObjectAccess",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::<BUCKET_NAME>/search-card-images/*"
    }
  ]
}
```

장기 Access Key는 생성하지 않습니다. Docker 컨테이너 안의 AWS SDK가 EC2
인스턴스 역할을 IMDSv2로 읽을 수 있도록 EC2 콘솔의 **작업 → 인스턴스 설정 →
인스턴스 메타데이터 옵션 수정**에서 다음과 같이 설정합니다.

| 항목 | 설정값 |
|---|---|
| 인스턴스 메타데이터 서비스 | 활성화 |
| IMDSv2 | 필수 |
| PUT 응답 홉 제한 | `2` |

기존 서버의 `.env.prod`에는 아래 두 줄을 직접 추가합니다.

```bash
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=<BUCKET_NAME>
```

새 서버에서는 `create-prod-env.sh`가 버킷 이름을 질문하고 두 값을 생성합니다.
S3 설정을 마치기 전에는 이미지 업로드 기능이 포함된 배포를 진행하지 않습니다.

## 상태 확인

```bash
docker compose --env-file .env.prod -f compose.prod.yaml ps
curl --fail http://localhost/actuator/health
```

모든 컨테이너가 `healthy` 또는 `running`이고 헬스체크가 `{"status":"UP"}`이면 정상입니다. Spring Boot 시작 시 Flyway가 운영 MySQL에 migration을 적용합니다.

애플리케이션 로그는 다음 명령으로 확인합니다.

```bash
docker compose --env-file .env.prod -f compose.prod.yaml logs --tail=200 backend
```

## IntelliJ에서 운영 MySQL 조회

IntelliJ의 **Database** 도구에서 `+` → **Data Source** → **MySQL**을
선택합니다. MySQL 드라이버가 없다면 IntelliJ가 표시하는 다운로드 버튼으로
설치합니다.

**General** 탭에는 다음 값을 입력합니다.

| 항목 | 값 |
|---|---|
| Host | `127.0.0.1` |
| Port | `3306` 또는 `.env.prod`의 `DB_TUNNEL_PORT` |
| Database | `.env.prod`의 `DB_NAME` |
| User | `.env.prod`의 `DB_USERNAME` |
| Password | `.env.prod`의 `DB_PASSWORD` |

**SSH/SSL** 탭에서 **Use SSH tunnel**을 켜고 다음 값을 입력합니다.

| 항목 | 값 |
|---|---|
| Proxy host | EC2 공인 IP 또는 탄력적 IP |
| Port | `22` |
| User | `ubuntu` |
| Authentication type | Key pair |
| Private key file | EC2 접속용 `.pem` 파일 |

**Test Connection**이 성공하면 `Schemas`에서 `dasi_find`와
`flyway_schema_history`, `user` 테이블을 확인할 수 있습니다. 운영 회원을
조회할 때 비밀번호 컬럼은 불필요하게 열람하거나 공유하지 않습니다.

연결되지 않으면 EC2에서 loopback 포트와 컨테이너 상태를 확인합니다.

```bash
ss -lnt | grep 3306
docker compose --env-file .env.prod -f compose.prod.yaml ps mysql
```

## 재배포

운영 서버는 항상 `main` 브랜치만 추적합니다.

```bash
cd ~/BE
./deploy/update-from-main.sh
```

GitHub Actions는 `develop` push의 BE CI가 성공하면 검증된 커밋을 `main`으로 자동 승격하고 BE CD를 호출합니다. BE CD는 AWS Systems Manager로 위 스크립트를 실행합니다. `main` push와 수동 workflow dispatch도 같은 CD를 실행하며, GitHub에는 장기 AWS Access Key나 SSH 개인키를 저장하지 않습니다.

`develop` PR 검사만 성공한 상태에서는 승격하지 않습니다. PR이 `develop`에 머지된 후 실행된 push CI가 성공해야 운영 배포가 시작됩니다. GitHub Actions가 생성한 `main` 병합은 후속 push 워크플로를 자동 실행하지 않으므로 승격 워크플로가 BE CD를 명시적으로 dispatch합니다.

CD는 SSM 명령의 최종 상태를 최대 15분간 확인합니다. 최초 Docker 이미지 빌드가 AWS CLI의 기본 waiter 대기 시간을 넘겨도 실제 배포 결과가 나올 때까지 기다립니다.

EC2에 `/home/ubuntu/BE` 저장소가 없는 최초 배포 상황에서는 CD가 `main` 단일 브랜치를 자동으로 clone합니다. 기존 저장소가 있으면 `main`을 fast-forward 방식으로 먼저 갱신한 뒤 배포합니다. `.env.prod`는 Git에 포함되지 않으므로 최초 배포 전에 서버에서 한 번 생성해야 합니다.

자동 배포에는 다음 구성이 필요합니다.

- EC2 인스턴스 역할: `AmazonSSMManagedInstanceCore` 권한
- GitHub OIDC 공급자: `https://token.actions.githubusercontent.com`, 대상 `sts.amazonaws.com`
- GitHub 배포 역할: 해당 EC2에 대한 Systems Manager 명령 실행 권한
- GitHub 저장소 변수 `AWS_DEPLOY_ROLE_ARN`, `EC2_INSTANCE_ID`

## CloudFront로 HTTPS 연결

Vercel HTTPS 페이지에서 HTTP EC2 주소를 직접 호출하면 브라우저의
mixed content 보안 정책에 막힙니다. CloudFront가 브라우저와는 HTTPS로,
EC2 Nginx와는 HTTP로 통신하도록 구성합니다.

AWS 콘솔의 **CloudFront** → **배포** → **배포 생성**에서 다음과 같이
설정합니다. 원본 도메인에는 EC2 콘솔에 표시된
`<EC2_PUBLIC_IPV4_DNS>` 값을 입력합니다. 탄력적 IP 숫자만 입력하지
않습니다.

| 항목 | 설정값 |
|---|---|
| 원본 유형 | 기타 원본(Custom origin) |
| 원본 도메인 | EC2 퍼블릭 IPv4 DNS |
| 프로토콜 | HTTP만 |
| HTTP 포트 | `80` |
| 뷰어 프로토콜 정책 | HTTP에서 HTTPS로 리디렉션 |
| 허용된 HTTP 메서드 | GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE |
| 캐시 정책 | `CachingDisabled` |
| 원본 요청 정책 | `AllViewer` |
| 원본 Shield | 사용 안 함 |
| 웹 애플리케이션 방화벽(WAF) | 일단 사용 안 함 |
| 가격 등급 | `Price Class 200`(한국 포함) |
| 대체 도메인/CNAME | 비워 둠 |
| SSL 인증서 | CloudFront 기본 인증서 |

API 응답이 다른 사용자에게 재사용되지 않도록 AWS 관리형
`CachingDisabled` 정책을 사용합니다. `AllViewer`는 인증 헤더, 쿠키,
쿼리 스트링과 `Origin` 헤더를 EC2까지 전달하기 위해 필요합니다.

배포 상태가 **활성화됨**이 되면 표시된
`https://dxxxxxxxxxxxxx.cloudfront.net`에서 헬스체크를 확인합니다.

```bash
curl --fail https://dxxxxxxxxxxxxx.cloudfront.net/actuator/health
```

`{"status":"UP"}`이면 EC2 보안 그룹의 `80 / 0.0.0.0/0` 규칙을 삭제하고,
소스를 AWS 관리형 접두사 목록
`com.amazonaws.global.cloudfront.origin-facing`으로 지정한 `HTTP 80` 규칙으로
교체합니다. 교체 전 CloudFront 헬스체크가 성공했는지 먼저 확인합니다.

CloudFront 요금은 AWS 공식 요금 페이지의 현재 제도를 확인하고
선택합니다. 이 구성은 Origin Shield, 실시간 로그, Lambda@Edge를
사용하지 않으며 AWS 예산 알림은 계속 유지합니다.

## Vercel에서 같은 출처로 API 연결

권장 방식은 브라우저가 CloudFront 도메인을 직접 호출하지 않고,
Vercel의 `/api` 경로를 통해 호출하는 것입니다. FE 저장소의
`vercel.json`에 다음 rewrite를 설정합니다.

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://dxxxxxxxxxxxxx.cloudfront.net/api/:path*"
    }
  ]
}
```

FE의 API 기본 주소는 외부 URL 대신 `/api`를 사용하고, 인증 요청은
cookie credentials를 포함합니다. Vercel 외부 rewrite는 브라우저 주소를
바꾸지 않고 API 요청을 CloudFront로 전달하므로 서드파티 쿠키에
의존하지 않습니다. 이 방식은 운영 `.env.prod`의
`AUTH_COOKIE_SAME_SITE=LAX`를 유지합니다.

CloudFront 주소를 FE에서 직접 호출해야 하는 경우에만
`.env.prod`의 `AUTH_COOKIE_SAME_SITE=NONE`으로 변경하고 재배포합니다.
`SameSite=None`은 `Secure=true`와 함께만 사용할 수 있으며, 브라우저의
서드파티 쿠키 차단 정책에 영향받을 수 있어 권장하지 않습니다.

## 재부팅 검증

모든 서비스에 `restart: unless-stopped`가 적용되어 있으므로 EC2 재부팅 후 자동으로 다시 시작됩니다.

```bash
sudo reboot
```

재접속 후 상태를 확인합니다.

```bash
cd ~/BE
docker compose --env-file .env.prod -f compose.prod.yaml ps
```

## 데이터 보호

MySQL과 Redis 데이터는 Docker named volume에 저장됩니다. 컨테이너를 다시 만들어도 유지되지만 `docker compose down -v`를 실행하면 삭제되므로 운영 서버에서 `-v` 옵션을 사용하지 않습니다.

EC2 종료 전에는 MySQL 백업과 EBS 스냅샷을 생성합니다. 사용하지 않는 EBS 볼륨과 탄력적 IP는 별도 비용이 발생할 수 있으므로 직접 정리합니다.

# AWS EC2 운영 배포

## 구성

- EC2: Ubuntu, Docker Engine, Docker Compose
- 외부 공개: Nginx `80` 포트만 공개
- 내부 통신: Spring Boot `8080`, MySQL `3306`, Redis `6379`
- 영속 데이터: Docker named volume
- HTTPS: CloudFront를 Nginx 앞에 연결
- 이미지: 후속 작업에서 비공개 S3 버킷 사용

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

## 재배포

운영 서버는 항상 `main` 브랜치만 추적합니다.

```bash
cd ~/BE
./deploy/update-from-main.sh
```

GitHub Actions는 `main` push를 감지해 테스트를 통과한 뒤 AWS Systems Manager로 위 스크립트를 실행합니다. GitHub에는 장기 AWS Access Key나 SSH 개인키를 저장하지 않습니다.

자동 배포에는 다음 구성이 필요합니다.

- EC2 인스턴스 역할: `AmazonSSMManagedInstanceCore` 권한
- GitHub OIDC 공급자: `https://token.actions.githubusercontent.com`, 대상 `sts.amazonaws.com`
- GitHub 배포 역할: 해당 EC2에 대한 Systems Manager 명령 실행 권한
- GitHub 저장소 변수 `AWS_DEPLOY_ROLE_ARN`, `EC2_INSTANCE_ID`

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

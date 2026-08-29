# 운영 배포 — 단일 서버 (Ubuntu 22.04)

`caddy + web(SPA) + app + mysql + redis` 를 docker compose 로 올린다.

| 역할 | 컨테이너 | 비고 |
|---|---|---|
| TLS·라우팅 | `caddy` | Let's Encrypt 자동 발급·갱신 |
| SPA | `web` | 프론트 레포에서 만든 이미지 (nginx) |
| API | `app` | `/api/*` 만 caddy가 프록시 |
| DB | `mysql` | 8.4, named volume |
| 캐시 | `redis` | AOF 켜짐 (refreshToken·인증코드) |

프론트와 API가 **같은 도메인**이라 CORS가 없고 `Secure` 쿠키가 그대로 동작한다.

## dev와 다른 점

- 소스에서 빌드하지 않는다. GHCR 이미지를 `pull` 한다.
- **`SPRING_JPA_HIBERNATE_DDL_AUTO`를 주지 않는다.** `application-prod.yml`이
  `ddl-auto: none` 이므로 스키마 변경은 마이그레이션으로만 한다 → `docs/schema-migration.md`
- Swagger는 프로필 자체가 비활성(`@Profile("!prod")`)이라 basic auth가 필요 없다.
- `/actuator/*` 는 caddy가 404로 막는다. 헬스체크는 컨테이너 안에서만 돈다.

## 서버 최초 준비

```bash
# 1) Docker (배포판 docker.io 말고 공식 저장소)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER && exit   # 재로그인

# 2) 방화벽
sudo ufw allow OpenSSH
sudo ufw allow 80,443/tcp
sudo ufw enable

# 3) 타임존
sudo timedatectl set-timezone Asia/Seoul

# 4) 디렉터리
mkdir -p ~/bookpool/backups
```

DNS A레코드가 이 서버를 가리켜야 Caddy가 인증서를 받는다. **DNS부터 붙이고 배포한다.**

## 시크릿

`~/bookpool/.env` 하나에 모인다. `.env.example` 을 채워 GitHub Secret **`ENV_FILE`** 에
통째로 넣어두면 CD가 배포 때마다 서버에 기록한다(권한 600).

`DB.PROD.URL` 처럼 점이 든 이름은 셸 변수로 못 쓴다. `.env` 에는 `DB_PROD_URL` 같은
밑줄 이름으로 담고, compose 가 컨테이너에 넣을 때 점 이름으로 바꾼다.

## GitHub Secrets

| 시크릿 | 백엔드 레포 | 프론트 레포 | 값 |
|---|:---:|:---:|---|
| `ENV_FILE` | O | - | `.env.example` 를 채운 **전체 내용** |
| `EC2_HOST` / `DEPLOY_HOST` | O | O | 서버 IP 또는 도메인 |
| `EC2_USER` / `DEPLOY_USER` | O | O | `park` |
| `EC2_SSH_KEY` / `DEPLOY_SSH_KEY` | O | O | 배포용 개인키 (`~/.ssh/authorized_keys` 에 공개키 등록) |

> 백엔드는 `EC2_*`, 프론트는 `DEPLOY_*` 이름을 쓴다. 같은 서버·같은 계정이면 값은 동일하다.

## 최초 배포 순서

`docker compose up -d` 는 `web` 도 같이 띄우므로, **프론트 이미지가 GHCR에 한 번은 올라가 있어야 한다.**

1. DNS A레코드를 서버로 지정 (Caddy 인증서 발급 전제)
2. 서버 준비 (위 "서버 최초 준비")
3. GitHub Secrets 등록
4. **프론트 레포 CD 먼저 실행** (`main` push 또는 workflow_dispatch) → `web` 이미지 생성
5. 백엔드 레포 CD 실행 → compose 파일 배치 + 전체 기동
6. `https://<도메인>` 확인, `docker compose ps` 로 5개 컨테이너 모두 healthy 확인
7. `crontab -e` 로 백업 등록

이후로는 각 레포가 자기 서비스만 재시작한다.

## 배포

`main` 에 머지되면 각 레포의 CD가 자동으로 돈다.

- **백엔드** — 이미지 push → 서버에서 `compose up -d app`
- **프론트** — 이미지 push → 서버에서 `compose up -d web`

두 레포가 `.env.images` 의 자기 키만 갱신하므로 서로 덮어쓰지 않는다.

수동 배포:

```bash
cd ~/bookpool
docker compose pull && docker compose up -d
docker compose ps
```

## 롤백

이미지 태그만 되돌리면 된다.

```bash
cd ~/bookpool
sed -i 's/^APP_IMAGE_TAG=.*/APP_IMAGE_TAG=sha-abc1234/' .env.images
set -a; source .env.images; set +a
docker compose up -d app
```

## 백업

`backup-mysql.sh` 를 cron에 건다.

```bash
crontab -e
# 매일 04:00
0 4 * * * /home/park/bookpool/backup-mysql.sh >> /home/park/bookpool/backups/backup.log 2>&1
```

> 이 스크립트는 **서버 안에** 파일을 만들 뿐이다. 서버가 통째로 죽으면 백업도 같이 죽는다.
> `backups/` 를 주기적으로 서버 밖(S3·다른 호스트)으로 옮기는 것까지가 백업이다.

복구:

```bash
gunzip -c backups/bookpool-20260830-040000.sql.gz \
  | docker exec -i bookpool-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" bookpool
```

## 주의

- **`docker compose down -v` 금지.** `-v` 는 DB·인증서 볼륨까지 지운다.
- `caddy_data` 볼륨을 지우면 인증서를 다시 받는다. Let's Encrypt 발급 한도가 있으니
  테스트로 반복 삭제하지 않는다.
- 메모리 4GB 권장. 2GB면 swap 2GB를 잡아둔다(mysql + redis + JVM 동거).
- 업로드 이미지는 `uploads_data` 볼륨에 있다. 재배포로는 사라지지 않지만,
  이것도 백업 대상이다.

# 운영 배포 — 단일 서버 (Ubuntu 22.04)

`nginx + web(SPA) + app + mysql + redis` 를 docker compose 로 올린다.

| 역할 | 컨테이너 | 비고 |
|---|---|---|
| TLS·라우팅 | `nginx` | certbot 발급, cron으로 갱신 |
| SPA | `web` | 프론트 레포에서 만든 이미지 (nginx) |
| API | `app` | `/api/*` 만 앞단 nginx가 프록시 |
| DB | `mysql` | 8.4, named volume |
| 캐시 | `redis` | AOF 켜짐 (refreshToken·인증코드) |

nginx가 두 번 나온다. 앞단 `nginx`는 **TLS 종료와 라우팅**만 하고,
`web` 이미지 안의 nginx는 **정적 파일 서빙**만 한다.

프론트와 API가 **같은 도메인**이라 CORS가 없고 `Secure` 쿠키가 그대로 동작한다.

## dev와 다른 점

- 소스에서 빌드하지 않는다. GHCR 이미지를 `pull` 한다.
- 프론트를 `dist/` 로 복사하지 않는다. 이미지로 받는다.
- **인증서 갱신이 자동이다.** dev는 certbot을 손으로 돌려야 했다 → `renew-cert.sh` + cron
- **`SPRING_JPA_HIBERNATE_DDL_AUTO`를 주지 않는다.** `application-prod.yml`이
  `ddl-auto: none` 이므로 스키마 변경은 마이그레이션으로만 한다 → `docs/schema-migration.md`
- Swagger는 프로필 자체가 비활성(`@Profile("!prod")`)이라 basic auth가 필요 없다.
- `/actuator/*` 는 앞단 nginx가 404로 막는다. 헬스체크는 컨테이너 안에서만 돈다.

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

# 5) (선택) 루트에서 바로 보이도록 심볼릭 링크
#    반드시 이름을 'bookpool' 로 맞춘다 — 아래 주의 참고
sudo ln -s /home/park/bookpool /bookpool
```

> **심볼릭 링크 이름은 반드시 `bookpool`.**
> docker compose 는 *입력한 디렉터리 이름*으로 프로젝트명을 정한다.
> `/bookpool` 과 `/home/park/bookpool` 은 이름이 같으므로 프로젝트명도 볼륨명도
> (`bookpool_mysql_data`) 동일해 어느 쪽에서 실행하든 같은 컨테이너를 가리킨다.
>
> 이름을 다르게 주면(예: `/app`) **별개 프로젝트로 인식돼 빈 MySQL이 새로 뜬다.**
> 실제 데이터는 원래 볼륨에 남아 있지만, 서비스는 빈 DB를 보게 된다.
>
> 실 경로는 `/home/park/bookpool` 그대로다. CD·cron 은 절대경로를 쓰므로 영향이 없다.

DNS A레코드가 이 서버를 가리켜야 certbot이 인증서를 받는다. **DNS부터 붙이고 배포한다.**

## 인증서 (최초 1회)

`nginx.conf` 가 인증서 파일을 참조하므로 **인증서가 없으면 nginx가 뜨지 못한다.**
그래서 nginx를 띄우기 전에 한 번 받아야 한다.

```bash
cd ~/bookpool
./init-cert.sh          # certbot standalone (80 포트를 직접 잡는다)
docker compose up -d    # 그다음 전체 기동
```

발급물은 `certs/live/bookpool/` 에 생긴다. `--cert-name bookpool` 로 고정했기 때문에
**도메인이 바뀌어도 `nginx.conf` 는 그대로**다.

### 갱신 (cron)

```bash
crontab -e
# 하루 두 번. certbot은 만료 30일 전부터만 실제로 갱신하므로 자주 돌려도 부담이 없다.
0 3,15 * * * /home/park/bookpool/renew-cert.sh >> /home/park/bookpool/certbot.log 2>&1
```

갱신은 nginx를 띄운 채 webroot 방식으로 하고, 끝나면 `nginx -s reload` 한다(무중단).

동작 확인:

```bash
./renew-cert.sh                      # 아직 갱신 시점이 아니면 아무 일도 안 일어난다
docker run --rm -v ~/bookpool/certs:/etc/letsencrypt \
  certbot/certbot certificates       # 만료일 확인
```

## 시크릿

`~/bookpool/.env` 하나에 모인다. `.env.example` 을 채워 GitHub Secret **`ENV_FILE`** 에
통째로 넣어두면 CD가 배포 때마다 서버에 기록한다(권한 600).

`DB.PROD.URL` 처럼 점이 든 이름은 셸 변수로 못 쓴다. `.env` 에는 `DB_PROD_URL` 같은
밑줄 이름으로 담고, compose 가 컨테이너에 넣을 때 점 이름으로 바꾼다.

## GitHub Secrets

| 시크릿 | 백엔드 레포 | 프론트 레포 | 값 |
|---|:---:|:---:|---|
| `ENV_FILE` | O | - | `.env.example` 를 채운 **전체 내용** (`SITE_DOMAIN`·`ACME_EMAIL` 포함) |
| `EC2_HOST` / `DEPLOY_HOST` | O | O | 서버 IP 또는 도메인 |
| `EC2_USER` / `DEPLOY_USER` | O | O | `park` |
| `EC2_SSH_KEY` / `DEPLOY_SSH_KEY` | O | O | 배포용 개인키 (`~/.ssh/authorized_keys` 에 공개키 등록) |

> 백엔드는 `EC2_*`, 프론트는 `DEPLOY_*` 이름을 쓴다. 같은 서버·같은 계정이면 값은 동일하다.

## 최초 배포 순서

`docker compose up -d` 는 `web` 도 같이 띄우므로, **프론트 이미지가 GHCR에 한 번은 올라가 있어야 한다.**

1. DNS A레코드를 서버로 지정 (인증서 발급 전제)
2. 서버 준비 (위 "서버 최초 준비")
3. GitHub Secrets 등록
4. **프론트 레포 CD 먼저 실행** (`main` push 또는 workflow_dispatch) → `web` 이미지 생성
5. 백엔드 레포 CD 실행 → compose·nginx.conf·스크립트가 서버에 배치되고 `.env` 가 기록된다
   (인증서가 아직 없어 nginx는 뜨지 못한다 — 정상)
6. 서버에서 `./init-cert.sh` → `docker compose up -d`
7. `https://<도메인>` 확인, `docker compose ps` 로 컨테이너 상태 확인
8. `crontab -e` 로 **인증서 갱신 + 백업** 두 개 등록

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
# 매일 04:00 백업
0 4 * * * /home/park/bookpool/backup-mysql.sh >> /home/park/bookpool/backups/backup.log 2>&1
# 하루 두 번 인증서 갱신 확인
0 3,15 * * * /home/park/bookpool/renew-cert.sh >> /home/park/bookpool/certbot.log 2>&1
```

> 이 스크립트는 **서버 안에** 파일을 만들 뿐이다. 서버가 통째로 죽으면 백업도 같이 죽는다.
> `backups/` 를 주기적으로 서버 밖(S3·다른 호스트)으로 옮기는 것까지가 백업이다.

복구:

```bash
gunzip -c backups/bookpool-20260830-040000.sql.gz \
  | docker exec -i bookpool-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" bookpool
```

## 주의

- **`docker compose down -v` 금지.** `-v` 는 DB 볼륨까지 지운다.
- `certs/` 를 지우면 인증서를 다시 받아야 한다. Let's Encrypt는 **도메인당 주당 5회**
  발급 한도가 있으니 테스트로 반복 삭제하지 않는다.
- 인증서는 `certs/` 바인드 마운트에 있다. 백업 대상이다.
- 메모리 4GB 권장. 2GB면 swap 2GB를 잡아둔다(mysql + redis + JVM 동거).
- 업로드 이미지는 `uploads_data` 볼륨에 있다. 재배포로는 사라지지 않지만,
  이것도 백업 대상이다.

# 개발(=QA) 서버 배포 — 단일 EC2

EC2 1대에 **nginx + app + mysql + redis** 를 docker compose 로 올린다.
운영(CloudFront+S3+ALB+RDS+ElastiCache)과 같은 *요청 경로/계약*을 흉내내되, 규모/이중화는 단일 박스로 다운그레이드한 구성.

| 역할 | 운영 | 개발(여기) |
|---|---|---|
| 진입/라우팅 | CloudFront | nginx |
| 정적 SPA | S3 | nginx 가 `dist/` 정적 서빙 |
| API | ALB → app | nginx `/api/*` → app:8080 |
| DB | RDS | mysql 컨테이너(8.4) |
| 캐시 | ElastiCache | redis 컨테이너 |

## 준비물

```
deploy/dev/
├─ docker-compose.yml
├─ nginx.conf
├─ application-secret.yml   ← .example 복사 후 값 채우기 (앱 시크릿, 커밋 금지)
│                              app 컨테이너의 /app/config 로 마운트되어 dev 프로필이 로드
├─ .env                     ← .example 복사 (mysql 컨테이너 부트스트랩 값만, 커밋 금지)
├─ .htpasswd                ← Swagger basic auth 계정 (아래 명령으로 생성, 커밋 금지)
├─ dist/                    ← 프론트 빌드 산출물 (CI가 복사 or 수동 업로드)
└─ certs/                   ← fullchain.pem, privkey.pem (certbot 발급)
```

> **Swagger 접근 제한**: `/swagger-ui`, `/v3/api-docs` 는 nginx basic auth 로 잠가 팀만 접근한다.
> 운영(prod)은 Swagger 자체가 비활성화라 노출되지 않는다.

> **값 분리**: 앱이 쓰는 DB/JWT/Redis/Mail/Swagger 는 `application-secret.yml`(중첩 YAML),
> mysql 컨테이너 생성용 값은 `.env` 에 둔다. **`DB_DEV_USERNAME/PASSWORD`(.env) 와
> `DB.DEV.USERNAME/PASSWORD`(secret.yml) 는 같은 값**이어야 한다(앱↔DB 인증 일치).

## 실행

```bash
cd deploy/dev
cp application-secret.yml.example application-secret.yml   # 앱 시크릿 채우기
cp .env.example .env                                       # mysql 부트스트랩 값 채우기
# 프론트 빌드 결과를 ./dist 에 배치 (예: scp 또는 CI artifact)

# Swagger basic auth 계정 생성 (반드시 up 전에! 없으면 docker가 디렉터리를 만들어 버린다)
#   htpasswd 가 있으면:
htpasswd -cbB .htpasswd devteam 'CHANGE-ME-PASSWORD'
#   없으면 docker 로:
#   docker run --rm httpd:2-alpine htpasswd -nbB devteam 'CHANGE-ME-PASSWORD' > .htpasswd

docker compose up -d --build
docker compose ps
docker compose logs -f app
```

## TLS 인증서 (certbot, 최초 1회)

dev 프로필은 쿠키 `Secure=true` 라 **HTTPS 필수**. webroot 방식 예:

```bash
# 80 포트가 nginx 로 열린 상태에서
docker run --rm \
  -v "$PWD/certs:/etc/letsencrypt" \
  -v "$PWD/certbot-www:/var/www/certbot" \
  certbot/certbot certonly --webroot -w /var/www/certbot \
  -d dev.bookpool.co.kr --email you@example.com --agree-tos --no-eff-email

# 발급물이 certs/live/dev.bookpool.co.kr/ 아래 생기므로
# nginx.conf 의 경로를 맞추거나 fullchain/privkey 를 certs/ 로 링크
docker compose restart nginx
```

## 확인 포인트 (운영과 동일하게 동작하는지)

- `https://dev.bookpool.co.kr/` → SPA 뜨고, 딥링크 새로고침(예: `/books/1`) 시 404 안 나야 함
- `https://dev.bookpool.co.kr/api/...` → 백엔드 응답, **CORS 없이** 쿠키 송수신
- 로그인 후 `Set-Cookie` 에 `Secure` 붙고 브라우저가 저장하는지 (HTTPS 라서 동작)
- `https://dev.bookpool.co.kr/swagger-ui/index.html` → **basic auth 창**이 뜨고, 통과해야 문서가 보임

## 메모

- dev EC2 는 메모리 **4GB+ (t3.medium 이상)** 권장 — mysql+redis+app 동거.
- 시크릿은 이미지에 굽지 않는다(`.dockerignore` 가 `application-secret.yml` 제외).
  대신 `deploy/dev/application-secret.yml` 을 **마운트**해 주입한다.
- mysql 버전은 운영 RDS 와 **같은 8.4** 로 고정해 동작 차이를 줄인다.

#!/usr/bin/env bash
# 인증서 최초 발급. 서버에서 딱 한 번만 실행한다.
#
# nginx.conf가 인증서 파일을 참조하므로, 인증서가 없으면 nginx가 아예 뜨지 못한다.
# 그래서 nginx를 띄우기 전에 standalone 방식(certbot이 직접 80을 잡음)으로 받는다.
# 이후 갱신은 nginx를 띄운 채 webroot 방식으로 한다 → renew-cert.sh
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

if [ ! -f .env ]; then
  echo "오류: .env 가 없다. 배포를 한 번 돌리거나 .env.example 을 채워 만든다." >&2
  exit 1
fi
# shellcheck disable=SC1091
set -a; source .env; set +a

: "${SITE_DOMAIN:?SITE_DOMAIN 이 .env 에 없다}"
: "${ACME_EMAIL:?ACME_EMAIL 이 .env 에 없다}"

if [ -f "certs/live/bookpool/fullchain.pem" ]; then
  echo "이미 발급돼 있다: certs/live/bookpool/"
  echo "재발급이 필요하면 renew-cert.sh 를 쓰거나 certs/ 를 지우고 다시 실행한다."
  exit 0
fi

# standalone 은 80 포트를 직접 잡는다. nginx가 떠 있으면 실패한다.
if docker ps --format '{{.Names}}' | grep -q '^bookpool-nginx$'; then
  echo "nginx 가 떠 있어 80 포트를 쓸 수 없다. 먼저 내린다:"
  echo "  docker compose stop nginx"
  exit 1
fi

mkdir -p certs certbot-www

echo "인증서 발급 중: $SITE_DOMAIN"
docker run --rm \
  -p 80:80 \
  -v "$DIR/certs:/etc/letsencrypt" \
  certbot/certbot certonly --standalone \
  --cert-name bookpool \
  -d "$SITE_DOMAIN" \
  --email "$ACME_EMAIL" \
  --agree-tos --no-eff-email --non-interactive

echo
echo "발급 완료. 이제 전체를 띄운다:"
echo "  docker compose up -d"

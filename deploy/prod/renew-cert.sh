#!/usr/bin/env bash
# 인증서 갱신. cron으로 하루 두 번 돌린다.
#   0 3,15 * * * /bookpool/renew-cert.sh >> /bookpool/certbot.log 2>&1
#
# Let's Encrypt 인증서는 90일짜리다. certbot renew 는 만료 30일 전부터만 실제로
# 갱신하고 그 외에는 아무것도 하지 않으므로, 자주 돌려도 부담이 없다.
# (권장 주기가 하루 2회인 이유 — 긴급 폐기 시 빠르게 재발급받기 위해서다)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# nginx가 떠 있는 상태에서 갱신하므로 webroot 방식을 쓴다.
# 최초 발급은 standalone 이었으므로 -w 를 명시해 인증 방식을 덮어쓴다.
docker run --rm \
  -v "$DIR/certs:/etc/letsencrypt" \
  -v "$DIR/certbot-www:/var/www/certbot" \
  certbot/certbot renew \
  --webroot -w /var/www/certbot \
  --quiet

# 실제로 갱신됐을 때만 reload 하면 좋지만, reload 는 무중단이고 비용이 거의 없다.
# 매번 돌려서 "갱신됐는데 reload를 안 해 옛 인증서를 계속 쓰는" 사고를 없앤다.
if docker ps --format '{{.Names}}' | grep -q '^bookpool-nginx$'; then
  docker exec bookpool-nginx nginx -s reload
  echo "[$(date -Is)] renew 확인 + nginx reload 완료"
else
  echo "[$(date -Is)] renew 확인 완료 (nginx 미기동)"
fi

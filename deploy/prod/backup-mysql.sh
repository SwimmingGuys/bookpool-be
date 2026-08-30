#!/usr/bin/env bash
# MySQL 논리 백업. cron으로 매일 돌린다.
#   0 4 * * * /bookpool/backup-mysql.sh >> /bookpool/backups/backup.log 2>&1
#
# 볼륨만 믿으면 서버가 죽을 때 데이터도 같이 죽는다.
# 여기서 만든 파일을 주기적으로 서버 밖(S3 등)으로 옮기는 것까지가 백업이다.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

# shellcheck disable=SC1091
set -a; source .env; set +a

BACKUP_DIR="$DIR/backups"
KEEP_DAYS=14
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$BACKUP_DIR/bookpool-$STAMP.sql.gz"

mkdir -p "$BACKUP_DIR"

docker exec bookpool-mysql \
  mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction --quick --routines --events \
    "${MYSQL_DATABASE:-bookpool}" \
  | gzip > "$OUT"

# 0바이트면 실패로 본다 (mysqldump가 죽어도 gzip은 성공하므로)
if [ ! -s "$OUT" ]; then
  echo "[$(date -Is)] 백업 실패: $OUT 이 비어 있음" >&2
  rm -f "$OUT"
  exit 1
fi

find "$BACKUP_DIR" -name 'bookpool-*.sql.gz' -mtime "+$KEEP_DAYS" -delete
echo "[$(date -Is)] 백업 완료: $OUT ($(du -h "$OUT" | cut -f1))"

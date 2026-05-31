#!/usr/bin/env sh
set -eu

BACKUP_DIR="${BACKUP_DIR:-./backups/mysql}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-warehouse-mysql}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
FILE="${BACKUP_DIR}/warehouse_management_${TIMESTAMP}.sql"

mkdir -p "$BACKUP_DIR"

docker exec "$MYSQL_CONTAINER" sh -c 'mysqldump -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' > "$FILE"
gzip "$FILE"

echo "${FILE}.gz"

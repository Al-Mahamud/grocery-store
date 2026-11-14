#!/usr/bin/env bash
# Restores a backup made by backup.sh into the compose deployment, replacing what is there now
# (docs/DEPLOY.md). The shop is down while it runs.
#
#   scripts/restore.sh backups/db-<time>.dump backups/storage-<time>.tar.gz
set -euo pipefail

cd "$(dirname "$0")/.."
DUMP="${1:?usage: restore.sh <db dump> <storage archive>}"
FILES="${2:?usage: restore.sh <db dump> <storage archive>}"
COMPOSE=(docker compose -f deploy/compose.yaml --env-file deploy/.env)

if [[ "${CONFIRM:-}" != "yes" ]]; then
  read -r -p "This replaces the live database and pictures with the backup. Type 'yes' to go on: " answer
  [[ "$answer" == "yes" ]] || { echo "Nothing changed."; exit 1; }
fi

# Nothing may write while the data is replaced.
"${COMPOSE[@]}" stop caddy frontend backend
"${COMPOSE[@]}" up -d --wait db

# A fresh, empty database, then the dump into it.
"${COMPOSE[@]}" exec -T db dropdb -U haat --if-exists --force haat
"${COMPOSE[@]}" exec -T db createdb -U haat haat
"${COMPOSE[@]}" exec -T db pg_restore -U haat -d haat --no-owner --exit-on-error < "$DUMP"

# The pictures, through a throwaway container on the backend's storage volume.
"${COMPOSE[@]}" run --rm --no-deps -T --entrypoint bash backend -c \
  'find /var/lib/haat/storage -mindepth 1 -delete && tar xzf - -C /var/lib/haat/storage' < "$FILES"

"${COMPOSE[@]}" up -d --wait
echo "Restored from $DUMP and $FILES"

#!/usr/bin/env bash
# Create super admin: mark user 'admin' as super admin.
# 1) Ensure user exists: register via UI with username=admin, or INSERT into users (see README).
# 2) Run this script to set is_super_admin=1 for that user.

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="$SCRIPT_DIR/create-super-admin.sql"

if [ ! -f "$SQL_FILE" ]; then
  echo "Missing $SQL_FILE"
  exit 1
fi

# Optional: pass DB credentials via env (e.g. DATABASE_URL style or MYSQL_*)
# Example: MYSQL_USER=redlining MYSQL_PASSWORD=redlining MYSQL_DB=redlining ./create-super-admin.sh
MYSQL_USER="${MYSQL_USER:-redlining}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-redlining}"
MYSQL_DB="${MYSQL_DB:-redlining}"

if command -v mysql &>/dev/null; then
  mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" < "$SQL_FILE"
  echo "Done. User 'admin' is now super admin (if it existed)."
else
  echo "mysql client not found. Run the SQL manually:"
  cat "$SQL_FILE"
fi

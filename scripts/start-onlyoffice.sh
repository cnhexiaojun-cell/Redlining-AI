#!/usr/bin/env bash
# 启动 OnlyOffice Document Server（需本机已安装 Docker）
# 使用：./scripts/start-onlyoffice.sh  或在项目根目录：docker compose -f docker-compose.onlyoffice.yml up -d

set -e
cd "$(dirname "$0")/.."
echo "Starting OnlyOffice Document Server..."
docker compose -f docker-compose.onlyoffice.yml up -d
echo "OnlyOffice 已在 http://127.0.0.1:8080 提供文档服务。"
echo "请确保后端 application.yml 中："
echo "  app.onlyoffice.document-server-url: http://127.0.0.1:8080"
echo "  app.onlyoffice.api-base-url: http://localhost:8003  （OnlyOffice 在 Docker 时改为 http://host.docker.internal:8003）"

#!/usr/bin/env bash
# 加载 .env 中的 DEEPSEEK_API_KEY 等变量后启动 Spring Boot（未配置密钥时会显示示例结果）
set -e
cd "$(dirname "$0")"
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi
exec mvn spring-boot:run "$@"

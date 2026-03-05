#!/usr/bin/env bash
# 在 OnlyOffice 容器启动后执行：允许从 host.docker.internal 拉取文档，并重启 docservice 使配置生效。
# 用法: ./scripts/onlyoffice-enable-preview.sh
# 建议: docker compose -f docker-compose.onlyoffice.yml up -d 后等待约 90 秒再运行本脚本。

set -e
CONTAINER="${ONLYOFFICE_CONTAINER:-redlining-onlyoffice}"

echo "Patching local.json (allowPrivateIPAddress) in $CONTAINER..."
docker exec "$CONTAINER" python3 -c "
import json
path = '/etc/onlyoffice/documentserver/local.json'
with open(path) as f:
    d = json.load(f)
d.setdefault('services', {})['CoAuthoring'] = d.get('services', {}).get('CoAuthoring', {})
d['services']['CoAuthoring']['request-filtering-agent'] = {'allowPrivateIPAddress': True, 'allowMetaIPAddress': True}
with open(path, 'w') as f:
    json.dump(d, f, indent=2)
print('Patched.')
"

echo "Restarting ds:docservice and ds:converter..."
docker exec "$CONTAINER" supervisorctl restart ds:docservice ds:converter

echo "Done. Try uploading and previewing again."

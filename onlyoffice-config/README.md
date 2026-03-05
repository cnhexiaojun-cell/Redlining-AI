# OnlyOffice config

`local.json` is gitignored (contains secrets).

**If the file is missing** (e.g. fresh clone): start OnlyOffice without the mount, then export from the running container:

```bash
docker compose -f docker-compose.onlyoffice.yml up -d
# Wait ~2 min, then patch and export:
docker exec redlining-onlyoffice python3 -c "
import json
path = '/etc/onlyoffice/documentserver/local.json'
with open(path) as f:
    d = json.load(f)
d.setdefault('services', {})['CoAuthoring'] = d.get('services', {}).get('CoAuthoring', {})
d['services']['CoAuthoring']['request-filtering-agent'] = {'allowPrivateIPAddress': True, 'allowMetaIPAddress': True}
with open(path, 'w') as f:
    json.dump(d, f, indent=2)
"
docker exec redlining-onlyoffice cat /etc/onlyoffice/documentserver/local.json > local.json
# Then uncomment the volume mount in docker-compose.onlyoffice.yml and run: docker compose -f docker-compose.onlyoffice.yml down && up -d
```

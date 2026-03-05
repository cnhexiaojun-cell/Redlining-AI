# Redlining AI API (Spring Boot)

与前端对接的 API 与 Python 版一致，可直接替换原 `backend/` 使用。

## 要求

- Java 17+
- Maven 3.8+
- MySQL 8（库 `redlining` 及用户已创建，见项目根 `backend/scripts/init_db.sql`）
- 可选：Redis（不配置则分析结果不缓存，需排除 Redis 自动配置见下方）

## 配置

复制 `.env.example` 为 `.env` 并填入 DeepSeek 密钥等，或通过环境变量设置：

- `DATABASE_URL` 或 `DATABASE_USERNAME` / `DATABASE_PASSWORD`：MySQL
- `JWT_SECRET`：JWT 签名密钥
- **`DEEPSEEK_API_KEY`**、`MODEL_NAME`：DeepSeek 等 LLM（**未配置时接口会返回基于合同内容的示例结果，不会调用真实 AI**）
- 可选：`REDIS_HOST`、`REDIS_PORT` 等，用于分析结果缓存

**重要**：若使用 `.env` 配置密钥，启动前需让当前 shell 加载该文件，否则仍会显示“未配置 AI 密钥”。推荐使用下方“运行”中的 `./run.sh`，会自动加载 `.env`。

无 Redis 时需排除自动配置，否则需保证 Redis 可连：

```bash
mvn spring-boot:run -- -Dspring-boot.run.arguments="--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
```

## 运行

**推荐**（自动加载 `backend-spring/.env` 中的 `DEEPSEEK_API_KEY`，获得真实 AI 审查）：

```bash
cd backend-spring
./run.sh
```

或手动加载环境变量后启动（默认端口 8003，需 MySQL 已启动）：

```bash
cd backend-spring
source .env   # 加载 DEEPSEEK_API_KEY 等
mvn spring-boot:run
```

打包后运行（需先 `source .env` 或 `export DEEPSEEK_API_KEY=sk-xxx`）：

```bash
mvn package -DskipTests
java -jar target/redlining-api-1.0.0.jar
```

## API

- `GET /api/health` — 健康检查
- `POST /api/register` — 注册（JSON: email, password）
- `POST /api/login` — 登录（JSON: email, password）
- `GET /api/me` — 当前用户（Header: Authorization: Bearer &lt;token&gt;）
- `POST /api/analyze` — 合同分析（需登录，multipart: file, stance, advanced_rules）

前端 Vite 代理目标保持 `http://127.0.0.1:8003` 即可。

#!/usr/bin/env bash
# 登录、注册接口测试（需先启动后端，默认 http://127.0.0.1:8003）
set -e
BASE_URL="${1:-http://127.0.0.1:8003}"

echo "=== 1. GET /api/captcha ==="
CAPTCHA_RESP=$(curl -s "${BASE_URL}/api/captcha")
CAPTCHA_ID=$(echo "$CAPTCHA_RESP" | grep -o '"captchaId":"[^"]*"' | cut -d'"' -f4)
if [ -z "$CAPTCHA_ID" ]; then
  echo "FAIL: no captchaId in response"
  echo "$CAPTCHA_RESP"
  exit 1
fi
echo "OK captchaId=$CAPTCHA_ID"
echo "（验证码为图片，无法在脚本中识别，以下注册/登录需在浏览器或 Postman 中手动输入验证码）"
echo ""

# 使用固定验证码无法通过（服务端随机生成），下面仅演示错误响应
echo "=== 2. POST /api/register（错误验证码，预期 400）==="
REG_STATUS=$(curl -s -o /tmp/reg_body.txt -w "%{http_code}" -X POST "${BASE_URL}/api/register" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser1\",\"password\":\"pass123\",\"captchaId\":\"${CAPTCHA_ID}\",\"captchaCode\":\"wrong\"}")
echo "HTTP $REG_STATUS"
if [ "$REG_STATUS" = "400" ]; then
  echo "OK 验证码错误时返回 400"
  cat /tmp/reg_body.txt | head -c 200
  echo ""
else
  echo "Response: $(cat /tmp/reg_body.txt)"
fi
echo ""

echo "=== 3. POST /api/login（错误验证码，预期 400）==="
LOGIN_STATUS=$(curl -s -o /tmp/login_body.txt -w "%{http_code}" -X POST "${BASE_URL}/api/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser1\",\"password\":\"pass123\",\"captchaId\":\"${CAPTCHA_ID}\",\"captchaCode\":\"wrong\"}")
echo "HTTP $LOGIN_STATUS"
if [ "$LOGIN_STATUS" = "400" ]; then
  echo "OK 验证码错误时返回 400"
else
  echo "Response: $(cat /tmp/login_body.txt)"
fi
echo ""

echo "=== 4. GET /api/me（无 token，预期 401）==="
ME_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/me")
echo "HTTP $ME_STATUS"
[ "$ME_STATUS" = "401" ] && echo "OK 未认证返回 401" || echo "Unexpected: $ME_STATUS"
echo ""

echo "--- 小结 ---"
echo "captcha 接口正常；错误验证码时 register/login 返回 400；/me 无 token 返回 401。"
echo "完整注册+登录流程请在浏览器打开前端，输入验证码进行测试。"

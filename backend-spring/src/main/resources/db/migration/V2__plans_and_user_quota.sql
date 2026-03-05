-- 套餐与用户配额：plans 表、users 新增 plan_id/quota_remaining/period_ends_at、orders 表
-- 若使用 Hibernate ddl-auto: update，可无需手动执行；本脚本供非 Hibernate 或生产迁移参考。

-- plans
CREATE TABLE IF NOT EXISTS plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    type VARCHAR(16) NOT NULL DEFAULT 'quota',
    quota INT NOT NULL DEFAULT 0,
    period VARCHAR(16) NULL,
    price_cents INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    description TEXT NULL,
    scope VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_plans_code ON plans(code);
CREATE INDEX IF NOT EXISTS idx_plans_is_default ON plans(is_default);
CREATE INDEX IF NOT EXISTS idx_plans_type ON plans(type);

-- users 新增列（若已存在则跳过）
ALTER TABLE users ADD COLUMN IF NOT EXISTS plan_id BIGINT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS quota_remaining INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS period_ends_at TIMESTAMP NULL;

-- orders
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    amount_cents INT NOT NULL,
    payment_method VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    external_order_id VARCHAR(128) NULL,
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_renewal BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_plan_id ON orders(plan_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

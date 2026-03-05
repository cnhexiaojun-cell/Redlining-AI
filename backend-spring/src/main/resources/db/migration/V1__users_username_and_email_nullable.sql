-- 用户表字段更新：增加用户名，邮箱改为选填（登录仅用用户名）
-- 执行前请备份数据库。执行方式示例：
--   mysql -u redlining -p redlining < src/main/resources/db/migration/V1__users_username_and_email_nullable.sql

-- 1. 增加 username 列（若已存在则跳过本句）
ALTER TABLE users ADD COLUMN username VARCHAR(64) NULL COMMENT '用户名，登录用';

-- 2. 为已有用户回填用户名，便于继续用用户名登录（无 username 的行会得到 user_<id>）
UPDATE users SET username = CONCAT('user_', id) WHERE username IS NULL OR username = '';

-- 3. 为 username 添加唯一索引（执行前确保无重复，上一步已保证每行有值）
ALTER TABLE users ADD UNIQUE INDEX idx_users_username (username);

-- 4. 邮箱改为可空
ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL COMMENT '邮箱，选填';

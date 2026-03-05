-- 用户表扩展：头像、姓名、职业（头像存 MinIO 对象键）
-- 执行：mysql -u redlining -p redlining < scripts/alter-users-profile.sql

ALTER TABLE users ADD COLUMN avatar_url VARCHAR(255) NULL COMMENT 'MinIO object key for avatar';
ALTER TABLE users ADD COLUMN real_name VARCHAR(64) NULL COMMENT 'Display name';
ALTER TABLE users ADD COLUMN occupation VARCHAR(128) NULL COMMENT 'Occupation';

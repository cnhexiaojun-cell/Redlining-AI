-- 仅将 users.email 改为可空，解决注册时 "Column 'email' cannot be null" 报错
-- 执行：mysql -u redlining -p redlining < scripts/fix-email-nullable.sql

ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL COMMENT '邮箱，选填';

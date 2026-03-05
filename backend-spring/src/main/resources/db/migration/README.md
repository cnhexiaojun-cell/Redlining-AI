# 数据库迁移说明

## 方式一：执行 SQL 脚本（推荐先备份）

在项目根目录或 `backend-spring` 下执行（按实际库名、用户、密码修改）：

```bash
mysql -u redlining -p redlining < src/main/resources/db/migration/V1__users_username_and_email_nullable.sql
```

若表里已有 `username` 列（例如由 Hibernate `ddl-auto: update` 自动加过），请只执行脚本中的第 2 步（UPDATE）和第 4 步（email 可空），或先注释掉会报错的语句再执行。

## 方式二：由应用自动更新

当前 `application.yml` 中为 `spring.jpa.hibernate.ddl-auto: update`，启动应用时 Hibernate 会自动尝试：

- 增加 `username` 列（若不存在）
- 将 `email` 改为可空

**注意**：自动更新不会为已有用户回填 `username`，老用户需手动执行迁移脚本中的 UPDATE，或自行在库里为每条记录设置 `username`，否则无法用“用户名”登录。

## 本次变更摘要

| 变更       | 说明 |
|------------|------|
| 新增字段   | `users.username`，VARCHAR(64)，唯一，可空（建议对老数据回填后加唯一约束） |
| 修改字段   | `users.email` 改为可空，仅作选填资料，不参与登录 |

# 02 - Tomcat 部署指南（外置 Tomcat 9 + JDK 8）

本文档说明如何将本系统以 **WAR 包** 形式部署到 **外置 Apache Tomcat 9**，数据库使用 **PostgreSQL 12+**。同时给出 Nginx 托管 Vue3 前端的生产拓扑、JNDI 数据源可选方案与运维要点。

---

## 1. 环境要求

| 组件 | 版本要求 | 说明 |
|---|---|---|
| JDK | 8（推荐 8u202+）| 后端编译目标 1.8；JDK 11 亦可运行（已带 jaxb-api 依赖） |
| Apache Tomcat | 9.0.x（Servlet 4.0 / javax.*） | **不可用 Tomcat 10+**（jakarta.* 命名空间不兼容 Spring Boot 2.7） |
| PostgreSQL | 12+（推荐 14/15） | 用到部分唯一索引、`UPDATE ... RETURNING`、`generate_series` |
| Maven | 3.6+ | 构建 WAR |
| Node.js | 18+（推荐 20 LTS） | 构建 Vue3 前端（Vite 5） |
| Nginx | 1.20+（可选，生产推荐） | 托管前端静态资源 + 反向代理 |

## 2. 初始化数据库

```bash
# 1) 创建数据库与专用账号（psql 超级用户执行）
CREATE USER campus_express WITH PASSWORD '请改为强密码';
CREATE DATABASE campus_express OWNER campus_express ENCODING 'UTF8';

# 2) 依次执行脚本（顺序不可颠倒）
psql -U campus_express -d campus_express -f sql/01_schema.sql
psql -U campus_express -d campus_express -f sql/02_seed.sql
```

`01_schema.sql` 创建 10 张业务表及索引（含防重复授权的部分唯一索引 `uk_proxy_active`）；`02_seed.sql` 写入角色/权限点、演示账号、驿站货架与演示包裹。

**种子账号（初始密码均为 `123456`，上线前必须全部改密）：**

| 用户名 | 角色 | 说明 |
|---|---|---|
| admin | ADMIN | 系统管理员 |
| staff01 | STAFF | 驿站员工 |
| 20230101 | STUDENT | 张小明（演示物主，手机 13900000101） |
| 20230102 | STUDENT | 李小红（演示代取人） |
| 20230103 | STUDENT | 王小刚 |

## 3. 构建后端 WAR

```bash
cd backend
mvn clean package -DskipTests
# 产物：backend/target/campus-express.war
```

关键打包配置（已在 pom.xml 中就绪）：
- `<packaging>war</packaging>`；
- `spring-boot-starter-tomcat` 置为 `provided`，避免与外置 Tomcat 的 Servlet 容器冲突；
- `ServletInitializer extends SpringBootServletInitializer` 作为外置容器启动入口；
- `finalName=campus-express`，保证 context path 为 `/campus-express`（与前端 axios baseURL 一致）。

> 注意：**WAR 文件名即 context path**。若改名为 `ROOT.war` 部署到根路径，须同步修改前端 `src/api/http.js` 的 `baseURL` 与 `vite.config.js` 代理并重新构建前端。

## 4. 配置外置 Tomcat

### 4.1 环境变量（推荐方式）

编辑 `$CATALINA_HOME/bin/setenv.sh`（Windows 为 `setenv.bat`），注入应用配置：

```bash
# setenv.sh —— Linux
export DB_URL="jdbc:postgresql://127.0.0.1:5432/campus_express"
export DB_USER="campus_express"
export DB_PASSWORD="强密码"
export JWT_SECRET="至少32字节的随机字符串-生产必须更换"
export TASK_ENABLED=true
export CORS_ORIGINS="https://express.example.edu.cn"
export JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
```

```bat
:: setenv.bat —— Windows
set "DB_URL=jdbc:postgresql://127.0.0.1:5432/campus_express"
set "DB_USER=campus_express"
set "DB_PASSWORD=强密码"
set "JWT_SECRET=至少32字节的随机字符串-生产必须更换"
set "TASK_ENABLED=true"
set "CORS_ORIGINS=https://express.example.edu.cn"
set "JAVA_OPTS=%JAVA_OPTS% -Xms512m -Xmx1024m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
```

支持的全部环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | jdbc:postgresql://127.0.0.1:5432/campus_express | JDBC 连接串 |
| `DB_USER` / `DB_PASSWORD` | postgres / postgres | 数据库账号 |
| `DB_POOL_MAX` | 20 | Hikari 最大连接数 |
| `JWT_SECRET` | 开发默认值（**必须覆盖**） | HS256 签名密钥，≥32 字节 |
| `TASK_ENABLED` | true | 定时任务开关；**多实例部署仅一台开启** |
| `CORS_ORIGINS` | http://localhost:5173,... | 允许跨域来源（逗号分隔）；同源 Nginx 部署可不依赖 |
| `SQL_LOG_LEVEL` | INFO | 设为 DEBUG 可打印 MyBatis SQL |

### 4.2 可选：Tomcat JNDI 数据源

若单位规范要求由 Tomcat 管理连接池，可改用 JNDI：

1. 将 PostgreSQL 驱动 jar（如 `postgresql-42.7.x.jar`）放入 `$CATALINA_HOME/lib`；
2. 在 `conf/context.xml`（或应用的 `META-INF/context.xml`）中添加：

```xml
<Resource name="jdbc/campusExpress" auth="Container"
          type="javax.sql.DataSource" driverClassName="org.postgresql.Driver"
          url="jdbc:postgresql://127.0.0.1:5432/campus_express"
          username="campus_express" password="强密码"
          maxTotal="20" maxIdle="5" maxWaitMillis="10000"/>
```

3. 在 `application.yml` 中将 `spring.datasource` 段替换为：

```yaml
spring:
  datasource:
    jndi-name: java:comp/env/jdbc/campusExpress
```

重新打包部署即可（其余配置不变）。默认方案（Hikari + 环境变量）开箱即用，二选一。

### 4.3 部署与启动

```bash
cp backend/target/campus-express.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh        # Windows: startup.bat

# 验证（无需登录的健康检查接口）
curl http://127.0.0.1:8080/campus-express/api/health
# 期望返回 {"code":0,"msg":"OK","data":{"status":"UP",...}}
```

启动失败排查顺序：`logs/catalina.out` → 数据库连通性（`psql` 同参数验证）→ 环境变量是否生效（`setenv` 文件名/位置）→ 是否误用 Tomcat 10+。

## 5. 构建与发布前端

### 5.1 开发模式（联调）

```bash
cd frontend
npm install
npm run dev            # http://localhost:5173
```

Vite 已配置代理：`/campus-express` → `http://127.0.0.1:8080`，前后端同域开发无需 CORS。

### 5.2 生产构建

```bash
cd frontend
npm run build          # 产物：frontend/dist/
```

### 5.3 Nginx 生产拓扑（推荐）

```
浏览器 ──> Nginx :443
            ├── /                    → frontend/dist 静态资源（SPA fallback）
            └── /campus-express/     → 反向代理 Tomcat 127.0.0.1:8080
```

参考配置：

```nginx
server {
    listen 443 ssl;
    server_name express.example.edu.cn;
    # ssl_certificate / ssl_certificate_key ...

    root /var/www/campus-express/dist;
    index index.html;

    # Vue Router history 模式回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端反向代理（同源部署，天然无跨域）
    location /campus-express/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    gzip on;
    gzip_types text/css application/javascript application/json;
}
```

> `X-Forwarded-For` 已被操作日志切面用于记录真实客户端 IP。

### 5.4 备选：前端并入 WAR（单机免 Nginx）

将 `frontend/dist/*` 复制到 `backend/src/main/webapp/` 后再打包，即可由 Tomcat 同时托管前后端（同 context 下访问 `http://host:8080/campus-express/`）。此方式需处理 SPA history 路由回退（可加一个将非 `/api` 请求 forward 到 `index.html` 的控制器或改用 hash 路由），**默认推荐 5.3 的 Nginx 方案**，构建脚本已按该方案组织。

## 6. 定时任务与多实例

- 催领扫描每 30 分钟、授权过期清理每 10 分钟，由 `campus.task.enabled` 总开关控制；
- **多实例负载均衡时**：仅一个节点设置 `TASK_ENABLED=true`，其余为 `false`，避免重复发通知；
- 状态迁移 SQL 均带前置状态条件（并发安全），即使误开多实例也不会重复核销/重复升级，但会重复发送提醒；
- 提醒阈值（24h 催领 / 72h 滞留 / 每日 3 次 / 授权 72h 上限）存于 `sys_config` 表，管理员在「提醒策略」页面热修改，无需重启。

## 7. 上线安全检查清单

- [ ] 所有种子账号密码已修改（尤其 admin）
- [ ] `JWT_SECRET` 已替换为随机强密钥并妥善保管
- [ ] 数据库使用专用低权限账号（非 postgres 超级用户）
- [ ] PostgreSQL 仅监听内网地址，`pg_hba.conf` 限制来源
- [ ] Tomcat 关闭 AJP Connector（或绑定内网并设 secret）、删除默认 webapps（docs/examples/manager 按需）
- [ ] Nginx 启用 HTTPS，Tomcat 8080 不对外网直接开放
- [ ] `CORS_ORIGINS` 收敛为真实前端域名
- [ ] 日志与数据库纳入定期备份（`pg_dump` 每日 + WAL 归档按需）

## 8. 升级与回滚

```bash
# 升级：保留旧包 → 停服 → 换包 → 起服 → 健康检查
mv $CATALINA_HOME/webapps/campus-express.war /backup/campus-express-$(date +%F).war
$CATALINA_HOME/bin/shutdown.sh
cp new-build/campus-express.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
curl -f http://127.0.0.1:8080/campus-express/api/health || echo "启动失败，回滚旧包"
```

数据库结构变更须先执行增量 SQL 并保证向后兼容，再发布新 WAR；回滚时旧 WAR 可直接换回（表结构兼容前提下）。
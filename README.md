# 校园快递驿站取件提醒与代取授权管理系统

企业级全栈项目：**Spring Boot（WAR，外置 Tomcat 9）+ Spring MVC + MyBatis + PostgreSQL + Vue3 + Element Plus**。
围绕校园快递驿站两大核心业务——**取件提醒**（入库通知 / 24h 催领 / 72h 滞留升级）与**代取授权**（授权码 + 取件码双因子核销、有效期控制、可撤销、防重复授权）——提供学生端、驿站端、管理员端三套工作台，并扩展了自寄、超时保险、钱包返利、红包卡券、客服工单、AI 助手等校园驿站周边能力。

## 一、功能总览

| 端 | 功能 |
|---|---|
| 学生端 | 我的包裹（状态筛选/取件码/条码全屏出示/催领）、发起代取授权、我的授权（撤销）、我代取的、代取广场接单、通知与好友（合并页：站内通知 + 好友申请/好友列表）、邀请有礼（邀请返利 10%）、钱包与流水（虚拟余额/充值演示）、红包卡券与兑换码、地址簿（浏览器 Geolocation 定位）、收货偏好、常用电话簿、自寄下单、超时保险购买、运费预算、实名认证、联系客服工单、意见反馈、关于我们 |
| 驿站端 | 包裹入库（驿站→货架容量联动）、包裹管理（补发提醒/退回）、取件核销（本人=包裹+取件码；代取=授权码+取件码；支持摄像头扫码 + 手动输入兜底）、批量导入包裹、自寄单管理（揽收/取消）、客服工单台（待处理/处理中/已处理）、滞留件处理、货架占用看板、包裹轨迹、身份核验 |
| 管理员端 | 数据看板（指标卡 + 入/出库趋势 + 状态/快递公司分布 ECharts）、用户管理（RBAC/重置密码/停启用）、驿站与货架管理、提醒策略热配置、公告发布（登录弹窗联动）、代取订单审计、钱包管理、广告位管理、红包卡券工厂（批量生成卡券/兑换码）、意见反馈处理（回复）、系统设置（AI 接入 + 关于我们）、操作日志审计 |
| 系统级 | JWT 认证 + 注解式权限点（ADMIN 23 项 / STAFF 9 项）、包裹/授权双状态机、定时催领与过期清理、操作日志切面（敏感字段脱敏）、统一响应与全局异常、并发安全核销、AI 助手（OpenAI 兼容代理 + 提示词注入防护，仅快递域）、登录公告弹窗（勾选「下次不再弹出」才按公告 id 本机记录）、个人中心一键清缓存 |

## 二、技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot（WAR 外置容器部署） | 2.7.18 |
| Web 层 | Spring MVC + 拦截器 JWT + AOP 权限/日志 | — |
| ORM | MyBatis（XML Mapper，驼峰映射） | mybatis-spring-boot-starter 2.3.2 |
| 数据库 | PostgreSQL（部分唯一索引 / UPDATE...RETURNING / generate_series），业务表位于 `campus_express` schema，共 37 张 | 12+（本机 18） |
| 认证 | jjwt 0.9.1（HS256，2h）+ BCrypt（spring-security-crypto） | — |
| AI | OpenAI 兼容 Chat Completions 代理（RestTemplate），地址/模型/Key 由管理员在系统设置页配置 | — |
| 前端 | Vue 3.5 + Vite 5.4 + Element Plus 2.8 + Pinia + Vue Router 4 + Axios + ECharts 5.5 | — |
| 部署 | 外置 Tomcat 9（javax.*，禁用 Tomcat 10+）+ Nginx（SPA + 反代） | — |
| 运行时 | 后端源码 Java 8（兼容 11+，本机以 JDK 21 运行 Tomcat 9）；前端构建 Node 18+ | — |

## 三、目录结构

```
campus-express-station/
├── README.md
├── docs/
│   ├── 01-总体设计文档.md        # 架构/模块/ER/接口/状态机/安全设计
│   └── 02-Tomcat部署指南.md      # 建库/打包/setenv/JNDI/Nginx/安全清单/回滚
├── sql/
│   ├── 01_schema.sql             # 核心 10 表 + 索引（含 uk_proxy_active 防重复授权）
│   ├── 02_seed.sql               # 角色权限点、演示账号、驿站货架、演示包裹
│   ├── 03_seed_realistic.sql     # 真实感批量演示数据（幂等重建业务数据）
│   ├── 04_features.sql           # 扩展业务表：钱包/卡券/兑换码/自寄/保险/工单/广告/反馈等
│   └── 05_community.sql          # 好友/邀请/地址簿/偏好/电话簿/公告已读等（合计 37 表）
├── scripts/
│   ├── build.bat                 # Windows 一键构建
│   └── build.sh                  # Linux/macOS 一键构建
├── backend/                      # Spring Boot WAR（32 个 REST 控制器）
│   ├── pom.xml                   # packaging=war, tomcat provided, finalName=campus-express
│   └── src/main/
│       ├── java/com/campus/express/
│       │   ├── CampusExpressApplication.java / ServletInitializer.java
│       │   ├── common/           # R / PageResult / 异常 / @OpLog 切面 / @RequirePerm
│       │   ├── config/ security/ # WebMvc / JWT 拦截器 / UserContext / BCrypt
│       │   ├── entity/ dto/ vo/  # 实体 + 校验 DTO + 视图对象
│       │   ├── mapper/           # MyBatis 接口（XML 在 resources/mapper/）
│       │   ├── service/          # 业务层（状态机、并发核销、AiService 防注入等）
│       │   ├── notify/ task/     # 三渠道提醒（站内/SMS/EMAIL 模拟）+ 定时任务
│       │   └── controller/       # /api 前缀：核心业务 + AI/卡券/自寄/保险/工单/广告/反馈/邀请/好友/钱包/系统设置等
│       └── resources/
│           ├── application.yml   # 环境变量占位（DB_URL/JWT_SECRET/TASK_ENABLED...）
│           └── mapper/*.xml
└── frontend/                     # Vue3 + Element Plus（46 个页面视图）
    ├── vite.config.js            # 代理 /campus-express → 127.0.0.1:8080
    └── src/
        ├── api/ router/ stores/ utils/ layout/ components/
        ├── styles/               # art-flair.css / art-page.css（排版令牌/淡线表格/毛玻璃/百叶窗）
        └── views/                # login/register + student(17)/staff(10)/admin(13) + 通知/公告/个人
```

## 四、快速开始

### 1. 初始化数据库

通用做法（独立库）：

```bash
psql -U postgres -c "CREATE USER campus_express WITH PASSWORD '强密码';"
psql -U postgres -c "CREATE DATABASE campus_express OWNER campus_express ENCODING 'UTF8';"
psql -U campus_express -d campus_express -f sql/01_schema.sql
psql -U campus_express -d campus_express -f sql/02_seed.sql
psql -U campus_express -d campus_express -f sql/04_features.sql
psql -U campus_express -d campus_express -f sql/05_community.sql
# 可选：03_seed_realistic.sql 灌入真实感演示数据
```

本机实际环境（已就绪）：PostgreSQL 18（`D:\Program_Files\PostgreSQL\18`），库 `artex`、schema `campus_express`，连接用户 `artex`；SQL 脚本内已固定 schema，按序执行 01 → 02 →（可选 03）→ 04 → 05 即可。### 2. 构建并部署后端（外置 Tomcat 9）

```bash
cd backend && mvn clean package -DskipTests
# 配置 $CATALINA_HOME/bin/setenv（DB_URL/DB_USER/DB_PASSWORD/JWT_SECRET，样例见部署指南）
cp target/campus-express.war $CATALINA_HOME/webapps/
# Tomcat 已运行则热部署（约 30s 完成重部署）；未运行则 startup.sh / startup.bat
curl http://127.0.0.1:8080/campus-express/api/health   # {"code":0,...,"status":"UP"}
```

本机实际路径：Maven `D:\Program_Files\apache-maven-3.9.16`，Tomcat `D:\Program_Files\apache-tomcat-9.0.124`（webapps 热部署验证通过）。

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev        # 开发：http://localhost:5173（Vite 代理到 Tomcat）
npm run build      # 生产：dist/ 交给 Nginx，配置见部署指南 §5.3
```

也可直接运行 `scripts/build.bat`（Windows）或 `bash scripts/build.sh` 一键构建两端。

### 4. 演示账号（初始密码均为 `123456`，上线前必须改密）

| 用户名 | 角色 | 说明 |
|---|---|---|
| admin | ADMIN | 系统管理员（教职工通道登录），全部管理功能 |
| staff01 | STAFF | 驿站员工（教职工通道登录）：入库/核销/滞留/货架/导入/自寄/工单 |
| 20230101 | STUDENT | 陈嘉睿，手机 13900000101（演示包裹物主；02 号种子初始名为张小明，执行 03 号真实种子后更新） |
| 20230102 | STUDENT | 林晚晴（演示代取人） |
| 20230103 | STUDENT | 王小刚 |

登录页提供演示账号一键填充（含验证码自动填写）；admin / staff01 需走「教职工通道」。

**演示闭环**：
1. **取件/代取**：staff01 入库（手机号 13900000101）→ 20230101 收到站内通知、查看取件码（可全屏条码）→ 发起代取授权给 20230102 → 20230102 获得授权码 → staff01 在「取件核销-代取」扫码或输入授权码+取件码完成核销 → 双方收到通知，看板/日志同步更新。
2. **自寄**：学生「自寄下单」→ staff01「自寄单管理」揽收 → 学生钱包扣费（虚拟余额）、可用运费券抵扣。
3. **卡券/邀请**：admin「红包卡券工厂」批量生成卡券与兑换码 → 学生兑换入包 → 下单抵扣；学生「邀请有礼」邀请注册，被邀人首单后邀请人获 10% 返利入钱包。
4. **工单/反馈**：学生「联系客服」提工单 → staff01 工单台处理；学生「意见反馈」→ admin 回复。
5. **AI 助手**：admin「系统设置」配置 OpenAI 兼容接口后，任意用户可用顶栏 AI 入口咨询快递域问题；未配置时优雅降级提示。

## 五、核心机制速览

- **取件提醒**：入库即发站内通知；定时任务每 30min 扫描超过 `remind.first.delay.hours`(24h) 未取件包裹催领，每日上限 `remind.daily.max`(3)；超过 `remind.overdue.hours`(72h) 升级为滞留（OVERDUE）并通知驿站。SMS/EMAIL 渠道为可替换的模拟实现（`notify/` 下策略接口）。
- **代取授权**：授权码（8 位）+ 包裹取件码（6 位）双因子；时限 1~72h（上限 `auth.max.valid.hours`）；部分唯一索引保证同一包裹同一时刻仅一条 ACTIVE 授权；核销 SQL 带前置状态条件，并发重复提交只会成功一次；可撤销，定时任务每 10min 过期清理。
- **RBAC**：JWT（2h）→ 拦截器解析 → `@RequirePerm("parcel:pickup")` 切面校验权限点；ADMIN 23 项（含 ad:manage / ai:config / coupon:manage / feedback:reply），STAFF 9 项（含 ship:manage / ticket:handle / parcel:inbound）；前端 `v-perm` 指令同步隐藏无权按钮。
- **策略热配置**：阈值存 `sys_config`，管理员界面即时生效（范围校验 +「滞留阈值>催领阈值」），无需重启。
- **AI 助手与防注入**：管理员在「系统设置」配置 `ai.api.url / ai.api.model / ai.api.key / ai.enabled / ai.max.history(0~20) / ai.timeout.ms(3000~60000)`，均存 `sys_config`。后端 GET 回显对 Key 脱敏（首 4 + **** + 尾 4，短 Key 全掩码）；保存时 Key 留空或含 `****` 视为不修改。`AiService` 以系统提示词限定仅回答快递/驿站域问题，做用户输入注入检测与话题过滤；未启用或未配 Key 时 `/api/ai/status` 返回 enabled=false，前端降级为不可用提示，不影响其他功能。
- **钱包/卡券/邀请**：虚拟余额（无真实支付），流水可追溯；卡券分 COUPON/RED_PACKET，适用域 ALL/SHIP/PROXY/INSURANCE；兑换码 UNUSED/USED/DISABLED 状态机；邀请返利按被邀人首单金额 10% 入账。
- **登录公告弹窗**：登录后弹出最新公告，底部「下次不再弹出」复选框——仅勾选后关闭才按公告 id 写入本机记录，未勾选每次登录仍弹；个人中心「清理缓存」会一并清除这些记录（弹窗恢复）。
- **审计**：`@OpLog` 切面记录操作人/IP（X-Forwarded-For）/模块/参数（密码、取件码等脱敏），管理员日志页可检索。

## 六、视觉与交互规范

- 排版令牌集中于 `styles/art-flair.css`（字号阶梯/字距/行距）；页面统一 `PageHero + MetricTile + PanelCard` 结构（`art-page.css`）。
- 表格：淡色分割线、点击表头正/倒序排序（`utils/tableSort.js`），默认按时间倒序；移动端降级为卡片列表，分页简化。
- 金额统一带「元」（`utils/format.js money()`）；时间统一 `yyyy-MM-dd HH:mm`，空值显示「—」。
- 毛玻璃（顶栏/弹窗遮罩）与百叶窗过渡；亮/暗双主题（html.dark），暗色卡底 #17171c。
- 悬浮按钮（回到顶部 / AI 入口）统一尺寸圆角，右侧内缩并抬离底部边缘。

## 七、环境变量

| 变量 | 默认 | 说明 |
|---|---|---|
| DB_URL / DB_USER / DB_PASSWORD | 本机 campus_express / postgres / postgres（本机实际：artex 库 + artex 用户） | 数据库连接 |
| DB_POOL_MAX | 20 | Hikari 连接池上限 |
| JWT_SECRET | 开发默认（生产必改） | HS256 密钥，≥32 字节 |
| TASK_ENABLED | true | 定时任务开关，多实例仅一台开启 |
| CORS_ORIGINS | localhost:5173 等 | 跨域白名单（Nginx 同源可不依赖） |
| SQL_LOG_LEVEL | INFO | DEBUG 打印 SQL |

AI 接入不走环境变量，由管理员在「系统设置」页写入 `sys_config`（见 §五）。

## 八、设计参考

功能与架构设计参考了 CSDN/GitHub 上同类校园快递/驿站管理系统与商用驿站产品的公开实践：包裹状态机与取件码核销流程、货架容量管理、滞留件升级策略、代取双人核验、RBAC 权限点模型与操作审计等，均结合校园场景做了适配与增强（详见 `docs/01-总体设计文档.md` 的参考分析章节）。

## 九、构建状态与已知说明

- 后端已实际执行 `mvn package`（Maven 3.9.16 + 本机受管 JDK）构建成功，WAR 已部署至本机 Tomcat 9 并验证热部署与接口鉴权正常。
- 前端已实际执行 `npm install` + `npm run build`（Vite 5）构建成功，全部页面产出独立 chunk；管理员端新页面（系统设置/卡券工厂/广告位/意见反馈）经浏览器亮/暗双主题实测，控制台无错误。
- 待办：最近一次筛选栏宽度微调（`art-page.css` 工具栏字段基准宽度）在 build 之后修改，发布前需重跑 `npm run build`；375px 移动端与员工端三新页（导入/自寄/工单）尚未做浏览器实测；AI 注入防护为代码级实现，端到端话术测试需配置真实 Key 后进行。
- SMS/EMAIL 通知为模拟实现，接入真实网关时替换 `notify/` 下对应策略类即可。
- 钱包充值、返利均为站内虚拟余额演示，不涉及真实支付。
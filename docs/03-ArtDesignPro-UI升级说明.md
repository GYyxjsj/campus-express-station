# Art Design Pro 企业级 UI 升级说明

> 本轮升级在已验证可运行的系统（Spring Boot WAR + Tomcat + PostgreSQL + Vue3）之上进行，
> 后端接口与业务逻辑零改动，仅重构前端视觉层与移动端体验。

## 1. 设计来源与方式

- 参考项目：**Art Design Pro**（https://github.com/Daymychen/art-design-pro ，MIT 许可）。
- 采用方式：**提取其源码设计令牌**（配色、灰阶、圆角、阴影、登录页动效、侧栏风格），
  以 CSS 变量层落地到现有 Element Plus 体系，而非整体更换模板，
  从而保住既有 API 对接、RBAC 权限指令与全部业务页面逻辑。

## 2. 设计令牌层（frontend/src/styles/art-theme.css）

| 令牌 | 亮色 | 暗色 |
|---|---|---|
| 主色 primary | `#5D87FF` | 同主色，梯度重算 |
| success / warning / danger | `#13deb9` / `#ffae1f` / `#fa896b` | 同步覆盖 |
| 页面背景 | `#fafbfc` | `#070707` |
| 卡片/盒背景 | `#ffffff` | `#161618` |
| 灰阶 100→900 | `#f9fafb → #323251` | 反转 `#110f0f → #e3e3e8` |
| 边框 | `rgba(0,0,0,.08)` / `#e2e8ee` | `rgba(255,255,255,.08/.1)` |
| 圆角 | `--custom-radius: 0.75rem`，el 组件 `calc(/3+2px)`，卡片 `+4px` | 同 |
| 卡片阴影 | `0 1px 3px rgba(0,0,0,.03), 0 1px 2px -1px rgba(0,0,0,.08)` | 弱化 |

- Element Plus 全色梯度覆盖：primary/success/warning/danger/error/info 各 light-3~9 / dark-2。
- 通用类：`.art-card`（含 -sm/-xs/-hover）、`.art-page`、`.art-page-header`、`.art-stat-card`、
  `.art-breathe` 呼吸红点（`#ff3860`，1.5s）、`slide-left` 页面过渡。

## 3. 亮暗双主题（frontend/src/stores/theme.js）

- 三态：`light / dark / auto`（跟随系统 `matchMedia`），持久化 `localStorage.ce_theme`。
- 切换 `html.dark` + 300ms 过渡；auto 模式监听系统主题变化实时生效。
- ECharts 看板通过 `watch(theme.mode)` 重渲染：轴线/图例/标签色、饼图描边随主题适配。

## 4. 布局重构（MainLayout.vue）

- **桌面**：230px 浅色侧栏（渐变 Logo、圆角高亮菜单 + 左侧 4px 指示条）、
  60px 顶栏（面包屑「首页/分组/页面」、主题切换、铃铛未读角标、用户 chip 下拉）、
  `slide-left` 路由过渡。
- **手机（≤768px）**：侧栏变左滑抽屉 + 半透明遮罩；顶栏汉堡 + 当前页标题。
- 未读提醒 30s 轮询保留。

## 5. 登录页（Login.vue）

- 双栏：左品牌区（≤1024px 隐藏）渐变浅蓝底 + 3 张浮动业务卡片 + hero 文案；
  右栏 440×650 `slideInRight 0.6s` 登录卡，"欢迎回来 👋"。
- 演示账号 chip 一键填充（admin / staff01 / 20230101，密码 123456）。
- ≤640px 单栏自适应，`box-sizing: border-box` + `overflow-x: hidden` 保证 375px 无横向溢出。

## 6. 移动客户端（学生端）

- **底部 TabBar**（纯学生角色显示）：包裹 / 授权 / 提醒（红点）/ 我的。
- **卡片列表替代表格**（`useIsMobile` ≤768px 切换）：
  - 我的包裹：运单号+状态头部、驿站/货架副行、取件码高亮块（等宽 18px）、找人代取入口；
  - 我发起的授权：授权码高亮块、代取人/有效期、卡内 popconfirm 撤销；
  - 受托代取：同风格卡片。
- 分页移动端精简为 `prev, pager, next`；弹窗全宽；筛选条堆叠。
- 桌面表格补充 `show-overflow-tooltip` + 编号 `white-space: nowrap`，避免生硬折行。

## 7. 验证结论（浏览器实测，QA 截图见 ../qa/）

| 场景 | 视口 | 结果 |
|---|---|---|
| 登录双栏 | 1280×720 | ✅ 无破损 |
| admin 看板亮色 | 1280×720 | ✅ 0 console errors |
| admin 看板暗色（含双饼图） | 1280×720 | ✅ 无白底残留 |
| 学生登录 | 375×812 | ✅ scrollWidth=375 无溢出 |
| 我的包裹移动卡片 + TabBar | 375×812 | ✅ 高亮/红点正确 |
| 汉堡抽屉 + 遮罩 | 375×812 | ✅ 菜单完整、当前项高亮 |
| 发起代取授权（真实业务链路） | 375×812 | ✅ 生成授权码 XFQTDZ6E |
| 授权卡片列表 | 375×812 | ✅ 授权码高亮/撤销齐全 |
| 学生授权页桌面回归 | 1280×720 | ✅ 表格形态、TabBar 隐藏 |

- `npm run build` 通过（约 6s，无错误；Dashboard chunk ~1MB 为 ECharts 既有体积）。
- 运行方式不变：Tomcat `http://localhost:8080/campus-express`（API）+ 前端 dev `http://localhost:5173`；
  生产部署仍按 docs/02 的 Nginx 托管 `frontend/dist` 方案。
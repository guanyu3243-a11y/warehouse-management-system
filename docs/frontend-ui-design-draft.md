# Frontend UI Design Draft

本设计稿用于将服装仓库管理系统从课程项目视觉升级为小型企业可试用的 MVP 后台。

## Design Direction

- 类型：轻量 ERP / 仓库运营后台
- 气质：清楚、克制、商业化、适合长时间录单和查库存
- 场景：服装 SKU、分类、仓库、供应商、入库、出库、库存预警、操作日志
- 重点：统一导航、筛选、表格、表单、弹窗和状态反馈

## Layout

### App Shell

- 左侧固定导航，使用深色墨绿底，提升系统感。
- 顶部栏保持浅色，提供折叠菜单、当前页面标题和用户操作。
- 内容区使用浅灰绿色背景，页面内容以白色工作面板承载。

### Page Structure

每个业务页面统一为：

1. 页面标题区：标题、说明、主要操作按钮。
2. 筛选区：关键字、状态、仓库等筛选条件。
3. 表格区：业务数据、状态标签、行操作。
4. 分页区：右下角统一分页。

## Visual Tokens

| Token | Value | Usage |
| --- | --- | --- |
| Primary | `#0f766e` | 主按钮、当前菜单、关键链接 |
| Primary Strong | `#0b5f59` | Hover / active |
| Surface | `#ffffff` | 页面面板、弹窗、表单 |
| App Background | `#f5f7f6` | 主内容背景 |
| Sidebar | `#12201f` | 左侧导航 |
| Text | `#1f2937` | 正文 |
| Muted | `#64748b` | 说明、次要信息 |
| Border | `#dbe5e1` | 分割线、输入框 |
| Warning | `#b7791f` | 低库存、待处理 |
| Danger | `#b42318` | 删除、异常 |
| Success | `#047857` | 正常、启用、确认 |

## Components

### Buttons

- 主操作使用 `primary`，高度 34-36px。
- 次要操作使用浅边框按钮。
- 表格行操作保持 link button，但 hover 时更清晰。
- 危险操作只在确认弹窗后执行。

### Tables

- 表头使用浅色背景，字体加粗。
- 行高适中，hover 有轻微底色。
- 状态字段统一使用 `ElTag effect="plain"`。
- 固定右侧操作列，保持业务操作稳定。

### Cards / Panels

- 业务页面只使用单层 `page-panel`，避免卡片套卡片。
- Dashboard 指标卡使用统一图标区、标题和数字层级。
- 面板圆角 8px，边框弱化，阴影轻。

### Forms / Dialogs

- 表单使用两列栅格，移动端降为单列。
- Dialog 顶部、底部留出明确边界。
- 表单控件宽度统一，减少跳动。

## Figma Handoff

当前环境没有可写入 Figma 的连接器或账号授权，无法直接把设计稿写入用户提供的 Figma 页面。

可同步到 Figma 的初始 Frame 建议：

- Frame 1：Dashboard Desktop，1440 x 1024
- Frame 2：List Page Desktop，1440 x 1024
- Frame 3：Document Dialog，960 x 720
- Frame 4：Login Page，1440 x 900

后续如果提供 Figma API token 或 Figma MCP/插件写入权限，可以将本设计稿中的 tokens、layout 和页面结构同步为可编辑 Figma frame。

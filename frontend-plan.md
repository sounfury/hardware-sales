# 五金销售管理系统 - Web 前端开发计划

## 技术选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5+ | 前端框架（Composition API + `<script setup>`） |
| Vite | 6+ | 构建工具 |
| Vue Router | 4 | 路由管理 |
| Pinia | 2 | 状态管理（用户信息、token） |
| Element Plus | 2 | UI 组件库 |
| Axios | 1 | HTTP 请求 |
| unplugin-auto-import + unplugin-vue-components | - | 自动导入 Element Plus 组件和 Vue API |

## 项目结构

```
hardware-sales-frontend/
├── public/
├── src/
│   ├── api/              # 按模块封装 API 请求
│   │   ├── auth.js
│   │   ├── product.js
│   │   ├── category.js
│   │   ├── supplier.js
│   │   ├── supplierProduct.js
│   │   ├── purchase.js
│   │   ├── sales.js
│   │   ├── inventory.js
│   │   ├── finance.js
│   │   ├── message.js
│   │   └── system.js
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── layout/           # 布局组件（侧边栏 + 顶栏 + 主区域）
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia stores（user）
│   ├── utils/            # 工具函数
│   │   └── request.js    # Axios 封装（baseURL、token 拦截、统一错误处理）
│   ├── views/            # 页面
│   │   ├── login/
│   │   ├── dashboard/
│   │   ├── product/
│   │   ├── category/
│   │   ├── supplier/
│   │   ├── supplier-product/
│   │   ├── purchase/
│   │   ├── sales/
│   │   ├── inventory/
│   │   ├── finance/
│   │   ├── message/
│   │   └── system/
│   ├── App.vue
│   └── main.js
├── index.html
├── vite.config.js
└── package.json
```

## 页面规划

### 登录页 `/login`
- 账号密码登录表单
- 登录成功后跳转首页，token 存 localStorage

### 首页/仪表盘 `/dashboard`
- 财务概览卡片（总收入、总支出、净利润）
- 应收/应付金额
- 快捷入口

### 商品分类管理 `/category`
- 分类列表（表格）
- 新增/编辑对话框
- 删除确认

### 商品管理 `/product`
- 分页表格 + 搜索栏（名称、品牌、分类下拉）
- 新增/编辑对话框
- 删除确认

### 供应商管理 `/supplier`
- 分页表格 + 搜索栏（企业名称、审核状态）
- 新增/编辑对话框
- 审核操作（通过/驳回 + 审核备注）
- 删除确认

### 供应商商品 `/supplier-product`
- 分页表格 + 搜索栏（供应商、商品名称）
- 新增/编辑对话框

### 采购管理 `/purchase`
- 采购单分页列表 + 搜索栏（单号、供应商、日期范围）
- 新建采购单页面（选供应商 → 添加商品明细 → 提交）
- 采购单详情查看
- 结算操作

### 销售管理 `/sales`
- 销售单分页列表 + 搜索栏（单号、客户、日期范围）
- 新建销售单页面（填客户信息 → 添加商品明细 → 提交）
- 销售单详情查看
- 结算操作

### 库存流水 `/inventory`
- 分页表格 + 筛选栏（商品、出入库类型、日期范围）
- 只读查询，无增删改

### 财务管理 `/finance`
- 收支记录分页表格 + 筛选栏（类型、结算状态、日期范围）
- 汇总卡片（总收入、总支出、净利润、应收、应付）
- 只读查询

### 站内消息 `/message`
- 消息列表（分页）
- 发送消息对话框（选择供应商）
- 标记已读

### 系统管理
- 用户管理 `/system/user`：分页表格 + 新增/编辑/删除/重置密码
- 数据库备份 `/system/db`：备份下载按钮 + 还原上传

## 公共机制

### Axios 封装 (`utils/request.js`)
- `baseURL: '/api'`，Vite 配置代理到 `localhost:8080`
- 请求拦截器：附加 `satoken` header
- 响应拦截器：`code !== 200` 时 `ElMessage.error(msg)`，401 跳登录页

### 路由守卫
- 未登录重定向到 `/login`
- 已登录禁止访问 `/login`

### 布局
- 左侧菜单栏（可折叠）+ 顶栏（用户信息、退出）+ 右侧内容区
- Element Plus `el-menu` 侧边导航

## 开发顺序

分 5 个阶段，每阶段产出可运行的功能：

1. **项目骨架**：脚手架 + 布局 + 路由 + Axios + 登录
2. **基础数据**：商品分类、商品管理
3. **供应商**：供应商管理、供应商商品、站内消息
4. **进销存**：采购管理、销售管理、库存流水
5. **财务与系统**：财务管理、仪表盘、用户管理、数据库备份还原

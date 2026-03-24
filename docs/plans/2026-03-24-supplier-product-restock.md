# 供应商商品关联与补货提醒 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将供应商商品重构为真实商品关联，并基于该关联实现低库存补货提醒的普通落库与前端轮询展示。

**Architecture:** 后端先调整数据库结构、实体和接口响应，再将消息模型扩展为轻量业务消息，最后改前端供应商商品、商品管理、仪表盘和消息页，形成“商品 -> 供应商报价 -> 补货提醒”的闭环。实现中不保留旧字段兼容，不引入实时推送。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、MPJoin、Vue 3、Element Plus、MySQL 8

---

### Task 1: 重构数据库结构

**Files:**
- Modify: `sql/ddl.sql`

**Step 1: 更新 `supplier_product` 表结构**

- 删除 `product_name`
- 删除 `spec`
- 增加 `product_id`
- 增加 `fk_sp_product`
- 增加唯一键 `(supplier_id, product_id)`

**Step 2: 更新 `message` 表结构**

- 增加 `type`
- 增加 `product_id`
- 增加 `fk_msg_product`

**Step 3: 复核 DDL 一致性**

确认表注释、字段注释、外键和唯一键与设计保持一致。

### Task 2: 重构供应商商品后端模型

**Files:**
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/entity/SupplierProduct.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/SupplierProductService.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/impl/SupplierProductServiceImpl.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/SupplierProductController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/mapper/SupplierProductMapper.java`

**Step 1: 调整实体字段**

- 落库字段改为 `supplierId`、`productId`、`supplyPrice`、`remark`
- 增加非数据库字段 `supplierName`、`productName`、`productSpec`、`productUnit`

**Step 2: 重写分页查询**

- 支持 `supplierId`、`productId`、`productName`
- 联查 `supplier` 和 `product`
- 返回带展示字段的分页结果

**Step 3: 新增按商品查询供应商报价接口**

- 新增 `/by-product`
- 结果按 `supplyPrice` 升序

**Step 4: 增加必要校验**

- 新增/更新时校验 `supplierId`、`productId` 存在
- 阻止重复的 `(supplierId, productId)`

### Task 3: 扩展消息后端模型

**Files:**
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/entity/Message.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/MessageService.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/impl/MessageServiceImpl.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/MessageController.java`

**Step 1: 调整消息实体**

- 增加 `type`
- 增加 `productId`
- 增加非数据库字段 `productName`

**Step 2: 扩展分页查询**

- 支持按 `type` 查询
- 联查商品名称与发送人昵称

**Step 3: 新增补货提醒接口**

- 新增 `POST /api/message/restock`
- 入参至少包含 `receiverId`、`productId`、`content`
- 后端自动写入 `type=RESTOCK`

**Step 4: 使用当前登录人作为发送人**

- 避免前端传 `senderId` 导致脏数据

### Task 4: 改造前端供应商商品页

**Files:**
- Modify: `hardware-sales-frontend/src/api/supplierProduct.js`
- Modify: `hardware-sales-frontend/src/views/supplier-product/index.vue`

**Step 1: 扩展接口调用参数**

- 支持 `productId`
- 支持按商品查询供应商列表

**Step 2: 改表单为选择系统商品**

- 加载商品列表
- `productName` 输入框改为 `productId` 下拉选择

**Step 3: 更新表格展示**

- 展示 `productName`、`productSpec`、`productUnit`
- 保留供应商、报价、备注

### Task 5: 改造消息页

**Files:**
- Modify: `hardware-sales-frontend/src/api/message.js`
- Modify: `hardware-sales-frontend/src/views/message/index.vue`

**Step 1: 支持消息类型**

- 查询参数增加 `type`
- 列表展示 `type` 和关联商品名

**Step 2: 发送普通消息保持不变**

- 仍支持管理员手工发普通消息
- 普通消息由后端取当前用户为发送人

**Step 3: 增加未读轮询**

- 轮询 `/unread-count`
- 组件卸载时清理定时器

### Task 6: 打通低库存提醒入口

**Files:**
- Modify: `hardware-sales-frontend/src/views/product/index.vue`
- Modify: `hardware-sales-frontend/src/views/dashboard/index.vue`
- Reuse: `hardware-sales-frontend/src/api/supplierProduct.js`
- Reuse: `hardware-sales-frontend/src/api/message.js`

**Step 1: 商品页增加低库存操作**

- 低库存商品支持“查看供应商”
- 低库存商品支持“提醒补货”

**Step 2: 仪表盘增加相同入口**

- 低库存卡片支持打开供应商报价列表
- 支持直接发送补货提醒

**Step 3: 完成页面闭环**

- 从商品或仪表盘直接发送 `RESTOCK` 消息
- 成功后刷新未读数或给出结果提示

### Task 7: 验证

**Files:**
- Verify: `sql/ddl.sql`
- Verify: `hardware-sales-backend/pom.xml`
- Verify: `hardware-sales-frontend/package.json`

**Step 1: 后端编译验证**

Run: `cd hardware-sales-backend && mvn test`

**Step 2: 前端构建验证**

Run: `cd hardware-sales-frontend && npm run build`

**Step 3: 手工回归关键流程**

- 新建供应商商品关联
- 按商品查询供应商报价
- 发送普通消息
- 发送补货提醒
- 查看消息列表与未读数

**Step 4: 同步文档**

- 更新 `接口文档.md`
- 更新 `目前问题.md` 中已解决项

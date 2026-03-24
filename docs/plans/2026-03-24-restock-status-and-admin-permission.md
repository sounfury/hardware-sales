# Restock Status And Admin Permission Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为商品补货流程增加阈值和补货中状态，补齐补货提醒回复链路，并把后台 Web 权限收紧为仅管理员可进入。

**Architecture:** 采用轻量状态建模方案，把补货状态挂在 `product` 上，把沟通记录继续放在 `message` 上。后台通过 Sa-Token 角色校验统一限制为 `ADMIN`，供应商后续只通过小程序端处理补货消息，不再进入管理后台。

**Tech Stack:** Spring Boot 3、MyBatis-Plus、MyBatis-Plus-Join、Sa-Token、Vue 3、Element Plus、Vite、MySQL

---

### Task 1: 扩展数据模型与实体字段

**Files:**
- Modify: `sql/ddl.sql`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/entity/Product.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/entity/Message.java`
- Modify: `sql/demo_data.sql`

**Step 1: 写失败前提检查**

确认当前表结构不满足需求：

- `product` 只有 `stock`，没有 `restockThreshold`、`restockStatus`、`restockSupplierId`
- `message` 没有 `replyToMessageId`

Run:

```bash
rg -n "restockThreshold|restockStatus|restockSupplierId|replyToMessageId" sql/ddl.sql hardware-sales-backend/src/main/java/com/hardware/sales/entity
```

Expected:

- 没有匹配结果

**Step 2: 修改 DDL**

在 `product` 表新增：

```sql
restock_threshold INT NOT NULL DEFAULT 10 COMMENT '补货阈值',
restock_status TINYINT NOT NULL DEFAULT 0 COMMENT '补货状态：0-正常 1-补货中',
restock_supplier_id BIGINT COMMENT '当前补货跟进供应商ID'
```

在 `message` 表新增：

```sql
reply_to_message_id BIGINT COMMENT '回复的原始消息ID'
```

并补充外键：

```sql
CONSTRAINT fk_product_restock_supplier FOREIGN KEY (restock_supplier_id) REFERENCES supplier (id),
CONSTRAINT fk_msg_reply_to FOREIGN KEY (reply_to_message_id) REFERENCES message (id)
```

**Step 3: 修改实体**

在 `Product` 中增加字段：

```java
private Integer restockThreshold;
private Integer restockStatus;
private Long restockSupplierId;
```

在 `Message` 中增加字段：

```java
private Long replyToMessageId;
```

必要时补充非数据库展示字段：

```java
@TableField(exist = false)
private String replyToTitle;
```

**Step 4: 更新演示数据**

为 `sql/demo_data.sql` 中商品补充更真实的补货阈值：

- 电钻：10
- 角磨机：8
- 切割片：20
- BV2.5电线：12

并补入至少一条“补货提醒 -> 供应商回复”的样例消息。

**Step 5: 手动验证**

Run:

```bash
rg -n "restock_threshold|restock_status|restock_supplier_id|reply_to_message_id" sql/ddl.sql sql/demo_data.sql hardware-sales-backend/src/main/java/com/hardware/sales/entity
```

Expected:

- 能看到新增字段定义和示例数据

---

### Task 2: 收紧后台 Web 权限为仅管理员可访问

**Files:**
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/AuthController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/CategoryController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/ProductController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/SupplierController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/SupplierProductController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/MessageController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/PurchaseOrderController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/SalesOrderController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/InventoryLogController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/FinanceRecordController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/SystemController.java`

**Step 1: 写失败前提检查**

确认当前后台接口没有角色限制：

Run:

```bash
rg -n "@SaCheckRole|SaCheckRole" hardware-sales-backend/src/main/java/com/hardware/sales/controller
```

Expected:

- 没有匹配结果

**Step 2: 限制后台登录角色**

在 `AuthController.login` 中加入：

```java
if (!"ADMIN".equals(user.getRole())) {
    throw new BizException("当前账号无权登录管理后台");
}
```

这样 `BUSINESS` 和 `SUPPLIER` 都不能进入后管。

**Step 3: 给后台控制器增加角色注解**

在所有管理端 controller 类上加：

```java
@SaCheckRole("ADMIN")
```

保留 `@RestController` 和 `@RequestMapping` 不变。

**Step 4: 保留登录与退出接口**

不要给 `/api/auth/login` 加角色注解，角色限制由登录方法内部处理；`/api/auth/info`、`/api/auth/logout` 仍然要求登录即可。

**Step 5: 验证**

Run:

```bash
rg -n "@SaCheckRole\\(\"ADMIN\"\\)" hardware-sales-backend/src/main/java/com/hardware/sales/controller
```

Expected:

- 所有管理端 controller 都能看到管理员角色注解

---

### Task 3: 补齐补货状态和消息回复的后端能力

**Files:**
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/MessageService.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/impl/MessageServiceImpl.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/MessageController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/ProductService.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/impl/ProductServiceImpl.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/ProductController.java`

**Step 1: 写失败测试草案**

新增后端集成测试，覆盖：

- 发送补货提醒后，商品状态变为 `补货中`
- 非法重复发送补货提醒时被拒绝
- 供应商回复补货提醒时，消息能关联原始提醒
- 管理员确认补货完成后，商品状态恢复正常

建议新增测试文件：

```text
hardware-sales-backend/src/test/java/com/hardware/sales/service/impl/RestockFlowServiceTest.java
```

**Step 2: 补充消息类型**

统一消息类型：

```java
"GENERAL"
"RESTOCK_NOTICE"
"RESTOCK_REPLY"
```

`sendRestockMessage` 改为发送 `RESTOCK_NOTICE`。

**Step 3: 发送补货提醒时联动商品状态**

在 `sendRestockMessage` 中增加逻辑：

```java
product.setRestockStatus(1);
product.setRestockSupplierId(supplier.getId());
productService.updateById(product);
```

同时防止重复发起：

```java
if (product.getRestockStatus() != null && product.getRestockStatus() == 1) {
    throw new BizException("该商品当前已处于补货中");
}
```

**Step 4: 新增供应商回复接口**

在 `MessageService` / `MessageController` 增加：

```java
void replyRestockMessage(Message message, Long senderId);
```

```java
@PostMapping("/restock-reply")
public Result<?> replyRestock(@RequestBody Message message) { ... }
```

逻辑要求：

- `replyToMessageId` 必填
- 原消息必须存在且类型为 `RESTOCK_NOTICE`
- 回复消息类型为 `RESTOCK_REPLY`
- 回复接收人应为原消息发送人
- 回复消息继承原消息的 `productId`

**Step 5: 新增补货完成接口**

在 `ProductController` 增加：

```java
@PutMapping("/restock-complete/{id}")
public Result<?> completeRestock(@PathVariable Long id) { ... }
```

对应 service 逻辑：

```java
product.setRestockStatus(0);
product.setRestockSupplierId(null);
productService.updateById(product);
```

**Step 6: 完善联表查询**

消息分页时补充原始消息标题：

```java
.selectAs(MessageReply::getTitle, Message::getReplyToTitle)
```

如果 MPJ 处理自连接复杂，就先不展示 `replyToTitle`，只保留 `replyToMessageId`。

**Step 7: 跑测试**

Run:

```bash
mvn -Dtest=PurchaseOrderServiceImplTest,RestockFlowServiceTest test
```

Expected:

- 相关补货流测试全部通过

---

### Task 4: 改造后台商品页与消息页交互

**Files:**
- Modify: `hardware-sales-frontend/src/views/product/index.vue`
- Modify: `hardware-sales-frontend/src/views/dashboard/index.vue`
- Modify: `hardware-sales-frontend/src/views/message/index.vue`
- Modify: `hardware-sales-frontend/src/api/product.js`
- Modify: `hardware-sales-frontend/src/api/message.js`

**Step 1: 商品页增加补货阈值**

在新增/编辑商品弹窗中加入：

```vue
<el-input-number v-model="formData.restockThreshold" :min="0" />
```

并在列表中增加两列：

- `补货阈值`
- `补货状态`

状态文案建议：

- `正常`
- `补货中`

**Step 2: 低库存判断改用阈值字段**

把所有 `stock < 10` 改成：

```js
Number(row.stock || 0) <= Number(row.restockThreshold || 0)
```

但显示逻辑要区分：

- `restockStatus === 1` 时显示“补货中”
- 否则才显示“低库存/缺货”

**Step 3: 调整商品页按钮**

当商品低于阈值且未补货：

- 显示 `查看供应商`
- 显示 `提醒补货`

当商品处于补货中：

- 显示 `补货中`
- 显示 `完成补货`

**Step 4: 仪表盘同步状态展示**

低库存卡片逻辑改成：

- 若 `restockStatus = 0`，允许发提醒
- 若 `restockStatus = 1`，显示“补货中”，不重复发起

**Step 5: 消息页增强展示**

后台消息页增加：

- `RESTOCK_NOTICE` 显示为“补货提醒”
- `RESTOCK_REPLY` 显示为“供应商回复”
- 有 `replyToMessageId` 时显示“回复了某条补货提醒”

普通“发送消息”按钮保留，但只允许管理员给供应商发。

---

### Task 5: 后台前端禁止非管理员继续使用

**Files:**
- Modify: `hardware-sales-frontend/src/stores/user.js`
- Modify: `hardware-sales-frontend/src/views/login/index.vue`
- Modify: `hardware-sales-frontend/src/router/index.js`

**Step 1: 确认当前前端没有角色拦截**

Run:

```bash
rg -n "role|ADMIN|BUSINESS|SUPPLIER" hardware-sales-frontend/src/router hardware-sales-frontend/src/stores
```

Expected:

- 没有后台角色准入判断

**Step 2: 登录后角色校验**

在用户登录成功后检查：

```js
if (user.role !== 'ADMIN') {
  throw new Error('当前账号无权登录管理后台')
}
```

如果后端已经拦住，这里作为前端兜底提示。

**Step 3: 路由守卫加管理员校验**

路由守卫中增加：

```js
if (userStore.userInfo?.role !== 'ADMIN') {
  userStore.logout()
  next('/login')
}
```

避免残留 token 时继续访问后台页面。

**Step 4: 登录页提示文案**

登录页增加一行辅助提示：

- 当前仅管理员可登录后台
- 供应商请使用微信小程序

---

### Task 6: 更新文档与演示数据说明

**Files:**
- Modify: `接口文档.md`
- Modify: `目前问题.md`
- Modify: `docs/前端菜单说明书.md`
- Create: `docs/plans/2026-03-24-restock-status-design.md`（如需补完整设计留档）

**Step 1: 更新接口文档**

补充：

- 商品新增字段 `restockThreshold`、`restockStatus`、`restockSupplierId`
- `POST /api/message/restock`
- `POST /api/message/restock-reply`
- `PUT /api/product/restock-complete/{id}`
- 后台仅管理员可访问

**Step 2: 更新问题文档**

把“缺货提醒只有通知没有状态流转”列为已整改项。

**Step 3: 更新说明书**

在 `docs/前端菜单说明书.md` 中补充：

- 商品页如何设置补货阈值
- 补货中状态如何展示
- 管理员如何完成补货
- 供应商回复补货提醒的业务口径
- 供应商不进入后台，只在小程序处理消息

---

### Task 7: 完整验证

**Files:**
- Test: `hardware-sales-backend/src/test/java/com/hardware/sales/service/impl/PurchaseOrderServiceImplTest.java`
- Test: `hardware-sales-backend/src/test/java/com/hardware/sales/service/impl/RestockFlowServiceTest.java`

**Step 1: 跑后端测试**

Run:

```bash
cd hardware-sales-backend && mvn test
```

Expected:

- 全部测试通过

**Step 2: 跑前端构建**

Run:

```bash
cd hardware-sales-frontend && npm run build
```

Expected:

- 构建成功

**Step 3: 启动并人工验证**

Run:

```bash
cd hardware-sales-backend && mvn spring-boot:run
```

```bash
cd hardware-sales-frontend && npm run dev -- --host 0.0.0.0
```

人工验证清单：

1. `admin` 能登录后台
2. `business01` 不能登录后台
3. `supplier_tool_01` 不能登录后台
4. 编辑商品时能设置补货阈值
5. 电钻库存低于阈值时可发补货提醒
6. 发提醒后商品状态变为“补货中”
7. 补货中时不能重复发起提醒
8. 管理员点“完成补货”后状态恢复正常


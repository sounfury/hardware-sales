# 供应商商品关联与补货提醒设计

**目标**

将当前“文本型供应商商品表”重构为“系统商品与供应商的真实关联表”，并在此基础上打通“低库存商品 -> 可供货供应商 -> 补货提醒消息”的业务链路。消息能力采用普通数据库落库和前端轮询，不引入 WebSocket、消息队列或第三方推送。

## 当前问题

- `supplier_product` 仅保存 `product_name/spec` 文本，无法稳定关联系统商品。
- 低库存页面只能提示，不能直接找到对应供应商。
- 消息功能与具体商品无关，无法表达“补货提醒”这类业务消息。
- 前端页面之间缺少“商品 -> 供应商商品 -> 消息”的跳转与数据承接。

## 核心设计

### 1. 供应商商品模型

`supplier_product` 调整为真实关联表：

- `id`
- `supplier_id`
- `product_id`
- `supply_price`
- `remark`
- `create_time`
- `update_time`

约束：

- 外键 `supplier_id -> supplier.id`
- 外键 `product_id -> product.id`
- 唯一键 `(supplier_id, product_id)`

返回视图中补充冗余展示字段：

- `supplierName`
- `productName`
- `productSpec`
- `productUnit`

这些字段用于接口响应，不直接落库。

### 2. 消息模型

`message` 保持站内消息定位，增加轻量业务语义：

- `id`
- `sender_id`
- `receiver_id`
- `type`
- `product_id`
- `title`
- `content`
- `is_read`
- `create_time`

约定：

- `type = GENERAL` 表示普通消息
- `type = RESTOCK` 表示补货提醒
- `product_id` 在 `RESTOCK` 类型下必填，在普通消息下可空

### 3. 接口设计

#### 供应商商品

- `GET /api/supplier-product/page`
  - 查询条件：`supplierId`、`productId`、`productName`
- `GET /api/supplier-product/{id}`
- `GET /api/supplier-product/list?supplierId=`
- `GET /api/supplier-product/by-product?productId=`
- `POST /api/supplier-product`
- `PUT /api/supplier-product`
- `DELETE /api/supplier-product/{id}`

接口返回包含供应商名与商品基础信息，前端无需二次拼接。

#### 消息

- `GET /api/message/page`
  - 查询条件：`receiverId`、`isRead`、`type`
- `POST /api/message`
  - 普通消息发送
- `POST /api/message/restock`
  - 补货提醒发送
- `PUT /api/message/read/{id}`
- `PUT /api/message/read-all`
- `GET /api/message/unread-count`

`/api/message/restock` 由后端基于 `productId` 和供应商自动生成业务消息内容。

## 前端交互

### 1. 供应商商品页

- 新增/编辑时从“手填商品名称”改为“选择系统商品”
- 列表展示供应商、商品、规格、单位、供货价、备注
- 支持按供应商与商品搜索

### 2. 商品管理页

- 低库存商品增加操作：
  - 查看可供货供应商
  - 发起补货提醒

### 3. 仪表盘

- 低库存卡片增加“查看供应商”“提醒补货”入口

### 4. 消息页

- 支持展示消息类型与关联商品
- 普通消息保留
- 顶部未读数与页面消息列表采用轮询或页面刷新后拉取

## 非目标

- 不做 WebSocket 实时推送
- 不做 Redis、MQ、短信、邮件、微信模板消息
- 不在本次改造中重构采购流程为“必须从供应商报价中选货”

## 验收标准

- 能建立“供应商 - 系统商品”真实关联
- 能按商品查到供应商报价列表
- 低库存商品可直接发起补货提醒
- 消息列表可区分普通消息与补货提醒，并显示关联商品
- 未读数能通过轮询稳定刷新

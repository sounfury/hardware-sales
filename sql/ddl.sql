CREATE DATABASE IF NOT EXISTS hardware_sales DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE hardware_sales;

-- ----------------------------
-- 用户表（管理员用账号密码，供应商用微信openid）
-- ----------------------------
CREATE TABLE sys_user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)     COMMENT '登录用户名（管理端）',
    password    VARCHAR(255)    COMMENT '登录密码（管理端）',
    openid      VARCHAR(100)    COMMENT '微信openid（小程序端）',
    nickname    VARCHAR(50)     COMMENT '昵称',
    avatar      VARCHAR(255)    COMMENT '头像',
    phone       VARCHAR(20)     COMMENT '手机号',
    role        VARCHAR(20)     NOT NULL COMMENT '角色：ADMIN-业务管理员 SUPPLIER-供应商 CUSTOMER-客户 空字符串-待申请用户',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_openid (openid)
) COMMENT '用户表';

-- 默认业务管理员（密码: admin123）
INSERT INTO sys_user (username, password, nickname, role) VALUES ('admin', '$2a$10$kz.6hN31xf22XCCLuA3EM.sNM1wIZA2h1/SpD6L18Sh10TlIpsbHq', '业务管理员', 'ADMIN');

-- ----------------------------
-- 供应商信息表
-- ----------------------------
CREATE TABLE supplier (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL COMMENT '关联用户ID',
    company_name    VARCHAR(100)    NOT NULL COMMENT '企业名称',
    contact_person  VARCHAR(50)     COMMENT '联系人',
    contact_phone   VARCHAR(20)     COMMENT '联系电话',
    address         VARCHAR(255)    COMMENT '地址',
    business_scope  VARCHAR(500)    COMMENT '经营范围',
    audit_status    TINYINT         NOT NULL DEFAULT 0 COMMENT '0-待审核 1-通过 2-驳回',
    audit_remark    VARCHAR(255)    COMMENT '审核备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id),
    CONSTRAINT fk_supplier_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) COMMENT '供应商信息表';

-- ----------------------------
-- 商品分类
-- ----------------------------
CREATE TABLE product_category (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '商品分类表';

-- ----------------------------
-- 商品表
-- ----------------------------
CREATE TABLE product (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id     BIGINT          COMMENT '分类ID',
    name            VARCHAR(100)    NOT NULL COMMENT '商品名称',
    brand           VARCHAR(50)     COMMENT '品牌',
    spec            VARCHAR(100)    COMMENT '规格型号',
    description     VARCHAR(500)    COMMENT '商品描述/作用',
    unit            VARCHAR(20)     COMMENT '单位（个/箱/kg等）',
    purchase_price  DECIMAL(10,2)   COMMENT '参考进价',
    sale_price      DECIMAL(10,2)   COMMENT '售价',
    stock           INT             NOT NULL DEFAULT 0 COMMENT '当前库存',
    restock_threshold INT           NOT NULL DEFAULT 10 COMMENT '补货阈值，库存小于等于该值时触发提醒',
    restock_status  TINYINT         NOT NULL DEFAULT 0 COMMENT '0-正常 1-补货中',
    restock_supplier_id BIGINT      COMMENT '当前跟进补货的供应商ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category (id),
    CONSTRAINT fk_product_restock_supplier FOREIGN KEY (restock_supplier_id) REFERENCES supplier (id)
) COMMENT '商品表';

-- ----------------------------
-- 供应商-商品关联（供应商可供货的商品及报价）
-- ----------------------------
CREATE TABLE supplier_product (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id     BIGINT          NOT NULL COMMENT '供应商ID',
    product_id      BIGINT          NOT NULL COMMENT '商品ID',
    supply_price    DECIMAL(10,2)   COMMENT '供货价格',
    remark          VARCHAR(255)    COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_supplier_product (supplier_id, product_id),
    CONSTRAINT fk_sp_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id),
    CONSTRAINT fk_sp_product FOREIGN KEY (product_id) REFERENCES product (id)
) COMMENT '供应商商品关联表';

-- ----------------------------
-- 采购进货单
-- ----------------------------
CREATE TABLE purchase_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(30)     NOT NULL COMMENT '采购单号',
    supplier_id     BIGINT          NOT NULL COMMENT '供应商ID',
    total_amount    DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '总金额',
    payment_status  TINYINT         NOT NULL DEFAULT 0 COMMENT '0-未结算 1-已结算',
    order_date      DATE            NOT NULL COMMENT '采购日期',
    remark          VARCHAR(255)    COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id)
) COMMENT '采购进货单';

-- ----------------------------
-- 采购明细
-- ----------------------------
CREATE TABLE purchase_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT          NOT NULL COMMENT '采购单ID',
    product_id  BIGINT          NOT NULL COMMENT '商品ID',
    quantity    INT             NOT NULL COMMENT '数量',
    price       DECIMAL(10,2)   NOT NULL COMMENT '进价',
    amount      DECIMAL(12,2)   NOT NULL COMMENT '小计',
    INDEX idx_order_id (order_id),
    CONSTRAINT fk_pi_order   FOREIGN KEY (order_id)   REFERENCES purchase_order (id),
    CONSTRAINT fk_pi_product FOREIGN KEY (product_id) REFERENCES product (id)
) COMMENT '采购明细';

-- ----------------------------
-- 销售出货单
-- ----------------------------
CREATE TABLE sales_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(30)     NOT NULL COMMENT '销售单号',
    customer_user_id BIGINT         COMMENT '关联客户用户ID',
    customer_name   VARCHAR(50)     COMMENT '客户名称',
    customer_phone  VARCHAR(20)     COMMENT '客户电话',
    order_source    VARCHAR(20)     NOT NULL DEFAULT 'MANUAL' COMMENT '订单来源：MANUAL-后台创建 MINIAPP-小程序预定',
    total_amount    DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '总金额',
    payment_status  TINYINT         NOT NULL DEFAULT 0 COMMENT '0-未结算 1-已结算',
    order_date      DATE            NOT NULL COMMENT '销售日期',
    remark          VARCHAR(255)    COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_customer_user_id (customer_user_id),
    CONSTRAINT fk_sales_order_customer_user FOREIGN KEY (customer_user_id) REFERENCES sys_user (id)
) COMMENT '销售出货单';

-- ----------------------------
-- 销售明细
-- ----------------------------
CREATE TABLE sales_item (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT          NOT NULL COMMENT '销售单ID',
    product_id  BIGINT          NOT NULL COMMENT '商品ID',
    quantity    INT             NOT NULL COMMENT '数量',
    price       DECIMAL(10,2)   NOT NULL COMMENT '售价',
    amount      DECIMAL(12,2)   NOT NULL COMMENT '小计',
    INDEX idx_order_id (order_id),
    CONSTRAINT fk_si_order   FOREIGN KEY (order_id)   REFERENCES sales_order (id),
    CONSTRAINT fk_si_product FOREIGN KEY (product_id) REFERENCES product (id)
) COMMENT '销售明细';

-- ----------------------------
-- 库存变动流水
-- ----------------------------
CREATE TABLE inventory_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id      BIGINT      NOT NULL COMMENT '商品ID',
    type            TINYINT     NOT NULL COMMENT '1-入库 2-出库',
    quantity        INT         NOT NULL COMMENT '变动数量',
    before_stock    INT         NOT NULL COMMENT '变动前库存',
    after_stock     INT         NOT NULL COMMENT '变动后库存',
    ref_type        VARCHAR(20) COMMENT 'PURCHASE-采购 SALES-销售',
    ref_order_id    BIGINT      COMMENT '关联单据ID',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    CONSTRAINT fk_invlog_product FOREIGN KEY (product_id) REFERENCES product (id)
) COMMENT '库存变动流水';

-- ----------------------------
-- 收支记录（采购/销售时自动生成）
-- ----------------------------
CREATE TABLE finance_record (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    type            TINYINT         NOT NULL COMMENT '1-收入(销售) 2-支出(采购)',
    amount          DECIMAL(12,2)   NOT NULL COMMENT '金额',
    ref_type        VARCHAR(20)     COMMENT 'PURCHASE-采购 SALES-销售',
    ref_order_id    BIGINT          COMMENT '关联单据ID',
    payment_status  TINYINT         NOT NULL DEFAULT 0 COMMENT '0-未结算 1-已结算',
    remark          VARCHAR(255)    COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '收支记录';

-- ----------------------------
-- 站内消息（供货提醒/反馈）
-- ----------------------------
CREATE TABLE message (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id   BIGINT      NOT NULL COMMENT '发送人ID',
    receiver_id BIGINT      NOT NULL COMMENT '接收人ID',
    type        VARCHAR(20) NOT NULL DEFAULT 'GENERAL' COMMENT '消息类型：GENERAL-普通消息 RESTOCK_NOTICE-补货提醒 RESTOCK_REPLY-补货回复',
    product_id  BIGINT      COMMENT '关联商品ID',
    reply_to_message_id BIGINT COMMENT '回复关联的原消息ID',
    title       VARCHAR(100) COMMENT '标题',
    content     TEXT         COMMENT '内容',
    is_read     TINYINT     NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_reply_to_message_id (reply_to_message_id),
    CONSTRAINT fk_msg_sender   FOREIGN KEY (sender_id)   REFERENCES sys_user (id),
    CONSTRAINT fk_msg_receiver FOREIGN KEY (receiver_id) REFERENCES sys_user (id),
    CONSTRAINT fk_msg_product  FOREIGN KEY (product_id)  REFERENCES product (id),
    CONSTRAINT fk_msg_reply_to FOREIGN KEY (reply_to_message_id) REFERENCES message (id)
) COMMENT '站内消息';

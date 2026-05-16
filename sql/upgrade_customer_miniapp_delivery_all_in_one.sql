USE hardware_sales;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 客户下单简化版 一体化增量脚本
-- 适用场景：
-- 1. 服务器数据库已经建表完成
-- 2. 需要一次性补齐客户角色、销售单来源、管理员昵称修正
-- 3. 可选补充演示用客户账号
-- =========================================================

-- ---------------------------------------------------------
-- 1. 统一数据库与用户表字符集，避免中文昵称写成问号
-- ---------------------------------------------------------
ALTER DATABASE hardware_sales CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

ALTER TABLE sys_user
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- ---------------------------------------------------------
-- 2. 统一 sys_user.role 注释，纳入 CUSTOMER 角色
-- ---------------------------------------------------------
ALTER TABLE sys_user
    MODIFY COLUMN role VARCHAR(20) NOT NULL COMMENT '角色：ADMIN-业务管理员 SUPPLIER-供应商 CUSTOMER-客户 空字符串-待申请用户';

-- ---------------------------------------------------------
-- 3. 修正默认管理员昵称，避免历史乱码
-- 使用十六进制 UTF-8 写法，降低控制台编码干扰
-- “业务管理员” 的 UTF-8 HEX：E4B89AE58AA1E7AEA1E79086E59198
-- ---------------------------------------------------------
UPDATE sys_user
SET nickname = CONVERT(0xE4B89AE58AA1E7AEA1E79086E59198 USING utf8mb4)
WHERE username = 'admin';

-- ---------------------------------------------------------
-- 4. 清理历史 BUSINESS 角色账号（如库里仍有遗留）
-- ---------------------------------------------------------
DELETE FROM sys_user
WHERE role = 'BUSINESS';

-- ---------------------------------------------------------
-- 5. 为销售单补充客户账号与订单来源字段
-- ---------------------------------------------------------
ALTER TABLE sales_order
    ADD COLUMN customer_user_id BIGINT NULL COMMENT '关联客户用户ID' AFTER order_no,
    ADD COLUMN order_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '订单来源：MANUAL-后台创建 MINIAPP-小程序预定' AFTER customer_phone;

ALTER TABLE sales_order
    ADD INDEX idx_customer_user_id (customer_user_id),
    ADD CONSTRAINT fk_sales_order_customer_user FOREIGN KEY (customer_user_id) REFERENCES sys_user (id);

UPDATE sales_order
SET order_source = 'MANUAL'
WHERE order_source IS NULL OR order_source = '';

-- ---------------------------------------------------------
-- 6. 可选：补充 3 个客户演示账号
-- 统一密码：admin123
-- 说明：
-- 下面昵称同样使用十六进制 UTF-8，避免中文乱码
-- 王晓雨：E78E8BE69993E99BA8
-- 李明轩：E69D8EE6988EE8BDA9
-- 陈思远：E99988E6809DE8BF9C
-- ---------------------------------------------------------
INSERT INTO sys_user (username, password, nickname, phone, role, status)
SELECT
    'customer_demo_01',
    '$2a$10$kz.6hN31xf22XCCLuA3EM.sNM1wIZA2h1/SpD6L18Sh10TlIpsbHq',
    CONVERT(0xE78E8BE69993E99BA8 USING utf8mb4),
    '13700001111',
    'CUSTOMER',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'customer_demo_01'
);

INSERT INTO sys_user (username, password, nickname, phone, role, status)
SELECT
    'customer_demo_02',
    '$2a$10$kz.6hN31xf22XCCLuA3EM.sNM1wIZA2h1/SpD6L18Sh10TlIpsbHq',
    CONVERT(0xE69D8EE6988EE8BDA9 USING utf8mb4),
    '13700002222',
    'CUSTOMER',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'customer_demo_02'
);

INSERT INTO sys_user (username, password, nickname, phone, role, status)
SELECT
    'customer_demo_03',
    '$2a$10$kz.6hN31xf22XCCLuA3EM.sNM1wIZA2h1/SpD6L18Sh10TlIpsbHq',
    CONVERT(0xE99988E6809DE8BF9C USING utf8mb4),
    '13700003333',
    'CUSTOMER',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'customer_demo_03'
);

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------
-- 7. 执行后建议自检
-- ---------------------------------------------------------
-- SELECT username, nickname, role, phone, status FROM sys_user WHERE username IN ('admin', 'customer_demo_01', 'customer_demo_02', 'customer_demo_03');
-- SHOW FULL COLUMNS FROM sys_user LIKE 'nickname';
-- SELECT order_no, customer_user_id, order_source FROM sales_order LIMIT 10;

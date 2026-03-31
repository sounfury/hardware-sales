USE hardware_sales;

-- 将已有供应商账号的角色与 supplier.audit_status 对齐：
-- audit_status = 1 -> SUPPLIER
-- audit_status = 0/2 -> 空角色
UPDATE sys_user AS u
INNER JOIN supplier AS s ON s.user_id = u.id
SET u.role = CASE
    WHEN s.audit_status = 1 THEN 'SUPPLIER'
    ELSE ''
END;

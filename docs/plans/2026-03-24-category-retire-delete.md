# Category Retire/Delete Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将商品分类从“直接删除”改为“停用/迁移/空分类可删”，避免误伤商品及其下游业务。

**Architecture:** 在 `product_category` 表增加状态字段，后端把分类删除改为显式业务校验，并新增停用/启用、商品迁移能力。前端分类页补充停用、迁移、删除规则提示，商品页只允许选择启用中的分类。

**Tech Stack:** MySQL 8, Spring Boot 3, MyBatis-Plus, Vue 3, Element Plus

---

### Task 1: 数据库与 DDL 同步

**Files:**
- Modify: `sql/ddl.sql`

**Step 1: 更新 DDL**

- 为 `product_category` 增加 `status` 字段，约定 `0-停用 1-启用`
- 为历史数据设置默认值 `1`

**Step 2: 更新在线 MySQL**

Run: `docker exec mysql mysql -usounfury -pa2133266 -D hardware_sales -e "ALTER TABLE ..."`
Expected: 列存在且历史分类状态为 `1`

### Task 2: 后端分类业务规则

**Files:**
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/entity/ProductCategory.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/controller/ProductCategoryController.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/ProductCategoryService.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/impl/ProductCategoryServiceImpl.java`
- Modify: `hardware-sales-backend/src/main/java/com/hardware/sales/service/impl/ProductServiceImpl.java`

**Step 1: 补充分类状态字段与查询过滤**

- 分类列表支持按状态过滤
- 默认列表返回全部，商品页可取启用中的分类

**Step 2: 实现分类停用/启用、迁移、删除校验**

- 停用/启用分类
- 批量迁移某分类下商品到目标分类
- 删除前校验“分类已停用且分类下无商品”
- 非空删除、启用状态删除、目标分类非法等场景抛 `BizException`

**Step 3: 在商品保存/编辑时校验分类可用性**

- 禁止把商品保存到停用分类
- 编辑已有商品时也执行同样校验

### Task 3: 前端分类与商品交互

**Files:**
- Modify: `hardware-sales-frontend/src/api/category.js`
- Modify: `hardware-sales-frontend/src/views/category/index.vue`
- Modify: `hardware-sales-frontend/src/views/product/index.vue`

**Step 1: 分类页增加状态展示与操作**

- 展示分类状态
- 支持启用/停用
- 支持迁移商品
- 删除按钮只作为“空分类清理”

**Step 2: 商品页按场景加载分类**

- 搜索栏支持全部分类
- 商品表单只展示启用分类
- 编辑停用分类下的旧商品时，当前分类仍可显示但不能用于新分配

### Task 4: 验证

**Files:**
- Test: `hardware-sales-backend/src/test/java/com/hardware/sales/service/impl/ProductCategoryServiceImplTest.java`

**Step 1: 增加后端测试**

- 验证停用分类不可直接删除
- 验证非空分类不可删除
- 验证迁移后可删除空停用分类
- 验证商品不能绑定停用分类

**Step 2: 运行验证**

Run: `mvn test`
Expected: 后端测试通过

Run: `npm run build`
Expected: 前端构建通过

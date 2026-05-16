# 项目运行指南（小白版）

这份 README 只讲一件事：怎么把项目跑起来。

## 一、先准备这几个软件

先在电脑里装好下面几个软件：

1. `MySQL 8.x`
2. `JDK 17`
3. `IntelliJ IDEA`
4. `Node.js 20+`（安装后会自带 `npm`）
5. `微信开发者工具`

如果这些软件还没装，先装完再继续。

## 二、先把数据库准备好

### 1. 新建数据库

这个项目用的是 MySQL，数据库名叫：

`hardware_sales`

其实 `sql\ddl.sql` 里已经写了建库语句，所以你直接执行这个文件也可以。

### 2. 导入表结构

把下面这个文件导入到 MySQL：

`sql\ddl.sql`

### 3. 导入演示数据

再导入这个文件：

`sql\demo_data.sql`

这样项目跑起来后，页面里就不是空的。

### 4. 检查后端数据库配置

打开这个文件：

`hardware-sales-backend\src\main\resources\application.yml`

里面默认写的是：

- 数据库地址：`jdbc:mysql://localhost:3306/hardware_sales`
- 用户名：`root`
- 密码：`a2133266`

如果你的 MySQL 用户名或密码不是这个，就把这里改成你自己电脑上的。

## 三、启动后端（用 IDEA）

### 1. 用 IDEA 打开后端项目

在 IDEA 里选择：

`Open` -> 选择文件夹 `hardware-sales-backend`

### 2. 等待依赖下载

第一次打开时，IDEA 会自动下载 Maven 依赖。等右下角不再转圈，或者等它下载完成。

### 3. 检查 Java 版本

这个项目后端要用：

`JDK 17`

如果 IDEA 里不是 17，改成 17 再启动。

### 4. 启动项目

找到启动类：

`com.hardware.sales.HardwareSalesBackendApplication`

直接点运行。

### 5. 启动成功怎么看

后端默认端口是：

`8080`

看到类似“启动成功”或者项目正常运行在 `8080` 端口，就说明后端好了。

## 四、启动 Web 前端

### 1. 打开前端目录

进入文件夹：

`hardware-sales-frontend`

### 2. 安装依赖

在这个目录打开 PowerShell，执行：

```powershell
npm i
```

### 3. 启动前端

继续执行：

```powershell
npm run dev
```

### 4. 打开网页

启动后，浏览器打开：

[http://localhost:5173](http://localhost:5173)

### 5. 登录账号

如果你已经导入了 `sql\demo_data.sql`，可以先用这个业务管理员账号登录：

- 用户名：`admin`
- 密码：`admin123`

## 五、启动微信小程序

### 1. 打开微信开发者工具

选择“导入项目”。

### 2. 选择小程序目录

项目目录选择：

`hardware-sales-wxapp`

### 3. AppID 说明

如果能直接导入，就直接导入。

如果提示 AppID 权限问题，就把 AppID 换成你自己的，或者换成测试号再导入。

### 4. 小程序默认连后端地址

小程序默认请求的是：

`http://127.0.0.1:8080`

对应文件在：

`hardware-sales-wxapp\utils\request.js`

所以：

1. 后端必须先启动
2. 如果你是用开发者工具本机调试，一般不用改
3. 如果你要真机调试，或者后端不在你这台电脑上，就把 `BASE_URL` 改成你后端实际地址(power shell 输入 ipconfig 查看)

## 六、推荐启动顺序

第一次运行，最推荐按这个顺序来：

1. 先启动 MySQL
2. 导入 `sql\ddl.sql`
3. 导入 `sql\demo_data.sql`
4. 用 IDEA 启动后端
5. 进入 `hardware-sales-frontend` 执行 `npm i`
6. 执行 `npm run dev`
7. 最后再打开微信开发者工具导入 `hardware-sales-wxapp`

## 七、如果网页打不开，优先检查这 4 件事

1. MySQL 有没有启动
2. `application.yml` 里的数据库账号密码对不对
3. 后端有没有成功跑在 `8080`
4. 前端是不是已经执行了 `npm i` 和 `npm run dev`

照着上面一步一步做，Web 前端、后端、微信小程序都可以跑起来。

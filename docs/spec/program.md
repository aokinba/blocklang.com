# 技术架构

## 项目结构

### Server

服务器端基于 Spring Boot 开发，使用以下组件：

1. Spring MVC - 开发 web 端
2. Spring Data JPA - 数据库交互组件
3. Spring Security - Spring 安全框架
4. Spring Cache - Spring 缓存组件
5. Thymeleaf - HTML 模板引擎
6. jgit - Java 实现的 git 版本控制组件
7. Liquibase - 管理和重构数据库表结构，以及管理系统初始数据
8. PostgreSQL - 数据库
9. Spring boot test - Spring boot 自动化测试工具
10. Rest Assured - Rest API 自动化测试工具

#### package 结构

1. `com.blocklang.database` - 数据库概念模型设计和物理模型开发
2. `com.blocklang.develop` - 软件开发
3. `com.blocklang.release` - 软件发布
4. `com.blocklang.core` - 通用功能

依赖关系：

1. `database`、`develop`、`release` 可依赖 `core` 包
2. `database`、`develop`、`release` 包之间不要互相依赖

#### `src/main/java` 目录

1. `controller` - 存放本项目专用的 HTTP 服务
2. `api` - 存放共享的 HTTP 服务
3. `service` - 存放业务逻辑接口
4. `service.impl` - 存放业务逻辑实现类
5. `dao` - 存放数据访问接口
6. `dao.impl` - 存放数据访问实现类
7. `model` - 存放实体对象
8. `data` - 存放 POJO 对象，是对 model 实体对象的组合

#### `src/main/resources` 目录

1. `db/changelog/data` - 存放初始化数据
2. `db/changelog/table` - 存放变更的表脚本
3. `db/db.sql` - 存放建库脚本
4. `help/` - 存放帮助文档

### Client

客户端使用以下组件

1. [Dojo](https://dojo.io/) - 最新版 Dojo Framework、Widgets 以及 CLI 等开发工具
2. [Bootstrap](http://getbootstrap.com/) - 使用 bootstrap 中的样式
3. [Fontawesome](https://fontawesome.com/) - 图标库

#### 目录结构

1. `src/index.html` - 宿主文件
2. `src/main.ts` - 入口文件
3. `src/App.ts` - 在这里使用项目中的组件组装 APP
4. `src/interfaces.d.ts` - 存放数据结构
5. `src/routes.ts` - 存放路由配置信息
6. `src/config.ts` - 存放项目配置信息
7. `src/containers/**` - 存放 container 部件
8. `src/processes/**` - dojo store 处理逻辑，用户查询和修改 store 中的数据
9. `src/pages/**` - 存放页面级别的部件
10. `src/pages/widgets/**` - 存放页面中共用的业务相关部件
11. `src/widgets/**` - 存放通用部件，页面级别的部件是基于通用部件搭建的
12. `src/nls/**` - 存放国际化文件
13. `tests/unit/**` - 存放单元测试用例
14. `tests/functional/**` - 存放集成测试用例

## 需求描述

详见 [需求文档](./README.md)

## 数据库表结构

1. 设计数据库表时常用的[文档模板](../db/_TEMPLATE.md)；
2. [数据库表结构](../db/README.md)。

## REST API

详见 [API 文档](../API/README.md)

## 开发

BlockLang 使用 Spring boot 的 profile 功能，将环境分为开发环境、测试环境和生产环境。

### 开发环境

默认是开发环境，profile 的名称为 `dev`，运行 `com.blocklang.BlockLangApplication` 类中的 `main` 函数，即可运行。

### 测试环境

测试环境的 profile 为 `test` 和 `mysql`。默认为 `test`。

在 `test` 下运行测试用例，会在 postgresql 数据库上安装测试数据库；在 `mysql` 下运行测试用例，会在 mysql 数据库上安装数据库。

要在 `test` 和 `mysql` 之间切换，可修改 `com.blocklang.core.test.AbstractSpringTest` 中的 `@ActiveProfiles` 的值即可。

### 生产环境

生产环境的 profile 为 `prod`。要为生产环境发布软件，则先将 `application.yml` 中的 `spring.profiles.active` 的值改为 `prod`，然后运行 `mvn clean install` 命令。

## 安装

### 安装环境

1. 安装 JDK
2. 安装 git
3. 安装 nodeJs
4. 安装 yarn
5. 配置 xxx

### 安装 blocklang 软件

1. 进入 `client` 文件夹，执行 `npm run build` 生成发布文件（存在 `output/dist` 文件夹中）
2. 将 `client/output/dist` 中的文件复制到 `server/src/main/java/resources/static` 文件夹中
3. 将 `server/src/main/java/resources/static/index.html` 移到 `server/src/main/java/resources/templates` 文件夹中
4. 安装 PostgreSQL 数据库，并在其中创建名为 `blocklang` 的数据库（或使用 `server/src/main/java/resources/db/db.sql`）
5. 在 `server/src/main/java/resources/application.yml` 中配置数据库链接
6. 进入 `server` 文件夹，并运行 `mvn install` 生成 jar 文件
7. 在命令行运行 `java -jar xxx.jar` 命令来启动 jar 文件

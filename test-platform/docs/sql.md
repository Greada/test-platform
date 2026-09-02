erDiagram
    test_case ||--o{ execution_record : "执行"
    test_case ||--o{ test_suite_case : "属于"
    test_suite ||--o{ test_suite_case : "包含"
    test_suite ||--o{ execution_report : "产生"
    execution_report ||--o{ execution_record : "汇总"
    user ||--o{ test_case : "创建" (未实现)

    user {
        BIGINT id PK
        VARCHAR username UK "用户名"
        VARCHAR password "BCrypt 密码"
        VARCHAR display_name "显示名称"
        VARCHAR role "USER/ADMIN"
        TINYINT deleted "V3迁移-软删标记"
        DATETIME create_time
        DATETIME update_time "V3迁移-补齐"
    }

    test_case {
        BIGINT id PK
        VARCHAR test_no UK "测试编号 TC-001"
        VARCHAR name "用例名称"
        VARCHAR request_url "请求地址"
        VARCHAR request_method "GET/POST/PUT/DELETE"
        TEXT request_headers "JSON"
        TEXT request_params "JSON"
        TEXT expected_result "预期结果"
        BIGINT category_id "V3-所属分类"
        TINYINT deleted "V3迁移-软删标记 0/1"
        DATETIME create_time
        DATETIME update_time
    }

    execution_record {
        BIGINT id PK
        BIGINT test_case_id FK "关联用例"
        BIGINT report_id FK "V2.1新增-关联报告"
        VARCHAR test_no "V2.1新增-快照"
        VARCHAR case_name "V2.1新增-快照"
        BIGINT execute_duration "V2.1新增-耗时ms"
        VARCHAR status "PASS/FAIL/ERROR"
        TEXT request_detail
        TEXT response_detail
        TEXT actual_result
        DATETIME execute_time
    }

    test_suite {
        BIGINT id PK
        VARCHAR name "套件名称"
        VARCHAR description "描述"
        TINYINT deleted "V3迁移-软删标记"
        DATETIME create_time
        DATETIME update_time
    }

    test_suite_case {
        BIGINT id PK
        BIGINT suite_id FK "套件"
        BIGINT case_id FK "用例"
        INT sort_order "排序"
    }

    execution_report {
        BIGINT id PK
        BIGINT suite_id FK "关联套件"
        VARCHAR report_name "报告名称"
        INT total "总计"
        INT passed "通过"
        INT failed "失败"
        INT errored "错误"
        DECIMAL pass_rate "通过率%"
        VARCHAR status "RUNNING/COMPLETED"
        DATETIME execute_time "执行时间"
        DATETIME create_time
    }

    ci_build {
        BIGINT id PK
        INT build_number "Jenkins 构建编号"
        INT total_tests "总用例数"
        INT passed "通过数"
        INT failed "失败数"
        DECIMAL pass_rate "通过率%"
        VARCHAR status "SUCCESS/FAILURE"
        VARCHAR build_url "Jenkins 构建链接"
        DATETIME created_at
        DATETIME updated_at
    }

## 版本记录

| 版本 | 变更内容 |
|---|---|
| V1 | test_case + execution_record |
| V2.1 | 新增 test_suite/test_suite_case/execution_report，execution_record 加 4 字段 |
| V2.2 | 无 schema 变更 |
| V3 | 新增 test_category，test_case 加 category_id |
| V3.1 | 无 schema 变更（纯前后端 + AI 集成） |
| V3.2 | 新增 user 表 |
| V3.3 | 新增 ci_build 表（V4 迁移 init_v4.sql） |
| V4 | `test_suite_case` / `execution_record` 核心关系加 RESTRICT 外键（B2.7） |
| Docker | 合并 DDL 为 `docker/init/init.sql` 单入口（V1+V2+V3+种子数据+SET NAMES utf8mb4） |
| Flyway V1/V2 | db/migration 快照基线（8 表最终形态 + 种子数据；存量库自动 baseline） |
| Flyway V3 | 软删字段（test_case/test_suite/test_category/user）+ user/execution_report 补 update_time + 索引（test_case.category_id / ci_build.build_number / execution_record.execute_time） |
| Flyway V4 | 核心关系外键约束（test_suite_case.suite_id/case_id、execution_record.test_case_id/report_id → RESTRICT） |

## 当前 schema 管理（B2.1 起）

- **权威来源**：`backend/src/main/resources/db/migration/`（Flyway V1~V3）
- **新变更 = 新建 Vn 脚本**，禁改历史脚本（checksum 校验会拒启动）
- 软删已生效：上述 4 表 deleteById 实际为 `UPDATE deleted=1`，查询自动 `WHERE deleted=0`
- `sql/` 手工脚本组已降级为存量维护路径；`docker/init/init.sql` 的 ALTER 已折叠进 CREATE（B2.3，2026-09-01 实测可重入）
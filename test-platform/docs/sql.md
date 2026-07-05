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
        DATETIME create_time
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
| Docker | 合并 DDL 为 `docker/init/init.sql` 单入口（V1+V2+V3+种子数据+SET NAMES utf8mb4） |
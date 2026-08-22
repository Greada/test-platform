# Lesson 4: 参数化 + 双环境（parameters + when）

> 目标:让一条 Jenkinsfile 能同时服务「测试环境」和「生产环境」,通过参数选择部署目标,用 `when` 让 stage 按条件执行/跳过。
> 前置:Lesson 3 已完成 Deploy + Verify,learn pipeline 能跑通 hello→Test→Build→Deploy→Verify 五个 stage。

## 一、概念

### 4.1 为什么要参数化

L1-L3 的 learn pipeline 是"写死"的:
- Deploy 写死用 `docker-compose.learn.yml`
- Verify 写死访问 `:8090`(backend) 和 `:82`(frontend)
- 端口、compose 文件全硬编码在 sh 里

问题:一条 pipeline 只能部署一个环境。生产 Jenkinsfile 的解法是**双 compose + 参数切换**:

| 环境 | compose 文件 | 容器名前缀 | 端口(mysql/backend/frontend) |
|------|-------------|-----------|------------------------------|
| test(测试) | `docker-compose.test.yml` | `tp-test-*` | 3308 / 8081 / 81 |
| prod(生产) | `docker-compose.yml` | `tp-*` | 3307 / 8080 / 80 |

同一条 pipeline,根据参数选不同 compose → 部署到不同环境,互不干扰。

### 4.2 parameters{} — 声明式参数

```groovy
parameters {
    choice(name: 'DEPLOY_ENV', choices: ['auto', 'test', 'prod'],
           description: '部署环境:auto 根据分支判断,test 强制测试,prod 强制生产')
}
```

| 要素 | 作用 |
|------|------|
| `choice` | 下拉单选参数(用户点 Build with Parameters 时选) |
| `name` | 参数名,后续用 `params.DEPLOY_ENV` 或 `env.DEPLOY_ENV` 读 |
| `choices` | 选项列表,**第一个是默认值** |
| `description` | 提示文字 |

**三种参数类型对比:**

| 类型 | 语法 | 用途 |
|------|------|------|
| `choice` | `choices: ['a','b']` | 枚举单选(环境、模式) |
| `string` | `defaultValue: ''` | 自由文本(PR 号、SHA) |
| `booleanParam` | `defaultValue: false` | 开关(是否跳过测试) |

> ⚠️ 参数只在**手动触发(Build with Parameters)**或**API 触发带参数**时才有值。Crontab 自动触发用默认值。

### 4.3 when{} — 条件 stage

```groovy
stage('Deploy') {
    when { expression { env.IS_PR != 'true' } }
    steps { ... }
}
```

`when{}` 放在 `stage{}` 内、`steps{}` 前。条件为 false → **整个 stage 跳过**(Console 显示 "Stage skipped due to when condition"),不报错。

**常用 when 条件:**

| 条件 | 语法 | 判定 |
|------|------|------|
| 表达式 | `expression { env.X == 'Y' }` | 任意 Groovy 布尔表达式 |
| 分支 | `branch 'main'`` |`env.BRANCH_NAME == 'main'`(多分支流水线) |
| 环境 | `environment name: 'X', value: 'Y'` | env.X == 'Y' |

> L4 重点用 `expression{}`——它最灵活,能写任意判断逻辑。

### 4.4 auto 模式 — 分支推断环境

生产 Jenkinsfile 的 Resolve Env stage(Jenkinsfile:73-90)逻辑:

```groovy
if (params.DEPLOY_ENV == 'auto') {
    def branch = env.BRANCH_NAME ?: ''
    if (branch == 'main' || branch == '') {
        env.DEPLOY_TARGET = 'prod'
    } else {
        env.DEPLOY_TARGET = 'test'
    }
} else {
    env.DEPLOY_TARGET = params.DEPLOY_ENV
}
```

`auto` = 让 pipeline 自己判断:
- `main` 分支 → prod
- 其他分支(或无分支名,如普通 Job)→ test

**为什么 main → prod?** 主干分支 = 稳定代码 = 生产。特性分支 = 开发中 = 测试环境。

### 4.5 env vs params — 两个变量的区别

| 变量 | 来源 | 可变 | 作用域 |
|------|------|------|--------|
| `params.X` | parameters{} 声明,触发时传入 | ❌ 只读 | 整个 pipeline |
| `env.X` | `env.X = 'Y'` 赋值 | ✅ 可改 | 整个 pipeline(跨 stage 共享) |

**关键模式:** 把 `params` 读出来写到 `env`,后续 stage 用 `env`——因为 params 不可变,env 可在中间 stage(如 Resolve Env)重新计算。

```
params.DEPLOY_ENV='auto'  → Resolve Env stage → env.DEPLOY_TARGET='test'  → 后续 stage 用 env.DEPLOY_TARGET
```

### 4.6 双 compose 切换的核心模式

```groovy
sh '''
    COMPOSE_FILE="docker-compose.yml"
    [ "$DEPLOY_TARGET" = "test" ] && COMPOSE_FILE="docker-compose.test.yml"
    docker compose -f "$COMPOSE_FILE" up -d
'''
```

- 在 sh 里用 shell 变量 `COMPOSE_FILE` 选文件(不是 Groovy 变量)
- `[ "$X" = "test" ] && COMPOSE_FILE=...` 是 shell 的条件赋值惯用法
- `env.DEPLOY_TARGET` 会自动注入到 sh 环境变量(名为 `DEPLOY_TARGET`)

> ⚠️ Groovy 的 `env.X` 在 `sh` 里变成环境变量 `X`(去掉 `env.` 前缀)。这是 Groovy↔shell 的桥接约定。

## 二、渐进式小步

### 2.1 小步 4a — 加 parameters 骨架

**概念:**
- 在 pipeline 顶部加 `parameters{}` 块,声明 `DEPLOY_ENV` 参数
- 加一个打印 stage 验证参数能读到

**代码骨架:**
```groovy
parameters {
    choice(name: 'DEPLOY_ENV', choices: ['auto', 'test', 'prod'],
           description: '部署环境:auto/test/prod')
}

stages {
    stage('Print Params') {
        steps {
            echo "===== DEPLOY_ENV = ${params.DEPLOY_ENV} ====="
        }
    }
}
```

**预期:** ✅ SUCCESS,Console 打印参数值

---

### 2.2 小步 4b — Resolve Env stage

**概念:**
- 加 `Resolve Env` stage,把 `params.DEPLOY_ENV`(auto/test/prod)解析成具体的 `env.DEPLOY_TARGET`(test/prod)
- auto 模式按分支判断(learn pipeline 无分支,默认走 prod 或 test 二选一)

**代码骨架:**
```groovy
stage('Resolve Env') {
    steps {
        script {
            if (params.DEPLOY_ENV == 'auto') {
                env.DEPLOY_TARGET = 'test'   // learn 无分支,auto 默认 test
            } else {
                env.DEPLOY_TARGET = params.DEPLOY_ENV
            }
            echo "===== 部署目标: ${env.DEPLOY_TARGET} ====="
        }
    }
}
```

**预期:** ✅ SUCCESS,打印 "部署目标: test/prod"

---

### 2.3 小步 4c — when 条件 + compose 切换模式

**概念:**
- 把 Build/Deploy 里的硬编码 compose 文件改成按 `env.DEPLOY_TARGET` 切换的 shell 变量模式
- learn 只有一套 `docker-compose.learn.yml`,4c 用 **B 路径**：不新建第二套 compose，两分支同指一个文件，演示"切换模式"惯用法
- 加 `Notify` stage 用 `when` 演示条件 stage（test 跳过、prod 执行）

**选定 B 路径（已执行）：**
- 路径 A（端口偏移真双环境）太重，把精力引到 compose 端口规划，偏离 L4 核心知识点
- B 路径聚焦 `parameters{}` + `when{}` 语法，轻量；切换模式仍写入 Build/Deploy 的 sh（两分支同文件），语法演示完整
- 真双环境留到 L6（规范化）或专门 infra 课再补

## 三、最终 Jenkinsfile-learn 结构(Lesson 4 完成时)

```groovy
pipeline {
    agent any
    parameters {
        choice(name: 'DEPLOY_ENV', choices: ['auto', 'test', 'prod'], description: '...')
    }
    stages {
        stage('hello') { ... }
        stage('Print Params') { ... }
        stage('Resolve Env') { ... }
        stage('Test') { ... }
        stage('Build') {
            steps {
                dir("test-platform") {
                    sh '''
                        COMPOSE_FILE="docker-compose.learn.yml"
                        # [ "$DEPLOY_TARGET" = "test" ] && COMPOSE_FILE="..."
                        docker compose -f "$COMPOSE_FILE" build backend
                        ...
                    '''
                }
            }
        }
        stage('Deploy') { ... }
        stage('Verify') { ... }
        stage('Notify') {
            when { expression { env.DEPLOY_TARGET == 'prod' } }
            steps { echo 'prod 部署完成,发送通知' }
        }
    }
}
```

## 四、复盘

### 4a 复盘

- **构建编号**: 4a 首次跑（DEPLOY_ENV=prod）
- **状态**: ✅ SUCCESS（修了两个 bug 后）
- **踩的坑**:
  - Bug 1: `stages {` 应为 `steps {` — `stages` 是 stage 组嵌套用，不能直接放 step，Jenkins 加载直接报错
  - Bug 2: 单引号 `'...${params.DEPLOY_ENV}...'` — Groovy 单引号不插值，原样输出字面量；双引号才做 `${}` 插值
- **关键认知**:
  - `parameters{}` 生效后，Jenkins Job 从 "Build Now" 变成 "Build with Parameters"，出现下拉框
  - `choice` 第一个选项是默认值（auto）
  - `params.X` 是只读，整个 pipeline 可访问
  - Groovy 字符串：单引号=字面量，双引号=`${}` 插值

### 4b 复盘

- **构建编号**: 4b 跑两次（auto + prod）
- **状态**: ✅ SUCCESS（修了一轮 bug 后）
- **踩的坑**:
  - Bug: `env.DEPLOY_TARGET = "params.DEPLOY_ENV"` — 双引号但无 `${}`，仍是字面量字符串
  - 修复过程两轮：单引号 → 双引号（仍错）→ 去掉引号 `params.DEPLOY_ENV`（对）
  - **验证陷阱**：首次跑 `auto` 只走到 `if` 分支，没覆盖 bug 所在的 `else` 分支 → 假通过
  - 补跑 `prod` 走 `else` 分支才真验证 bug 修复 — 和 L3b 的"JVM 早就绪假通过"同构
- **关键认知**:
  - Groovy 双引号**只有遇到 `${}` 才插值**，无 `${}` 时双引号 == 单引号（都是字面量）
  - 读变量值：去引号 `params.DEPLOY_ENV` 或 加 `${}` 插值 `"${params.DEPLOY_ENV}"`
  - 给字面量字符串：`'test'`（加引号）
  - **验证覆盖**：if/else 两个分支都要跑到，否则绿灯可能是假通过
  - `env.X = 'Y'` 赋值后，后续 stage 跨 stage 共享；在 sh 里变 `$X`（去掉 `env.` 前缀）

### 4c 复盘

- **构建编号**: 4c 跑两次（test + prod）
- **状态**: ✅ SUCCESS（修了一个 bug 后）
- **踩的坑**:
  - Bug: Deploy stage 判断 `$COMPOSE_FILE` 而非 `$DEPLOY_TARGET` — 判断错了变量
  - **巧合通过陷阱**：因为 B 路径两个分支同指 `docker-compose.learn.yml`，即使判断错变量结果也恰好对
  - 和 L3b、4b 的 auto-only 跑同构 — 绿灯但逻辑错
- **关键认知**:
  - `when{}` 放在 `stage{}` 内、`steps{}` **之前**，顺序错了报错
  - `when` 条件 false → Console 显示 `Stage "Notify" skipped due to when conditional`，不报错
  - `when` 条件 true → stage 正常执行
  - `env.DEPLOY_TARGET` 在 sh 里是 `$DEPLOY_TARGET`（Groovy↔shell 桥接：去 `env.` 前缀）
  - shell 条件赋值惯用法：`[ "$X" = "test" ] && COMPOSE_FILE=...`
  - B 路径的价值：聚焦 parameters+when 语法，不偏题到 compose 端口规划
- **L4 总结**:
  - `parameters{}`: choice/string/booleanParam 三种参数类型
  - `when{}`: 条件 stage，expression{} 最灵活
  - `env.X`: 跨 stage 共享变量，sh 里变 `$X`
  - auto 模式: 按分支推断环境（learn 无分支默认 test）
  - 三次"假通过"教训（L3b / 4b-auto / 4c-bug）: 绿灯 ≠ 逻辑对，验证要覆盖所有分支

## 五、Console Output 关键片段

### 4a — Print Params（DEPLOY_ENV=prod）

```
[Pipeline] stage
[Pipeline] { (Print Params)
[Pipeline] echo
===== DEPLOY_ENV = prod =====
```

### 4b — Resolve Env（auto → test + prod → prod）

**auto 模式（走 if 分支）：**
```
===== 部署目标: test =====
===== DEPLOY_ENV = auto =====
```

**prod 模式（走 else 分支，覆盖 bug 修复处）：**
```
===== 部署目标: prod =====
===== DEPLOY_ENV = prod =====
```

### 4c — Notify（test 跳过 + prod 执行）

**test 模式（when false → skipped）：**
```
Stage "Notify" skipped due to when conditional
```

**prod 模式（when true → 执行）：**
```
===== prod 部署完成，发送通知 =====
```

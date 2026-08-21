# Lesson 6: 规范化（options + 镜像锁版本）

> 目标:让 pipeline 更健壮(防卡死/防并发冲突)+ 可重复(同样的代码=同样的产物)
> 前置:L0-L5 已跑通,Jenkinsfile-learn 当前是 L5 完成版

## 一、概念

### 6.1 为什么需要规范化

L1-L5 的 pipeline 能跑通,但缺两道工程化保护:

| 缺口 | 风险 | L6 解法 |
|------|------|---------|
| 无全局选项 | 并发构建端口冲突 / 日志无时间戳 / 构建历史塞满磁盘 / maven 卡死无超时 | `options{}` |
| 镜像浮动 tag | `:latest`/`:alpine` 滚动更新,今天绿灯明天同样的代码可能红 | 镜像锁版本 |

### 6.2 options{} — Pipeline 级全局选项

Declarative Pipeline 里 `options{}` 放在 `pipeline{}` 内、`stages{}` 前,配置**整条流水线**的行为。一次声明,全程生效。

```groovy
pipeline {
    agent any
    options { ... }        // ← 放这里(parameters 之前或之后都行)
    parameters { ... }
    stages { ... }
}
```

**常用 option(按价值排序):**

| option | 作用 | 解决什么问题 |
|--------|------|------------|
| `disableConcurrentBuilds()` | 禁止同 Job 并发构建 | learn 环境端口固定(8090/82/3309),两个构建同时 Deploy 会端口/容器名冲突 |
| `timestamps()` | 给每行 Console 加时间戳 | 看 Test stage 花了 5 分钟还是 16 秒,定位耗时 |
| `buildDiscarder(logRotator(numToKeepStr: '20'))` | 只留最近 20 次构建 | 避免磁盘被历史构建塞满(Jenkins 默认全留) |
| `timeout(time: 30, unit: 'MINUTES')` | 整条流水线 30 分钟超时 | 防止 maven 下载卡死导致 Job 永远挂着 |
| `retry(1)` | 失败自动重试 1 次 | 偶发网络问题(daocloud 单源拉镜像断流) |
| `ansiColor('xterm')` | 彩色日志 | 可读性,可选 |

> ⚠️ `disableConcurrentBuilds()` 对 learn 环境尤其重要:上次 Build 的 tp-learn-* 容器还在跑,新构建同时 Deploy 会因容器名冲突失败。

### 6.3 镜像锁版本 — 为什么 :latest/:alpine 危险

**当前浮动 tag 清单(这台 learn 文件里的):**

| 文件 | 镜像 | 当前 tag | 风险 |
|------|------|---------|------|
| Jenkinsfile-learn:112,121 | `curlimages/curl` | `:latest` | 🔴 完全浮动 |
| backend/Dockerfile-learn:15 | `eclipse-temurin` | `:17-jre-alpine` | 🟡 17 锁了,alpine 滚动 |
| frontend/Dockerfile-learn:2 | `node` | `:20-alpine` | 🟡 20 锁了,alpine 滚动 |
| frontend/Dockerfile-learn:11 | `nginx` | `:alpine` | 🔴 完全浮动 |
| docker-compose.learn.yml:3 | `mysql` | `:5.7` | ✅ 已锁 |
| Jenkinsfile-learn:62 | `maven` | `:3.9-eclipse-temurin-17` | ✅ 已锁 |
| backend/Dockerfile-learn:2 | `maven` | `:3.9-eclipse-temurin-17-alpine` | ✅ 已锁 |

**4 个浮动 tag 要锁**(2 个高危 latest + 2 个中危 alpine)。

**为什么危险:**

CI/CD 的核心原则是**可重复构建**——同样的代码 + 同样的镜像 = 同样的产物。浮动 tag 破坏这个原则:

```
今天构建:curl:latest = v8.7.0 → Verify 通过(HTTP 200)
明天构建:curl:latest 被更新到 v8.8.0(含 breaking change)
        → 同样的代码,Verify 可能红
        → 但你代码没变,排查到死都不知道为什么
```

`nginx:alpine` 更危险——alpine 版本跳了可能基础库变了,导致前端容器起不来。

**锁版本粒度选择:**

| 粒度 | 示例 | 优缺点 |
|------|------|--------|
| digest | `nginx@sha256:abc123...` | 最稳,但难维护(一串哈希) |
| 具体版本 tag | `nginx:1.25.4-alpine` | 实用,可读,够稳 |
| 大版本 tag | `nginx:1.25-alpine` | 折中,小版本自动更新 |

> L6c 策略:用「具体版本 tag」粒度,锁「当前本地已拉取的版本」(最安全,不引入新变量)。

## 二、渐进式小步

### 2.1 小步 6a — 加 options{} 骨架

**概念:**
- 在 pipeline{} 内、stages{} 前加 `options{}` 块
- 加 4 个基础 option:timestamps + disableConcurrentBuilds + buildDiscarder + timeout
- retry 留到 6b(造超时演练时一起验证)

**代码(已 push,commit 0c5f6d2):**

```groovy
pipeline {
    agent any

    // Lesson 6a: options{} — Pipeline 级全局选项
    // timestamps: 每行日志加时间戳,定位 stage 耗时
    // disableConcurrentBuilds: 禁并发,避免两个构建同时 Deploy 端口/容器名冲突
    // buildDiscarder: 只留最近 20 次构建,避免磁盘塞满
    // timeout: 整条流水线 30 分钟超时,防 maven 下载卡死
    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters { ... }
    stages { ... }
}
```

**验证点:**
- Console 每行前出现 `[2026-08-21 HH:MM:SS]` 时间戳(timestamps 生效)
- 触发后立即再点一次 Build,第二次应排队等待(disableConcurrentBuilds 生效)

**验证结果(2026-08-21,Build #5 + #6,均 SUCCESS):**
- ✅ `timestamps()` — 日志行带时间戳前缀(raw log 存 UTC,Web Console 渲染本地时区)
- ✅ `timeout(30)` — 流水线启动即打印 `Timeout set to expire in 30 min`(附带证明)
- ✅ `disableConcurrentBuilds()` — #5 于 11:27:50 结束,#6 于 11:27:51 启动(仅差 25ms):#6 全程在队列等待 #5 释放 executor,页面可见排队
- ➖ `buildDiscarder(20)` — 构建数仅 6(< 20),暂无可观察丢弃效果;配置随每次构建结束检查,属"静默生效"
- 时间线:#5 11:26:39→11:27:50(71.6s) / #6 11:27:51→11:29:00(69.7s)

---

### 2.2 小步 6b — 验证 timeout/retry

**概念:**
- 加 `retry(1)` 到 options{}
- 故意在 Verify stage 加 `sleep 600`(10 分钟)触发 30 分钟 timeout
- 观察 pipeline 是否中断 + retry 是否重试
- 验证完删掉 sleep 600 恢复正常

**代码骨架(待写):**
```groovy
options {
    ... // 6a 的 4 个
    retry(1)  // ← 6b 新增
}

stage('Verify') {
    steps {
        sh '''
            sleep 600  // ← 临时造超时,验证完删
            ...
        '''
    }
}
```

<!-- 待 6b 完成后回填:造超时实际表现 + retry 重试日志 -->

---

### 2.3 小步 6c — 镜像锁版本

**概念:**
- 查本地 4 个浮动镜像的 digest/具体版本
- 把 `:latest`/`:alpine` 锁成具体版本 tag
- 重新 Build 确认仍绿灯(HTTP 200)

**待锁清单:**
| 文件 | 当前 | 锁定为 | 状态 |
|------|------|--------|------|
| Jenkinsfile-learn:112,121 | `curlimages/curl:latest` | ? | 待查 digest |
| backend/Dockerfile-learn:15 | `eclipse-temurin:17-jre-alpine` | ? | 待查 digest |
| frontend/Dockerfile-learn:2 | `node:20-alpine` | ? | 待查 digest |
| frontend/Dockerfile-learn:11 | `nginx:alpine` | ? | 待查 digest |

<!-- 待 6c 完成后回填:每个镜像锁定的具体版本 + Build 验证结果 -->

## 三、最终 Jenkinsfile-learn 结构(L6 完成时)

<!-- 待 6c 完成后回填完整结构 -->

## 四、复盘

### 6a 复盘

- **状态**: ✅ 完成 — 代码 push(commit `0c5f6d2`)+ Build #5/#6 验证通过(2026-08-21)
- **已踩的坑:**
  - **Gitee hook 拒绝 push**:这台新电脑 git config 是 `kangqiao <kangqiao1@huawei.com>`(企业私有邮箱),Gitee "不公开邮箱" 设置让该 email 私有,push 时 hook 拦截(commit 历史会公开 email)
    - 报错:`remote: Push will publish a hidden email` + `hook declined`
    - 修复:改 `git config user.email` 为 Gitee 提供的 noreply email `9354122+greada@user.noreply.gitee.com`(user.name 一并改 `greada` 保持两台身份一致),`git commit --amend --reset-author --no-edit` 重写 author,重 push 成功(`98eccf0..0c5f6d2`)
  - **新电脑身份不匹配**:这台 git config 是 kangqiao(从别处带来),Gitee 账户是 greada。两台电脑 commit 身份应统一,改 user.name=greada
- **关键认知:**
  - Gitee hook 检查 commit author email 是否私有,不检查认证方式(改 SSH/https 没用,必须改 email)
  - noreply email 格式:`{数字ID}+{username}@user.noreply.gitee.com`
  - `git commit --amend --reset-author` 重写 author,--no-edit 保持原 commit message
  - commit 未 push 到远程时 amend 安全(无需 force push,fast-forward)
- **验证结论(2026-08-21 回填):**
  - timestamps() ✅ Console 出现 `[2026-08-21T03:27:02.584Z]` 前缀
  - disableConcurrentBuilds() ✅ #6 排队等待,#5 结束后 25ms 内启动(页面亲见排队 + build.xml 时间线双重证明)
  - 4 个 option 不影响正常流水线推进 ✅ #5/#6 全程绿灯(各约 70s)
- **新证据通道(本课新技能):** Jenkins API 有认证(403),但 WSL 内可 `docker exec jenkins sh -c '...'` 直读
  - `/var/jenkins_home/jobs/test-platform-learn/builds/N/build.xml` → result / startTime / duration / queueId
  - `/var/jenkins_home/jobs/test-platform-learn/builds/N/log` → 原始 Console(含 UTC 时间戳行)
  - 换算秒差即得排队衔接证据,比看页面截图更硬

## 五、Console Output 关键片段

**Build #5 — timestamps + timeout 生效(log 开头数行):**
```
[2026-08-21T03:27:02.584Z] Timeout set to expire in 30 min
[2026-08-21T03:27:02.675Z] hello
[2026-08-21T03:27:02.770Z] ===== 部署目标: test =====
```

**排队证据(build.xml 时间线,disableConcurrentBuilds 生效):**
```
#5 start: 2026-08-21 11:26:39 +08:00   queueId=9
#5 end:   2026-08-21 11:27:50 +08:00   (duration 71.6s, SUCCESS)
#6 start: 2026-08-21 11:27:51 +08:00   queueId=11  ← #5 结束后 25ms 启动,此前全程排队
#6 end:   2026-08-21 11:29:00 +08:00   (duration 69.7s, SUCCESS)
```

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-08-18 | 初版创建:概念(options + 镜像锁版本)+ 6a 代码已 push + Gitee hook 坑复盘;6b/6c 待回填 |
| 2026-08-21 | 6a 验证通过:Build #5/#6 双绿,timestamps/timeout/disableConcurrentBuilds 均有实证(排队 25ms 衔接);笔记回填 |

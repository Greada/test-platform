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
| `retry(2)` | 失败后从头重跑,共 2 次尝试 | 偶发网络问题(daocloud 单源拉镜像断流);⚠️ N=总尝试次数,retry(1) 只跑 1 次不重试(#7 实证) |
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
- 加 `retry(2)` 到 options{}(N=总尝试次数,retry(2)=失败重试 1 次;⚠️ retry(1) 只执行 1 次不重试,Build #7 实证)
- 临时把 timeout 改为 2 分钟,Verify 加 `sleep 600`(10 分钟)触发超时
- 观察 pipeline 是否中断 + retry 是否重试
- 验证完把 sleep 600 改回 sleep 15、timeout 改回 30(retry 保留)

**代码骨架(待写):**
```groovy
options {
    ... // 6a 的 4 个(6b 期间 timeout 临时 2 分钟)
    retry(2)  // ← 6b 新增(N=总尝试次数,retry(1) 不重试)
}

stage('Verify') {
    steps {
        sh '''
            sleep 600 # ← 临时造超时,验证完改回 sleep 15(sh 内注释必须用 #,不能用 //)
            ...
        '''
    }
}
```

<!-- 6b 已于 2026-08-21 完成,验证结果见"四、复盘 → 6b 复盘 → #8 验证结果" -->

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

### 6b 复盘(进行中)

- **状态**: 演练中 — Build #7 为无效实验轮(两处问题,数据仍记录如下)
- **已踩的坑:**
  - **sh 块内 Groovy 注释污染**(用户发现):`sleep 600 // 说明` 中 `//` 及后续文字被 shell 当成 sleep 参数 → `sleep: invalid time interval` 秒败,超时根本不触发
    - 证据(#7 log):`+ sleep 600 // 6b 临时...` 后连续 4 行 invalid time interval,duration 仅 42s
    - 规则:**sh '''...''' 内注释必须用 `#`;`//` 只在 Groovy 层有效**
  - **retry(1) 语义陷阱**:retry(N) 的 N=**总尝试次数**,retry(1) 只执行 1 次、失败不重试;要重试 1 次须 retry(2)
    - 证据(#7):「部署目标」echo 仅 1 次,`[Pipeline] retry` 开→闭之间单轮执行
    - 错误源头:本笔记 6.2 表格与 6b 骨架原写 retry(1),连同 Jenkinsfile-learn 注释已全部修正
  - **git push 被拒三连坑 → 根因已查明:git-ai 守护进程改写提交(2026-08-21 卸载)**
    - 现象:每次 commit 后 2-8 秒,HEAD 被**无标签 reflog 操作**顶替成同树/同消息/新 hash 的孪生提交 → push 必 non-fast-forward
    - 侦查链:reflog 空标签(=程序化 update-ref,非人工命令)→ 进程扫描发现 `git-ai.exe bg run` daemon → `.git-ai\bin\git.exe` 与 `git-ai.exe` SHA256 完全相同(PATH shim 劫持)→ opencode 插件 `~/.config/opencode/plugins/git-ai.ts` 是拉起源(每次 edit/bash 工具调用 spawn checkpoint)
    - ~~早先两个错误假设~~:「两个克隆各 commit 一次产生平行 hash」「误 amend 已推送提交」——都是 daemon 改写的表象,不是真相
    - 处置(Windows 侧全量卸载):杀 daemon + 删 opencode 插件 + 删 `.bash_profile`(安装器注入 2 行) + 清用户 PATH 注册表(保 ExpandString 与 `%DevEco Studio%`) + 删 `~/.git-ai` + 清仓库 `refs/notes/ai` 与 `.git/ai`(均未推远端,ls-remote 已验证)
    - 遗留:本会话内存中的插件实例仍会在每次工具调用时重建空日志目录 `~/.git-ai/logs`(仅日志,无 exe 无 daemon)——**关闭本次 opencode 会话后手动删一次即绝根**
    - WSL 侧 git-ai 按用户要求保留(检查时无进程运行,未动)
    - 通则仍有效:push 被拒不慌,先 `git fetch` + `git diff origin/main HEAD` ——**树 diff 为空才可安全丢弃本地 hash;绝不 force push**
- **待验证(retry(2) 重跑后回填):** → 已验证,见下方 #8 结论(2026-08-21)

**#8 验证结果(2026-08-21,retry(2) + timeout 2min + sleep 600):**
- ✅ 嵌套方向定论:**声明顺序 timeout→retry = timeout 包住 retry**(第一个声明的是最外层)
  - 证据:总时长 137.2s(≈2min17s,含 checkout),result=ABORTED,单轮执行,`[Pipeline] retry` 标记一次未出现
  - 推论:重试要拿到**独立时间预算**,必须把 `retry` 声明在 `timeout` **之前**(retry 包 timeout,每轮各 30 分钟)
- ✅ 超时中断链(三段式,log 原文):`Cancelling nested steps due to timeout` → `Sending interrupt signal to process` → `Timeout has been exceeded` ×1
- ✅ junit 收集 91 用例(7 suite)= 单轮;Test 早已完成,超时只杀 Verify 的 sleep
- 🎯 **超预期发现:超时终止 result = ABORTED ≠ FAILURE**
  - post 行为:#8 只执行了 `cleanup{}`(流水线开始清理),**`failure{}` 与 `success{}` 都没跑**
  - 生产含义:失败通知只写在 `post.failure{}` 的 pipeline,**超时场景会静默无通知**——须加 `post.aborted{}` 或把通知放 `always/cleanup`
- 📌 retry(2) 的"失败重试一次"仍未被直接验证(timeout 抢先把整条掐死)——留给后续真实失败场景,或 retry 挪到 timeout 外层再演练
- 时间线:#8 12:13:47 启动 → 12:14:27 sleep 600 开始 → 12:15:50 超时中断(83s 处)→ 12:16:04 ABORTED 收尾

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

**Build #8 — 超时中断链(三段式)+ ABORTED 收尾:**
```
[2026-08-21T04:13:50.587Z] Timeout set to expire in 2 min 0 sec
[2026-08-21T04:14:27.460Z] + sleep 600
[2026-08-21T04:15:50.588Z] Cancelling nested steps due to timeout
[2026-08-21T04:15:50.646Z] Sending interrupt signal to process
Timeout has been exceeded
Finished: ABORTED     ← 不是 FAILURE;post.failure/success 均未执行,仅 cleanup 兜底
```
- build.xml:duration 137214ms,result ABORTED;junitResult.xml:91 case / 7 suite(单轮)

## 变更日志

| 日期 | 变更 |
|------|------|
| 2026-08-18 | 初版创建:概念(options + 镜像锁版本)+ 6a 代码已 push + Gitee hook 坑复盘;6b/6c 待回填 |
| 2026-08-21 | 6a 验证通过:Build #5/#6 双绿,timestamps/timeout/disableConcurrentBuilds 均有实证(排队 25ms 衔接);笔记回填 |
| 2026-08-21 | 6b 演练中:#7 无效轮实证两坑(sh 内 //注释污染 / retry(1) 不重试)→ 注释与笔记源头修正,待 retry(2) 重跑 |
| 2026-08-21 | 6b #8 验证完成:timeout 包 retry(单轮 137.2s ABORTED)+ ABORTED≠FAILURE 的 post 陷阱;同日查明 git-ai 守护进程为 push 三连拒根因并卸载(Windows 侧) |

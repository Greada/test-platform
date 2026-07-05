<template>
    <div class="ci-page">
        <h2 class="page-title">CI/CD 构建状态</h2>

        <el-card v-if="latest" class="summary-card" shadow="never">
            <div class="stats-row">
                <div class="stat-box">
                    <div class="stat-num">{{ latest.totalTests }}</div>
                    <div class="stat-desc">总用例</div>
                </div>
                <div class="stat-box pass">
                    <div class="stat-num">{{ latest.passed }}</div>
                    <div class="stat-desc">通过</div>
                </div>
                <div class="stat-box fail">
                    <div class="stat-num">{{ latest.failed }}</div>
                    <div class="stat-desc">失败</div>
                </div>
                <div class="stat-box" :class="rateClass(latest.passRate)">
                    <div class="stat-num">{{ latest.passRate }}%</div>
                    <div class="stat-desc">通过率</div>
                </div>
            </div>
            <el-progress
                :percentage="Number(latest.passRate)"
                :color="rateColor(latest.passRate)"
                :stroke-width="16"
                striped
            />
            <div class="build-meta">
                <span>构建 #{{ latest.buildNumber }}</span>
                <el-tag
                    :type="latest.status === 'SUCCESS' ? 'success' : 'danger'"
                    size="small"
                    effect="dark"
                >
                    {{ latest.status }}
                </el-tag>
                <span class="meta-time">{{ latest.createdAt }}</span>
            </div>
        </el-card>

        <el-card v-if="!latest && !loading" shadow="never" class="empty-card">
            <el-empty description="暂无构建记录" />
        </el-card>

        <el-card shadow="never" class="history-card">
            <template #header>
                <span>构建历史</span>
            </template>
            <el-table
                v-loading="loading"
                :data="builds"
                stripe
                empty-text="暂无数据"
            >
                <el-table-column prop="buildNumber" label="构建 #" width="90" />
                <el-table-column prop="totalTests" label="用例数" width="80" align="center" />
                <el-table-column prop="passed" label="✅ 通过" width="80" align="center" />
                <el-table-column prop="failed" label="❌ 失败" width="80" align="center" />
                <el-table-column label="通过率" width="160">
                    <template #default="scope">
                        <el-progress
                            :percentage="Number(scope.row.passRate)"
                            :color="rateColor(scope.row.passRate)"
                            :stroke-width="12"
                        />
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100" align="center">
                    <template #default="scope">
                        <el-tag
                            :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'"
                            size="small" effect="plain"
                        >
                            {{ scope.row.status }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdAt" label="时间" width="170" />
            </el-table>
        </el-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getLatestBuild, getBuildList } from '../api/ci'

const latest = ref(null)
const builds = ref([])
const loading = ref(true)

function rateClass(rate) {
    const n = Number(rate)
    if (n >= 90) return 'pass'
    if (n >= 50) return 'warn'
    return 'fail'
}

function rateColor(rate) {
    const n = Number(rate)
    if (n >= 90) return '#67c23a'
    if (n >= 50) return '#e6a23c'
    return '#f56c6c'
}

onMounted(async () => {
    try {
        const [latestRes, listRes] = await Promise.all([
            getLatestBuild(),
            getBuildList()
        ])
        latest.value = latestRes.data
        builds.value = listRes.data
    } catch (e) {
        console.error('获取构建状态失败', e)
    } finally {
        loading.value = false
    }
})
</script>

<style scoped>
.ci-page {
    padding: 24px;
    max-width: 1000px;
    margin: 0 auto;
}
.page-title {
    margin: 0 0 20px;
    font-size: 20px;
    font-weight: 600;
}
.summary-card {
    margin-bottom: 20px;
}
.stats-row {
    display: flex;
    justify-content: space-around;
    margin-bottom: 24px;
}
.stat-box {
    text-align: center;
}
.stat-num {
    font-size: 34px;
    font-weight: 700;
    color: #409eff;
    line-height: 1.2;
}
.stat-desc {
    font-size: 13px;
    color: #999;
    margin-top: 4px;
}
.stat-box.pass .stat-num { color: #67c23a; }
.stat-box.fail .stat-num { color: #f56c6c; }
.stat-box.warn .stat-num { color: #e6a23c; }
.build-meta {
    display: flex;
    gap: 12px;
    align-items: center;
    margin-top: 14px;
    font-size: 13px;
    color: #999;
}
.meta-time { color: #999; }
.empty-card { margin-bottom: 20px; }
.history-card { margin-bottom: 20px; }
</style>

<template>
  <page-container title="Fabric诊断" description="支持基础连通性检测与链码函数执行，默认函数为 GetAllDevices">
    <n-card size="small" title="执行参数" :bordered="false">
      <n-form :model="form" label-placement="left" label-width="96">
        <n-form-item label="链码函数">
          <n-input v-model:value="form.function" placeholder="例如：GetAllDevices / ReadDevice" />
        </n-form-item>
        <n-form-item label="函数参数 args">
          <n-input
            v-model:value="form.argsText"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            placeholder="可选。支持按行或逗号分隔，例如：device001"
          />
        </n-form-item>
      </n-form>
      <n-space>
        <n-button type="primary" :loading="loading" @click="handleExecuteFunction">执行函数诊断</n-button>
        <n-button :loading="loading" @click="handleBasicTest">仅基础诊断</n-button>
        <n-button :disabled="loading" @click="handleUseDefault">使用默认函数</n-button>
        <n-button :disabled="loading" @click="handleReset">重置</n-button>
      </n-space>
    </n-card>

    <n-empty v-if="!loading && !result" description="尚未执行诊断" style="margin-top: 14px" />

    <n-spin :show="loading">
      <div v-if="result" class="result-wrap">
        <n-card size="small" title="诊断结果" :bordered="false">
          <template #header-extra>
            <n-button text type="primary" @click="rawVisible = true">查看原始结果</n-button>
          </template>
          <n-space align="center">
            <n-tag :type="result.success ? 'success' : 'error'" size="medium">
              {{ result.success ? '成功' : '失败' }}
            </n-tag>
            <span class="message-text">{{ result.message || '--' }}</span>
          </n-space>

          <n-descriptions bordered :column="2" size="small" style="margin-top: 12px">
            <n-descriptions-item label="Fabric启用">
              <n-tag :type="flagTag(result.enabled)">{{ flagText(result.enabled) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="Mock模式">
              <n-tag :type="result.mock ? 'warning' : 'success'">{{ flagText(result.mock) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="网关配置完整">
              <n-tag :type="flagTag(result.gatewayConfigReady)">{{ flagText(result.gatewayConfigReady) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="Peer TCP可达">
              <n-tag :type="flagTag(result.tcpReachable)">{{ flagText(result.tcpReachable) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="Gateway可用">
              <n-tag :type="flagTag(result.gatewayReady)">{{ flagText(result.gatewayReady) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="基础连通性通过">
              <n-tag :type="flagTag(result.basicConnectionSuccess)">{{ flagText(result.basicConnectionSuccess) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="Peer地址">{{ result.peerEndpoint || '--' }}</n-descriptions-item>
            <n-descriptions-item label="通道">{{ result.channelName || '--' }}</n-descriptions-item>
          </n-descriptions>
        </n-card>

        <n-card size="small" title="问题清单" :bordered="false" style="margin-top: 12px">
          <n-empty v-if="!result.issues?.length" description="未发现问题" />
          <pre v-else class="json-viewer issue-viewer">{{ issuesText }}</pre>
        </n-card>

        <n-card
          v-if="result.chaincodeInvocationRequested || result.evaluateResult || result.error"
          size="small"
          title="函数执行结果"
          :bordered="false"
          style="margin-top: 12px"
        >
          <n-descriptions bordered :column="1" size="small">
            <n-descriptions-item label="函数名">{{ result.function || '--' }}</n-descriptions-item>
            <n-descriptions-item label="args">{{ (result.args || []).join(', ') || '--' }}</n-descriptions-item>
            <n-descriptions-item label="执行成功">
              <n-tag :type="flagTag(result.chaincodeInvocationSuccess)">
                {{ flagText(result.chaincodeInvocationSuccess) }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="返回结果">
              <pre class="json-viewer issue-viewer">{{ formattedEvaluateResult }}</pre>
            </n-descriptions-item>
            <n-descriptions-item label="错误信息">{{ result.error || '--' }}</n-descriptions-item>
          </n-descriptions>
        </n-card>
      </div>
    </n-spin>

    <json-viewer-dialog v-model:show="rawVisible" title="Fabric诊断原始结果" :json-data="result" />
  </page-container>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useMessage } from 'naive-ui';
import { runFabricDiagnostic, type FabricDiagnosticResult } from '@/api/fabric';
import JsonViewerDialog from '@/components/JsonViewerDialog.vue';
import PageContainer from '@/components/PageContainer.vue';

const DEFAULT_FUNCTION = 'GetAllDevices';

const message = useMessage();
const loading = ref(false);
const rawVisible = ref(false);
const result = ref<FabricDiagnosticResult | null>(null);

const form = reactive({
  function: DEFAULT_FUNCTION,
  argsText: ''
});

const issuesText = computed(() => {
  if (!result.value?.issues?.length) {
    return '';
  }
  return result.value.issues.map((item, index) => `${index + 1}. ${item}`).join('\n');
});

const formattedEvaluateResult = computed(() => {
  const raw = result.value?.evaluateResult;
  if (!raw) {
    return '--';
  }
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
});

const parseArgs = () =>
  form.argsText
    .split(/[\r\n,]+/)
    .map((item) => item.trim())
    .filter((item) => item.length > 0);

const executeDiagnostic = async (withInvocation: boolean) => {
  loading.value = true;
  try {
    const functionName = form.function.trim();
    const payload = withInvocation
      ? {
          function: functionName,
          args: parseArgs()
        }
      : undefined;
    const res = await runFabricDiagnostic(payload);
    result.value = res.data;
    message.success(withInvocation ? '函数诊断执行完成' : '基础诊断执行完成');
  } catch (error: any) {
    message.error(error?.message || '诊断请求失败');
  } finally {
    loading.value = false;
  }
};

const handleBasicTest = () => {
  executeDiagnostic(false);
};

const handleExecuteFunction = () => {
  if (!form.function.trim()) {
    message.warning('请输入链码函数名');
    return;
  }
  executeDiagnostic(true);
};

const handleUseDefault = () => {
  form.function = DEFAULT_FUNCTION;
  form.argsText = '';
};

const handleReset = () => {
  handleUseDefault();
  result.value = null;
};

const flagText = (value: boolean) => (value ? '是' : '否');
const flagTag = (value: boolean) => (value ? 'success' : 'error');
</script>

<style scoped>
.result-wrap {
  margin-top: 14px;
}

.message-text {
  color: var(--text-secondary);
}

.issue-viewer {
  max-height: 260px;
}
</style>

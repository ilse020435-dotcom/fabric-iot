<template>
  <div class="login-page">
    <div class="glow glow-1"></div>
    <div class="glow glow-2"></div>

    <n-card class="login-card" :bordered="false">
      <h1 class="title">基于区块链的物联网设备生命周期管理系统</h1>
      <p class="subtitle">设备 DID 管理 · 状态摘要上链 · 审计全流程追踪</p>

      <n-form :model="formModel" :show-label="false" @submit.prevent="handleLogin">
        <n-form-item path="username">
          <n-input v-model:value="formModel.username" placeholder="请输入用户名" clearable />
        </n-form-item>

        <n-form-item path="password">
          <n-input
            v-model:value="formModel.password"
            type="password"
            show-password-on="mousedown"
            placeholder="请输入密码"
          />
        </n-form-item>

        <n-button type="primary" block :loading="loading" attr-type="submit" @click="handleLogin">
          登录系统
        </n-button>
      </n-form>

<!--      <p class="tips">推荐账号：admin / operator / vendor / regulator，默认密码：123456。</p>-->
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useMessage } from 'naive-ui';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const message = useMessage();
const userStore = useUserStore();

const loading = ref(false);

const formModel = reactive({
  username: undefined,
  password: undefined
});

const handleLogin = async () => {
  if (!formModel.username || !formModel.password) {
    message.warning('请输入用户名和密码');
    return;
  }

  loading.value = true;
  try {
    await userStore.login({ ...formModel });
    message.success('登录成功');
    router.push('/dashboard');
  } catch (error: any) {
    message.error(error?.message || '登录失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  background: var(--login-bg);
}

.glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(2px);
}

.glow-1 {
  width: 380px;
  height: 380px;
  background: var(--login-glow-1);
  left: -110px;
  top: -120px;
}

.glow-2 {
  width: 420px;
  height: 420px;
  background: var(--login-glow-2);
  right: -120px;
  bottom: -130px;
}

.login-card {
  width: 500px;
  border-radius: 16px;
  box-shadow: 0 16px 40px rgba(26, 73, 141, 0.2);
  background: var(--login-card-bg);
  position: relative;
  z-index: 2;
}

.title {
  margin: 4px 0;
  font-size: 24px;
  line-height: 1.4;
  color: var(--login-title);
}

.subtitle {
  margin-top: 4px;
  margin-bottom: 20px;
  color: var(--login-subtitle);
}

.tips {
  margin-top: 14px;
  color: var(--login-tip);
  font-size: 12px;
}

@media (max-width: 600px) {
  .login-card {
    width: 100%;
  }

  .title {
    font-size: 18px;
  }
}
</style>

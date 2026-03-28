import { defineStore } from 'pinia';
import { loginApi, logoutApi } from '@/api/auth';
import { clearToken, clearUser, getToken, getUser, setToken, setUser } from '@/utils/storage';

interface LoginPayload {
  username: string;
  password: string;
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    username: getUser()?.username || '',
    role: getUser()?.role || '',
    permissions: getUser()?.permissions || []
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(payload: LoginPayload) {
      const res = await loginApi(payload);
      this.token = res.data.token;
      this.username = res.data.username;
      this.role = res.data.role;
      this.permissions = res.data.permissions || [];
      setToken(this.token);
      setUser({ username: this.username, role: this.role, permissions: this.permissions });
    },
    async logout() {
      try {
        await logoutApi();
      } finally {
        this.token = '';
        this.username = '';
        this.role = '';
        this.permissions = [];
        clearToken();
        clearUser();
      }
    }
  }
});

import { defineStore } from 'pinia';

export const useAppStore = defineStore('app', {
  state: () => ({
    siderCollapsed: false,
    darkMode: false
  }),
  actions: {
    toggleSider() {
      this.siderCollapsed = !this.siderCollapsed;
    },
    setDarkMode(value: boolean) {
      this.darkMode = value;
    }
  }
});

import { defineStore } from 'pinia';

export const useDeviceFilterStore = defineStore('device-filter', {
  state: () => ({
    deviceId: '',
    deviceName: '',
    status: ''
  }),
  actions: {
    setFilter(payload: { deviceId: string; deviceName: string; status: string }) {
      this.deviceId = payload.deviceId;
      this.deviceName = payload.deviceName;
      this.status = payload.status;
    },
    resetFilter() {
      this.deviceId = '';
      this.deviceName = '';
      this.status = '';
    }
  }
});

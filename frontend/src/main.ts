import { createApp } from 'vue';
import { createPinia } from 'pinia';
import {
  create,
  NBreadcrumb,
  NBreadcrumbItem,
  NButton,
  NCard,
  NConfigProvider,
  NDataTable,
  NDatePicker,
  NDescriptions,
  NDescriptionsItem,
  NDialogProvider,
  NEmpty,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NIcon,
  NInput,
  NLayout,
  NLayoutContent,
  NLayoutHeader,
  NLayoutSider,
  NLoadingBarProvider,
  NMenu,
  NMessageProvider,
  NModal,
  NNotificationProvider,
  NSelect,
  NSpace,
  NSpin,
  NStatistic,
  NSwitch,
  NTag,
  NTree
} from 'naive-ui';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import App from './App.vue';
import { setupMock } from './mock';
import router from './router';
import './styles/global.css';

use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent
]);

if (import.meta.env.VITE_ENABLE_MOCK === 'true') {
  setupMock();
}

const naive = create({
  components: [
    NBreadcrumb,
    NBreadcrumbItem,
    NButton,
    NCard,
    NConfigProvider,
    NDataTable,
    NDatePicker,
    NDescriptions,
    NDescriptionsItem,
    NDialogProvider,
    NEmpty,
    NForm,
    NFormItem,
    NGrid,
    NGridItem,
    NIcon,
    NInput,
    NLayout,
    NLayoutContent,
    NLayoutHeader,
    NLayoutSider,
    NLoadingBarProvider,
    NMenu,
    NMessageProvider,
    NModal,
    NNotificationProvider,
    NSelect,
    NSpace,
    NSpin,
    NStatistic,
    NSwitch,
    NTag,
    NTree
  ]
});

const app = createApp(App);
app.use(createPinia());
app.use(naive);
app.use(router);
app.mount('#app');

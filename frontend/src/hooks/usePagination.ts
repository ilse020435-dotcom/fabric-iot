import { ref } from 'vue';

export function usePagination(defaultPageSize = 10) {
  const page = ref(1);
  const pageSize = ref(defaultPageSize);
  const itemCount = ref(0);

  const setTotal = (total: number) => {
    itemCount.value = total;
  };

  const reset = () => {
    page.value = 1;
  };

  return {
    page,
    pageSize,
    itemCount,
    setTotal,
    reset
  };
}

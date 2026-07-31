import request from '@/utils/request';

export function getStatisticsDashboard() {
  return request({
    url: '/statistics/dashboard',
    method: 'get',
  });
}

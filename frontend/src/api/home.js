import request from '@/utils/request';

export function getHomeDashboard() {
  return request({
    url: '/home/dashboard',
    method: 'get',
  });
}

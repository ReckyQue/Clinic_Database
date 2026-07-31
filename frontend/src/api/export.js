import request from '@/utils/request';

/** 按筛选条件智能导出（患者列表所见即所得） */
export function exportFiltered(data) {
  return request({
    url: '/export/filtered',
    method: 'post',
    data,
    responseType: 'blob',
    timeout: 60000,
  });
}

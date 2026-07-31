import request from '@/utils/request';

export function getDiagnoses(params) {
  return request({
    url: '/diagnoses',
    method: 'get',
    params,
  });
}

export function getDiagnosisStats() {
  return request({
    url: '/diagnoses/stats',
    method: 'get',
  });
}

export function getDiagnosisById(id) {
  return request({
    url: `/diagnoses/${id}`,
    method: 'get',
  });
}

export function createDiagnosis(data) {
  return request({
    url: '/diagnoses',
    method: 'post',
    data,
  });
}

export function updateDiagnosis(id, data) {
  return request({
    url: `/diagnoses/${id}`,
    method: 'put',
    data,
  });
}

export function deleteDiagnosis(id) {
  return request({
    url: `/diagnoses/${id}`,
    method: 'delete',
  });
}

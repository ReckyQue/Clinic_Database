import request from '@/utils/request';

export function getPatients(params) {
  return request({
    url: '/patients',
    method: 'get',
    params,
  });
}

export function getPatientById(id) {
  return request({
    url: `/patients/${id}`,
    method: 'get',
  });
}

export function createPatient(data) {
  return request({
    url: '/patients',
    method: 'post',
    data,
  });
}

export function updatePatient(id, data) {
  return request({
    url: `/patients/${id}`,
    method: 'put',
    data,
  });
}

export function deletePatient(id) {
  return request({
    url: `/patients/${id}`,
    method: 'delete',
  });
}

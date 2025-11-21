// src/services/orderService.js
import { consumerApi, producerApi } from './api';


export const getAllOrders = async ({ page = 0, size = 20, sortBy = 'processedAt', direction = 'DESC' }) => {
  const response = await consumerApi.get('/api/orders', {
    params: { page, size, sortBy, direction }
  });
  return response.data;
};


export const getOrderById = async (id) => {
  const response = await consumerApi.get(`/api/orders/${id}`);
  return response.data;
};


export const getOrderByOrderId = async (orderId) => {
  const response = await consumerApi.get(`/api/orders/order/${orderId}`);
  return response.data;
};


export const getOrdersByProduct = async (product) => {
  const response = await consumerApi.get(`/api/orders/product/${product}`);
  return response.data;
};


export const getOrdersByStatus = async (status, page = 0, size = 20) => {
  const response = await consumerApi.get(`/api/orders/status/${status}`, {
    params: { page, size }
  });
  return response.data;
};


export const getOrderStatistics = async () => {
  const response = await consumerApi.get('/api/orders/statistics');
  return response.data;
};


export const getProductStatistics = async () => {
  const response = await consumerApi.get('/api/orders/statistics/products');
  return response.data;
};


export const searchOrders = async ({ orderId, product, status }) => {
  const response = await consumerApi.get('/api/orders/search', {
    params: { orderId, product, status }
  });
  return response.data;
};


export const createOrder = async (orderData) => {
  const response = await producerApi.post('/order', orderData);
  return response.data;
};


export const createRandomOrder = async () => {
  const response = await producerApi.post('/order/random');
  return response.data;
};


export const getFailedOrders = async (status = null) => {
  const response = await consumerApi.get('/api/dlq/failed-orders', {
    params: status ? { status } : {}
  });
  return response.data;
};


export const getFailedOrdersByType = async (failureType) => {
  const response = await consumerApi.get(`/api/dlq/failed-orders/type/${failureType}`);
  return response.data;
};


export const getDlqStatistics = async () => {
  const response = await consumerApi.get('/api/dlq/statistics');
  return response.data;
};


export const reprocessOrder = async (id, reprocessedBy = 'admin') => {
  const response = await consumerApi.post(`/api/dlq/reprocess/${id}`, null, {
    params: { reprocessedBy }
  });
  return response.data;
};


export const reprocessAllPending = async (reprocessedBy = 'admin') => {
  const response = await consumerApi.post('/api/dlq/reprocess-all', null, {
    params: { reprocessedBy }
  });
  return response.data;
};


export const discardOrder = async (id) => {
  const response = await consumerApi.post(`/api/dlq/discard/${id}`);
  return response.data;
};


export const getFailedOrderById = async (id) => {
  const response = await consumerApi.get(`/api/dlq/failed-orders/${id}`);
  return response.data;
};


export const checkOrderHealth = async () => {
  const response = await consumerApi.get('/api/orders/health');
  return response.data;
};
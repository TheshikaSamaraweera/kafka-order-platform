// src/services/aggregationService.js
import { aggregationApi } from './api';


export const getProductStatistics = async (productName) => {
  const response = await aggregationApi.get(`/api/statistics/product/${productName}`);
  return response.data;
};


export const getAllStatistics = async () => {
  const response = await aggregationApi.get('/api/statistics/all');
  return response.data;
};


export const getSummaryStatistics = async () => {
  const response = await aggregationApi.get('/api/statistics/summary');
  return response.data;
};


export const checkAggregationHealth = async () => {
  const response = await aggregationApi.get('/api/statistics/health');
  return response.data;
};
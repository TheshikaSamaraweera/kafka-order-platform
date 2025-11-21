import axios from 'axios';


export const API_BASE_URL = 'http://localhost:8083'; 
export const PRODUCER_URL = 'http://localhost:8082'; 
export const AGGREGATION_URL = 'http://localhost:8084'; 

export const consumerApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const producerApi = axios.create({
  baseURL: PRODUCER_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const aggregationApi = axios.create({
  baseURL: AGGREGATION_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});


consumerApi.interceptors.request.use(
  (config) => {
    console.log(` ${config.method.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);


consumerApi.interceptors.response.use(
  (response) => {
    console.log(` ${response.config.method.toUpperCase()} ${response.config.url}`, response.data);
    return response;
  },
  (error) => {
    console.error(` ${error.config?.method?.toUpperCase()} ${error.config?.url}`, error.message);
    return Promise.reject(error);
  }
);


producerApi.interceptors.request.use(consumerApi.interceptors.request.handlers[0].fulfilled);
producerApi.interceptors.response.use(
  consumerApi.interceptors.response.handlers[0].fulfilled,
  consumerApi.interceptors.response.handlers[0].rejected
);

aggregationApi.interceptors.request.use(consumerApi.interceptors.request.handlers[0].fulfilled);
aggregationApi.interceptors.response.use(
  consumerApi.interceptors.response.handlers[0].fulfilled,
  consumerApi.interceptors.response.handlers[0].rejected
);
import { ApiResponse } from '../types/response.types';

export const successResponse = <T>(data: T, message?: string): ApiResponse<T> => ({
  success: true,
  message,
  data,
});

export const errorResponse = (message: string, errors?: any[]): ApiResponse => ({
  success: false,
  message,
  errors,
});

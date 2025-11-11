export interface ApiResponse<T = any> {
  success: boolean;
  message?: string;
  data?: T;
  errors?: any[];
}

export interface ErrorResponse {
  success: false;
  message: string;
  errors?: any[];
}

export interface SuccessResponse<T> {
  success: true;
  message?: string;
  data: T;
}

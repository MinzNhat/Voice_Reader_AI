import { Request, Response, NextFunction } from 'express';
import { AppError } from './error.middleware';

export const validateApiKey = (
  req: Request,
  res: Response,
  next: NextFunction
): void => {
  const apiKey = req.headers['x-api-key'] as string;
  const validApiKey = process.env.API_KEY;

  // Skip validation in development if no API key is set
  if (process.env.NODE_ENV === 'development' && !validApiKey) {
    return next();
  }

  if (!apiKey || apiKey !== validApiKey) {
    throw new AppError('Invalid or missing API key', 401);
  }

  next();
};

import { Router, Request, Response, NextFunction } from 'express';
import { pdfService } from '../services/pdf.service';
import { upload, deleteFile } from '../utils/storage';
import { successResponse } from '../utils/response';
import { AppError } from '../middleware/error.middleware';
import { pdfValidation } from '../utils/validation';

const router = Router();

/**
 * POST /api/pdf/extract
 * Extract text from a PDF file
 * 
 * @body pdf - PDF file
 * @body language - Language code (default: 'vi')
 * 
 * @returns { text: string, pages: number, metadata?: object }
 */
router.post(
  '/extract',
  upload.single('pdf'),
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      if (!req.file) {
        throw new AppError('No PDF file uploaded', 400);
      }

      if (req.file.mimetype !== 'application/pdf') {
        deleteFile(req.file.path);
        throw new AppError('File must be a PDF', 400);
      }

      const { error, value } = pdfValidation.extractText.validate(req.body);
      if (error) {
        deleteFile(req.file.path);
        throw new AppError('Validation error', 400, error.details);
      }

      const { language } = value;

      const result = await pdfService.extractText(req.file.path, { language });

      deleteFile(req.file.path);

      res.status(200).json(successResponse(result, 'Text extracted from PDF successfully'));
    } catch (error) {
      if (req.file) {
        deleteFile(req.file.path);
      }
      next(error);
    }
  }
);

/**
 * POST /api/pdf/extract-page
 * Extract text from a specific page of a PDF
 * 
 * @body pdf - PDF file
 * @body page - Page number (1-indexed)
 * @body language - Language code (default: 'vi')
 * 
 * @returns { text: string, page: number }
 */
router.post(
  '/extract-page',
  upload.single('pdf'),
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      if (!req.file) {
        throw new AppError('No PDF file uploaded', 400);
      }

      if (req.file.mimetype !== 'application/pdf') {
        deleteFile(req.file.path);
        throw new AppError('File must be a PDF', 400);
      }

      const page = parseInt(req.body.page);
      if (!page || page < 1) {
        deleteFile(req.file.path);
        throw new AppError('Invalid page number', 400);
      }

      const { error, value } = pdfValidation.extractText.validate(req.body);
      if (error) {
        deleteFile(req.file.path);
        throw new AppError('Validation error', 400, error.details);
      }

      const { language } = value;

      const result = await pdfService.extractPage(req.file.path, page, { language });

      deleteFile(req.file.path);

      res.status(200).json(successResponse(result, `Text extracted from page ${page} successfully`));
    } catch (error) {
      if (req.file) {
        deleteFile(req.file.path);
      }
      next(error);
    }
  }
);

/**
 * POST /api/pdf/metadata
 * Get PDF metadata
 * 
 * @body pdf - PDF file
 * 
 * @returns { pages: number, title?: string, author?: string, subject?: string, creator?: string, creationDate?: string }
 */
router.post(
  '/metadata',
  upload.single('pdf'),
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      if (!req.file) {
        throw new AppError('No PDF file uploaded', 400);
      }

      if (req.file.mimetype !== 'application/pdf') {
        deleteFile(req.file.path);
        throw new AppError('File must be a PDF', 400);
      }

      const result = await pdfService.getMetadata(req.file.path);

      deleteFile(req.file.path);

      res.status(200).json(successResponse(result, 'PDF metadata retrieved successfully'));
    } catch (error) {
      if (req.file) {
        deleteFile(req.file.path);
      }
      next(error);
    }
  }
);

export default router;

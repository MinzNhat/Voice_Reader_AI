import { Router, Request, Response, NextFunction } from 'express';
import { ocrService } from '../services/ocr.service';
import { upload, deleteFile } from '../utils/storage';
import { successResponse } from '../utils/response';
import { AppError } from '../middleware/error.middleware';
import { ocrValidation } from '../utils/validation';

const router = Router();

/**
 * POST /api/ocr/extract
 * Extract text from an image using OCR
 * 
 * @body image - Image file (JPEG, PNG)
 * @body language - Language code (default: 'vi')
 * @body engine - OCR engine ('google' or 'tesseract', default: 'google')
 * 
 * @returns { text: string, confidence?: number, language: string }
 */
router.post(
  '/extract',
  upload.single('image'),
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      if (!req.file) {
        throw new AppError('No image file uploaded', 400);
      }

      // Validate request body
      const { error, value } = ocrValidation.extractText.validate(req.body);
      if (error) {
        deleteFile(req.file.path);
        throw new AppError('Validation error', 400, error.details);
      }

      const { language, engine } = value;

      // Process OCR
      const result = await ocrService.extractText(req.file.path, { language, engine });

      // Cleanup uploaded file
      deleteFile(req.file.path);

      res.status(200).json(successResponse(result, 'Text extracted successfully'));
    } catch (error) {
      if (req.file) {
        deleteFile(req.file.path);
      }
      next(error);
    }
  }
);

/**
 * POST /api/ocr/batch
 * Extract text from multiple images
 * 
 * @body images - Array of image files
 * @body language - Language code (default: 'vi')
 * @body engine - OCR engine ('google' or 'tesseract', default: 'google')
 * 
 * @returns Array<{ filename: string, text: string, confidence?: number }>
 */
router.post(
  '/batch',
  upload.array('images', 10),
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const files = req.files as Express.Multer.File[];

      if (!files || files.length === 0) {
        throw new AppError('No image files uploaded', 400);
      }

      const { error, value } = ocrValidation.extractText.validate(req.body);
      if (error) {
        files.forEach(file => deleteFile(file.path));
        throw new AppError('Validation error', 400, error.details);
      }

      const { language, engine } = value;

      // Process all images
      const results = await Promise.all(
        files.map(async (file) => {
          try {
            const result = await ocrService.extractText(file.path, { language, engine });
            return {
              filename: file.originalname,
              ...result,
            };
          } catch (error) {
            return {
              filename: file.originalname,
              error: error instanceof Error ? error.message : 'Unknown error',
            };
          } finally {
            deleteFile(file.path);
          }
        })
      );

      res.status(200).json(successResponse(results, 'Batch OCR completed'));
    } catch (error) {
      if (req.files) {
        (req.files as Express.Multer.File[]).forEach(file => deleteFile(file.path));
      }
      next(error);
    }
  }
);

export default router;

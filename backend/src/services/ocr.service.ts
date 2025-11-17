import { ImageAnnotatorClient } from '@google-cloud/vision';
import { createWorker } from 'tesseract.js';
import { AppError } from '../middleware/error.middleware';
import { OcrOptions, OcrResult } from '../types/ocr.types';
import fs from 'fs';

class OcrService {
  private googleClient: ImageAnnotatorClient | null = null;

  constructor() {
    // Initialize Google Vision client if credentials are available
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      try {
        this.googleClient = new ImageAnnotatorClient();
      } catch (error) {
        console.warn('Google Vision client initialization failed:', error);
      }
    }
  }

  /**
   * Extract text from an image using the specified OCR engine
   */
  async extractText(imagePath: string, options: OcrOptions = {}): Promise<OcrResult> {
    const { language = 'vi', engine = 'google' } = options;

    if (!fs.existsSync(imagePath)) {
      throw new AppError('Image file not found', 404);
    }

    if (engine === 'google') {
      return this.extractTextGoogle(imagePath, language);
    } else {
      return this.extractTextTesseract(imagePath, language);
    }
  }

  /**
   * Extract text using Google Cloud Vision API
   */
  private async extractTextGoogle(imagePath: string, language: string): Promise<OcrResult> {
    if (!this.googleClient) {
      throw new AppError('Google Vision API is not configured. Please set GOOGLE_APPLICATION_CREDENTIALS.', 500);
    }

    try {
      const [result] = await this.googleClient.textDetection(imagePath);
      const detections = result.textAnnotations;

      if (!detections || detections.length === 0) {
        return {
          text: '',
          confidence: 0,
          language,
        };
      }

      // First annotation contains full text
      const text = detections[0].description || '';

      // Calculate average confidence from all detections
      const confidence = detections
        .slice(1)
        .reduce((sum, detection) => sum + (detection.confidence || 0), 0) / (detections.length - 1);

      return {
        text: text.trim(),
        confidence: Math.round(confidence * 100),
        language,
      };
    } catch (error) {
      console.error('Google Vision API error:', error);
      throw new AppError('Failed to extract text using Google Vision API', 500);
    }
  }

  /**
   * Extract text using Tesseract.js
   */
  private async extractTextTesseract(imagePath: string, language: string): Promise<OcrResult> {
    let worker = null;
    try {
      // Map language codes to Tesseract language codes
      const tesseractLang = this.mapLanguageToTesseract(language);

      worker = await createWorker(tesseractLang);
      const { data } = await worker.recognize(imagePath);
      await worker.terminate();

      return {
        text: data.text.trim(),
        confidence: Math.round(data.confidence),
        language,
      };
    } catch (error) {
      if (worker) {
        await worker.terminate();
      }
      console.error('Tesseract error:', error);
      throw new AppError('Failed to extract text using Tesseract', 500);
    }
  }

  /**
   * Map language codes to Tesseract language codes
   */
  private mapLanguageToTesseract(language: string): string {
    const languageMap: Record<string, string> = {
      vi: 'vie',
      en: 'eng',
      'en-US': 'eng',
      'vi-VN': 'vie',
    };

    return languageMap[language] || 'eng';
  }

  /**
   * Check if OCR service is available
   */
  isAvailable(): { google: boolean; tesseract: boolean } {
    return {
      google: this.googleClient !== null,
      tesseract: true, // Tesseract is always available
    };
  }
}

export const ocrService = new OcrService();

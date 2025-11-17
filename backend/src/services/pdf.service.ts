import pdf from 'pdf-parse';
import fs from 'fs';
import { AppError } from '../middleware/error.middleware';
import { PdfOptions, PdfResult, PageResult, MetadataResult } from '../types/pdf.types';

class PdfService {
  /**
   * Extract all text from a PDF file
   */
  async extractText(pdfPath: string, _options: PdfOptions = {}): Promise<PdfResult> {
    if (!fs.existsSync(pdfPath)) {
      throw new AppError('PDF file not found', 404);
    }

    try {
      const dataBuffer = fs.readFileSync(pdfPath);
      const data = await pdf(dataBuffer);

      return {
        text: data.text.trim(),
        pages: data.numpages,
        metadata: {
          title: data.info?.Title,
          author: data.info?.Author,
          subject: data.info?.Subject,
          creator: data.info?.Creator,
          creationDate: data.info?.CreationDate,
        },
      };
    } catch (error) {
      console.error('PDF parsing error:', error);
      throw new AppError('Failed to extract text from PDF', 500);
    }
  }

  /**
   * Extract text from a specific page
   */
  async extractPage(pdfPath: string, pageNumber: number, _options: PdfOptions = {}): Promise<PageResult> {
    if (!fs.existsSync(pdfPath)) {
      throw new AppError('PDF file not found', 404);
    }

    try {
      const dataBuffer = fs.readFileSync(pdfPath);
      const data = await pdf(dataBuffer, {
        max: pageNumber,
      });

      if (pageNumber > data.numpages) {
        throw new AppError(`Page ${pageNumber} does not exist. PDF has ${data.numpages} pages.`, 400);
      }

      // Extract text for the specific page
      // Note: pdf-parse doesn't support per-page extraction directly
      // This is a simplified implementation
      const pages = data.text.split('\f'); // Form feed character typically separates pages
      const pageText = pages[pageNumber - 1] || '';

      return {
        text: pageText.trim(),
        page: pageNumber,
      };
    } catch (error) {
      if (error instanceof AppError) {
        throw error;
      }
      console.error('PDF page extraction error:', error);
      throw new AppError('Failed to extract text from PDF page', 500);
    }
  }

  /**
   * Get PDF metadata
   */
  async getMetadata(pdfPath: string): Promise<MetadataResult> {
    if (!fs.existsSync(pdfPath)) {
      throw new AppError('PDF file not found', 404);
    }

    try {
      const dataBuffer = fs.readFileSync(pdfPath);
      const data = await pdf(dataBuffer);

      return {
        pages: data.numpages,
        title: data.info?.Title,
        author: data.info?.Author,
        subject: data.info?.Subject,
        creator: data.info?.Creator,
        creationDate: data.info?.CreationDate,
      };
    } catch (error) {
      console.error('PDF metadata extraction error:', error);
      throw new AppError('Failed to extract PDF metadata', 500);
    }
  }

  /**
   * Validate PDF file
   */
  async validatePdf(pdfPath: string): Promise<boolean> {
    try {
      const dataBuffer = fs.readFileSync(pdfPath);
      await pdf(dataBuffer);
      return true;
    } catch (error) {
      return false;
    }
  }

  /**
   * Extract text with page boundaries preserved
   */
  async extractTextWithPages(pdfPath: string, _options: PdfOptions = {}): Promise<{ pages: string[]; total: number }> {
    if (!fs.existsSync(pdfPath)) {
      throw new AppError('PDF file not found', 404);
    }

    try {
      const dataBuffer = fs.readFileSync(pdfPath);
      const data = await pdf(dataBuffer);

      // Split by form feed character
      const pages = data.text.split('\f').map((page: string) => page.trim());

      return {
        pages,
        total: data.numpages,
      };
    } catch (error) {
      console.error('PDF extraction error:', error);
      throw new AppError('Failed to extract text from PDF', 500);
    }
  }
}

export const pdfService = new PdfService();

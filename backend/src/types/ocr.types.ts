export interface OcrOptions {
  language?: string;
  engine?: 'google' | 'tesseract';
}

export interface OcrResult {
  text: string;
  confidence?: number;
  language: string;
}

export interface OcrRequest {
  language?: string;
  engine?: 'google' | 'tesseract';
}

export interface OcrResponse {
  text: string;
  confidence?: number;
  language: string;
}

export interface BatchOcrResult {
  filename: string;
  text?: string;
  confidence?: number;
  language?: string;
  error?: string;
}

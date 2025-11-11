export interface PdfOptions {
  language?: string;
}

export interface PdfResult {
  text: string;
  pages: number;
  metadata?: PdfMetadata;
}

export interface PdfMetadata {
  title?: string;
  author?: string;
  subject?: string;
  creator?: string;
  creationDate?: string;
}

export interface PageResult {
  text: string;
  page: number;
}

export interface MetadataResult {
  pages: number;
  title?: string;
  author?: string;
  subject?: string;
  creator?: string;
  creationDate?: string;
}

export interface PdfRequest {
  language?: string;
}

export interface PdfResponse {
  text: string;
  pages: number;
  metadata?: PdfMetadata;
}

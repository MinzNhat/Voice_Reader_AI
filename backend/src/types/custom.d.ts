// Lightweight module declarations to silence TS errors for untyped dependencies
declare module 'express';
declare module 'cors';
declare module 'helmet';
declare module 'compression';
declare module 'morgan';
declare module 'express-rate-limit';
declare module '@google-cloud/vision';
declare module 'tesseract.js';
declare module 'pdf-parse';
declare module '@google-cloud/text-to-speech';
declare module 'multer';
declare module 'uuid';
declare module 'joi';
declare module 'pdf-parse';

// Allow importing .js from node_modules without types
declare module '*';

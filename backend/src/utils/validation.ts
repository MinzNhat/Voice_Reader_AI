import Joi from 'joi';

export const ocrValidation = {
  extractText: Joi.object({
    language: Joi.string().default('vi'),
    engine: Joi.string().valid('google', 'tesseract').default('google'),
  }),
};

export const ttsValidation = {
  synthesize: Joi.object({
    text: Joi.string().required().min(1).max(5000),
    language: Joi.string().default('vi-VN'),
    voice: Joi.string().optional(),
    speed: Joi.number().min(0.25).max(4.0).default(1.0),
    pitch: Joi.number().min(-20.0).max(20.0).default(0),
    engine: Joi.string().valid('google', 'elevenlabs', 'openai').default('google'),
  }),
};

export const pdfValidation = {
  extractText: Joi.object({
    language: Joi.string().default('vi'),
  }),
};

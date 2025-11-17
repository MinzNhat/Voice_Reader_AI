import { Router, Request, Response, NextFunction } from 'express';
import { ttsService } from '../services/tts.service';
import { successResponse } from '../utils/response';
import { AppError } from '../middleware/error.middleware';
import { ttsValidation } from '../utils/validation';

const router = Router();

/**
 * POST /api/tts/synthesize
 * Convert text to speech
 * 
 * @body text - Text to convert (required, max 5000 characters)
 * @body language - Language code (default: 'vi-VN')
 * @body voice - Voice name (optional, engine-specific)
 * @body speed - Speech speed (0.25-4.0, default: 1.0)
 * @body pitch - Speech pitch (-20.0 to 20.0, default: 0)
 * @body engine - TTS engine ('google', 'elevenlabs', 'openai', default: 'google')
 * 
 * @returns { audioContent: string (base64), duration?: number }
 */
router.post(
  '/synthesize',
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { error, value } = ttsValidation.synthesize.validate(req.body);
      if (error) {
        throw new AppError('Validation error', 400, error.details);
      }

      const { text, language, voice, speed, pitch, engine } = value;

      const result = await ttsService.synthesize(text, {
        language,
        voice,
        speed,
        pitch,
        engine,
      });

      res.status(200).json(successResponse(result, 'Text synthesized successfully'));
    } catch (error) {
      next(error);
    }
  }
);

/**
 * POST /api/tts/stream
 * Convert text to speech with streaming response
 * 
 * @body text - Text to convert (required)
 * @body language - Language code (default: 'vi-VN')
 * @body voice - Voice name (optional)
 * @body speed - Speech speed (0.25-4.0, default: 1.0)
 * @body pitch - Speech pitch (-20.0 to 20.0, default: 0)
 * @body engine - TTS engine (default: 'google')
 * 
 * @returns Audio stream (audio/mp3)
 */
router.post(
  '/stream',
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const { error, value } = ttsValidation.synthesize.validate(req.body);
      if (error) {
        throw new AppError('Validation error', 400, error.details);
      }

      const { text, language, voice, speed, pitch, engine } = value;

      const audioBuffer = await ttsService.synthesizeStream(text, {
        language,
        voice,
        speed,
        pitch,
        engine,
      });

      res.setHeader('Content-Type', 'audio/mp3');
      res.setHeader('Content-Length', audioBuffer.length);
      res.send(audioBuffer);
    } catch (error) {
      next(error);
    }
  }
);

/**
 * GET /api/tts/voices
 * Get available voices for the specified language
 * 
 * @query language - Language code (optional, default: 'vi-VN')
 * @query engine - TTS engine (optional, default: 'google')
 * 
 * @returns Array<{ name: string, gender: string, language: string }>
 */
router.get(
  '/voices',
  async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    try {
      const language = (req.query.language as string) || 'vi-VN';
      const engine = (req.query.engine as string) || 'google';

      const voices = await ttsService.getVoices(language, engine);

      res.status(200).json(successResponse(voices, 'Voices retrieved successfully'));
    } catch (error) {
      next(error);
    }
  }
);

export default router;

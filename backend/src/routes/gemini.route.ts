import { Router, Request, Response, NextFunction } from 'express';
import { geminiService } from '../services/gemini.service';
import { successResponse } from '../utils/response';
import { AppError } from '../middleware/error.middleware';
import { geminiValidation } from '../utils/validation';

const router = Router();

/**
 * POST /api/gemini/summarize
 * Summarize provided content using Gemini API
 *
 * @body content - string (required)
 * @body model - optional model id (string)
 * @body maxTokens - optional max tokens for response (number)
 * @body length - optional summary length: 'short'|'medium'|'long'
 */
router.post(
    '/summarize',
    async (req: Request, res: Response, next: NextFunction): Promise<void> => {
        try {
            const { error, value } = geminiValidation.summarize.validate(req.body);
            if (error) {
                throw new AppError('Validation error', 400, error.details);
            }

            const { content, model, maxTokens, length } = value;

            const result = await geminiService.summarize(content, { model, maxTokens, length });

            res.status(200).json(successResponse(result, 'Content summarized successfully'));
        } catch (err) {
            next(err);
        }
    }
);

export default router;

import axios from 'axios';
import { AppError } from '../middleware/error.middleware';
import { GeminiOptions, GeminiPayload, GeminiResult } from '../types/gemini.types';

class GeminiService {
    private apiUrl: string;
    private defaultModel: string;
    private apiKey: string;

    private lengthToMaxTokens: Record<string, number> = {
        // Tăng giới hạn token cho 'short' để tránh lỗi MAX_TOKENS khi input lớn
        short: 512,
        medium: 1024,
        long: 2048,
    };

    constructor() {
        // Sử dụng URL API chung nếu GEMINI_API_URL không được định nghĩa
        const apiUrl = process.env.GEMINI_API_URL || 'https://generativelanguage.googleapis.com/v1';
        const defaultModel = process.env.GEMINI_DEFAULT_MODEL!;
        const apiKey = process.env.GEMINI_API_KEY!; // Lấy API Key từ ENV

        if (!apiUrl || !defaultModel || !apiKey) {
            console.warn('Gemini API environment not fully configured; service disabled.');
        }

        this.apiUrl = apiUrl;
        this.defaultModel = defaultModel;
        this.apiKey = apiKey;
    }

    async summarize(content: string, options: GeminiOptions = {}): Promise<GeminiResult> {
        if (!content || content.trim().length === 0) {
            throw new AppError('Content is required for summarization', 400);
        }

        if (!this.apiKey) {
            throw new AppError('Gemini API Key is not configured (GEMINI_API_KEY missing).', 500);
        }

        const model = options.model || this.defaultModel;
        const maxOutputTokens = options.length
            ? this.lengthToMaxTokens[options.length]
            : options.maxTokens || 2048;

        const baseUrl = `${this.apiUrl.replace(/\/+$/, '')}/models/${model}:generateContent`;
        const urlWithKey = `${baseUrl}?key=${this.apiKey}`;

        // ----------------------------------------------------
        // XÂY DỰNG PROMPT KÉP (DUAL PROMPTING) CHO DỮ LIỆU OCR
        // ----------------------------------------------------
        const systemInstruction = `
            You are an intelligent OCR Text Summarizer specializing in mobile screen capture analysis. The content below was extracted from a screenshot and may include:
            1. OCR errors (typos, bad line breaks).
            2. UI/Navigation artifacts (menu names, app names, buttons, timestamp, navigation bar text).

            Your task is three-fold:
            1. **Filter and Clean:** Identify and ignore text clearly belonging to UI elements (e.g., 'Settings', 'Back', '10:30 AM', app names, navigation titles).
            2. **Reconstruct:** Correct OCR errors and restore the logical flow of the main body text.
            3. **Summarize:** Summarize the clean, reconstructed main body text concisely, aiming for a length suitable for the specified maximum tokens.

            Output ONLY the final summary text, without any introductory phrases, commentary, or Markdown formatting.
        `;

        const finalContent = `${systemInstruction}\n\n--- TEXT TO SUMMARIZE ---\n${content}`;

        // ----------------------------------------------------

        const payload: GeminiPayload = {
            contents: [{ parts: [{ text: finalContent }] }],
            generationConfig: { maxOutputTokens },
        };

        try {
            const headers = {
                'Content-Type': 'application/json',
            };

            const resp = await axios.post(urlWithKey, payload, { headers, timeout: 60_000 });

            const data = resp.data || {};
            let summary = '';
            let tokensUsed: number | undefined;

            // 1. Trích xuất Văn bản tóm tắt một cách mạnh mẽ
            summary = data.candidates?.[0]?.content?.parts?.[0]?.text || '';

            // 2. Trích xuất Token sử dụng an toàn
            if (data.usageMetadata?.totalTokenCount) {
                tokensUsed = data.usageMetadata.totalTokenCount;
            }

            // 3. Kiểm tra các lý do dừng không mong muốn (Safety, Max Tokens, v.v.)
            if (!summary && data.candidates?.[0]?.finishReason) {
                const candidate = data.candidates[0];
                const reason = candidate.finishReason;

                if (reason === 'SAFETY' || reason === 'RECITATION' || reason === 'BLOCK') {
                    // Xử lý lỗi chặn an toàn
                    const safetyMessage = candidate.safetyRatings?.[0]?.category || 'Content blocked by safety filter.';
                    throw new AppError(
                        `Generation blocked (Reason: ${reason}). Details: ${safetyMessage}`,
                        500
                    );
                }
            }

            // 4. Kiểm tra cuối cùng: Nếu không có văn bản nào được trích xuất
            if (!summary || summary.trim().length === 0) {
                const finishReason = data.candidates?.[0]?.finishReason || 'Unknown';
                const apiErrorMsg = data?.error?.message || `Gemini API returned an empty text response. Finish Reason: ${finishReason}`;

                throw new AppError(
                    'Failed to summarize content via Gemini: API returned no readable text.',
                    500,
                    [apiErrorMsg]
                );
            }

            return { summary, model, tokensUsed };
        } catch (error: any) {
            console.error('Gemini API error:', error?.response?.data || error.message);

            const apiErrorMsg = error?.response?.data?.error?.message || error?.message;

            throw new AppError(
                'Failed to summarize content via Gemini',
                500,
                [apiErrorMsg]
            );
        }
    }
}

export const geminiService = new GeminiService();
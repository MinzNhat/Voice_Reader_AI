export type SummaryLength = 'short' | 'medium' | 'long';

export interface GeminiOptions {
    model?: string;
    maxTokens?: number;
    length?: SummaryLength;
}

export interface GeminiResult {
    summary: string;
    model: string;
    tokensUsed?: number;
}

export interface GeminiRequest {
    content: string;
    model?: string;
    maxTokens?: number;
    length?: SummaryLength;
}

export interface GeminiPayload {
    contents: Array<{
        parts: Array<{
            text: string;
        }>;
    }>;
    generationConfig: {
        maxOutputTokens: number;
    };
}

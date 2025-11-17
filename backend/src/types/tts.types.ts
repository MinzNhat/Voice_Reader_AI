export interface TtsOptions {
  language?: string;
  voice?: string;
  speed?: number;
  pitch?: number;
  engine?: 'google' | 'elevenlabs' | 'openai';
}

export interface TtsResult {
  audioContent: string; // Base64 encoded audio
  duration?: number;
}

export interface TtsRequest {
  text: string;
  language?: string;
  voice?: string;
  speed?: number;
  pitch?: number;
  engine?: 'google' | 'elevenlabs' | 'openai';
}

export interface TtsResponse {
  audioContent: string; // Base64 encoded
  duration?: number;
}

export interface Voice {
  name: string;
  gender: string;
  language: string;
}

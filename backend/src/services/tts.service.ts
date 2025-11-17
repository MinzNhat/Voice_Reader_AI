import { TextToSpeechClient } from '@google-cloud/text-to-speech';
import axios from 'axios';
import { AppError } from '../middleware/error.middleware';
import { TtsOptions, TtsResult, Voice } from '../types/tts.types';

class TtsService {
  private googleClient: TextToSpeechClient | null = null;

  constructor() {
    // Initialize Google TTS client if credentials are available
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      try {
        this.googleClient = new TextToSpeechClient();
      } catch (error) {
        console.warn('Google TTS client initialization failed:', error);
      }
    }
  }

  /**
   * Convert text to speech
   */
  async synthesize(text: string, options: TtsOptions = {}): Promise<TtsResult> {
    const { engine = 'google' } = options;

    if (!text || text.trim().length === 0) {
      throw new AppError('Text is required', 400);
    }

    switch (engine) {
      case 'google':
        return this.synthesizeGoogle(text, options);
      case 'elevenlabs':
        return this.synthesizeElevenLabs(text, options);
      case 'openai':
        return this.synthesizeOpenAI(text, options);
      default:
        throw new AppError('Invalid TTS engine', 400);
    }
  }

  /**
   * Convert text to speech with streaming (returns Buffer)
   */
  async synthesizeStream(text: string, options: TtsOptions = {}): Promise<Buffer> {
    const result = await this.synthesize(text, options);
    return Buffer.from(result.audioContent, 'base64');
  }

  /**
   * Google Cloud Text-to-Speech
   */
  private async synthesizeGoogle(text: string, options: TtsOptions): Promise<TtsResult> {
    if (!this.googleClient) {
      throw new AppError('Google TTS is not configured. Please set GOOGLE_APPLICATION_CREDENTIALS.', 500);
    }

    const {
      language = 'vi-VN',
      voice = 'vi-VN-Standard-A',
      speed = 1.0,
      pitch = 0,
    } = options;

    try {
      const request = {
        input: { text },
        voice: {
          languageCode: language,
          name: voice,
        },
        audioConfig: {
          audioEncoding: 'MP3' as const,
          speakingRate: speed,
          pitch,
        },
      };

      const [response] = await this.googleClient.synthesizeSpeech(request);

      if (!response.audioContent) {
        throw new AppError('No audio content returned from Google TTS', 500);
      }

      return {
        audioContent: Buffer.from(response.audioContent).toString('base64'),
      };
    } catch (error) {
      console.error('Google TTS error:', error);
      throw new AppError('Failed to synthesize speech using Google TTS', 500);
    }
  }

  /**
   * ElevenLabs Text-to-Speech
   */
  private async synthesizeElevenLabs(text: string, options: TtsOptions): Promise<TtsResult> {
    const apiKey = process.env.ELEVENLABS_API_KEY;
    if (!apiKey) {
      throw new AppError('ElevenLabs API key is not configured', 500);
    }

    const { voice = 'Rachel', speed = 1.0 } = options;

    try {
      const response = await axios.post(
        `https://api.elevenlabs.io/v1/text-to-speech/${voice}`,
        {
          text,
          model_id: 'eleven_multilingual_v2',
          voice_settings: {
            stability: 0.5,
            similarity_boost: 0.75,
            speed,
          },
        },
        {
          headers: {
            'xi-api-key': apiKey,
            'Content-Type': 'application/json',
          },
          responseType: 'arraybuffer',
        }
      );

      return {
        audioContent: Buffer.from(response.data).toString('base64'),
      };
    } catch (error) {
      console.error('ElevenLabs error:', error);
      throw new AppError('Failed to synthesize speech using ElevenLabs', 500);
    }
  }

  /**
   * OpenAI Text-to-Speech
   */
  private async synthesizeOpenAI(text: string, options: TtsOptions): Promise<TtsResult> {
    const apiKey = process.env.OPENAI_API_KEY;
    if (!apiKey) {
      throw new AppError('OpenAI API key is not configured', 500);
    }

    const { voice = 'alloy', speed = 1.0 } = options;

    try {
      const response = await axios.post(
        'https://api.openai.com/v1/audio/speech',
        {
          model: 'tts-1',
          input: text,
          voice,
          speed,
        },
        {
          headers: {
            Authorization: `Bearer ${apiKey}`,
            'Content-Type': 'application/json',
          },
          responseType: 'arraybuffer',
        }
      );

      return {
        audioContent: Buffer.from(response.data).toString('base64'),
      };
    } catch (error) {
      console.error('OpenAI TTS error:', error);
      throw new AppError('Failed to synthesize speech using OpenAI', 500);
    }
  }

  /**
   * Get available voices
   */
  async getVoices(language: string, engine: string): Promise<Voice[]> {
    if (engine === 'google' && this.googleClient) {
      try {
        const [response] = await this.googleClient.listVoices({ languageCode: language });
        return (
          response.voices?.map((voice: any) => ({
            name: voice.name || '',
            gender: voice.ssmlGender || 'NEUTRAL',
            language: voice.languageCodes?.[0] || language,
          })) || []
        );
      } catch (error) {
        console.error('Error fetching Google voices:', error);
        return [];
      }
    }

    // Default voices for other engines
    return this.getDefaultVoices(language);
  }

  /**
   * Get default voices list
   */
  private getDefaultVoices(language: string): Voice[] {
    const defaultVoices: Record<string, Voice[]> = {
      'vi-VN': [
        { name: 'vi-VN-Standard-A', gender: 'FEMALE', language: 'vi-VN' },
        { name: 'vi-VN-Standard-B', gender: 'MALE', language: 'vi-VN' },
        { name: 'vi-VN-Standard-C', gender: 'FEMALE', language: 'vi-VN' },
        { name: 'vi-VN-Standard-D', gender: 'MALE', language: 'vi-VN' },
      ],
      'en-US': [
        { name: 'en-US-Standard-A', gender: 'MALE', language: 'en-US' },
        { name: 'en-US-Standard-B', gender: 'MALE', language: 'en-US' },
        { name: 'en-US-Standard-C', gender: 'FEMALE', language: 'en-US' },
        { name: 'en-US-Standard-D', gender: 'MALE', language: 'en-US' },
      ],
    };

    return defaultVoices[language] || defaultVoices['en-US'];
  }

  /**
   * Check if TTS service is available
   */
  isAvailable(): { google: boolean; elevenlabs: boolean; openai: boolean } {
    return {
      google: this.googleClient !== null,
      elevenlabs: !!process.env.ELEVENLABS_API_KEY,
      openai: !!process.env.OPENAI_API_KEY,
    };
  }
}

export const ttsService = new TtsService();

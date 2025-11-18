package com.example.voicereaderapp.domain.model

/**
 * Supported languages for TTS
 */
enum class TTSLanguage(val code: String, val displayName: String) {
    KOREAN("ko-KR", "Korean"),
    ENGLISH("en-US", "English");

    companion object {
        fun fromCode(code: String): TTSLanguage {
            return values().find { it.code == code } ?: ENGLISH
        }
    }
}

/**
 * Available TTS voices
 */
enum class TTSVoice(
    val id: String,
    val displayName: String,
    val language: TTSLanguage,
    val gender: VoiceGender
) {
    // Korean Voices
    KOREAN_MINSEO("nminseo", "Minseo", TTSLanguage.KOREAN, VoiceGender.FEMALE),
    KOREAN_SHASHA("nshasha", "Shasha", TTSLanguage.KOREAN, VoiceGender.FEMALE),
    KOREAN_MOVIE_CHOI("nyounghwa", "Movie Choi", TTSLanguage.KOREAN, VoiceGender.MALE),
    KOREAN_MAMMOM("nmammon", "Mammom the Devil", TTSLanguage.KOREAN, VoiceGender.MALE),

    // English Voices
    ENGLISH_ANNA("danna", "Anna", TTSLanguage.ENGLISH, VoiceGender.FEMALE),
    ENGLISH_CLARA("clara", "Clara", TTSLanguage.ENGLISH, VoiceGender.FEMALE),
    ENGLISH_MATT("matt", "Matt", TTSLanguage.ENGLISH, VoiceGender.MALE);

    companion object {
        fun fromId(id: String): TTSVoice? {
            return values().find { it.id == id }
        }

        fun getVoicesForLanguage(language: TTSLanguage): List<TTSVoice> {
            return values().filter { it.language == language }
        }

        fun getDefaultVoiceForLanguage(language: TTSLanguage): TTSVoice {
            return when (language) {
                TTSLanguage.KOREAN -> KOREAN_MINSEO
                TTSLanguage.ENGLISH -> ENGLISH_MATT
            }
        }
    }
}

/**
 * Voice gender classification
 */
enum class VoiceGender(val displayName: String) {
    FEMALE("Female"),
    MALE("Male")
}

/**
 * Voice configuration for a document or global settings
 */
data class VoiceConfiguration(
    val language: TTSLanguage = TTSLanguage.ENGLISH,
    val voice: TTSVoice = TTSVoice.ENGLISH_MATT,
    val speed: Float = 1.0f
) {
    companion object {
        fun default(): VoiceConfiguration {
            return VoiceConfiguration(
                language = TTSLanguage.ENGLISH,
                voice = TTSVoice.ENGLISH_MATT,
                speed = 1.0f
            )
        }

        fun fromLanguage(language: TTSLanguage): VoiceConfiguration {
            return VoiceConfiguration(
                language = language,
                voice = TTSVoice.getDefaultVoiceForLanguage(language),
                speed = 1.0f
            )
        }
    }
}

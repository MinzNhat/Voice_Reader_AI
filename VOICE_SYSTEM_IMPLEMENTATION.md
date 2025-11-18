# Voice Selection System Implementation

## Overview
This document describes the implementation of a comprehensive voice selection system for the Voice Reader AI application. The system supports **Korean** and **English** languages with multiple voice options, allowing users to select voices both globally (in settings) and per-document (in the PDF viewer).

## Features Implemented

### 1. Supported Languages and Voices

#### Korean (ko-KR)
- **Female Voices**:
  - **Minseo** (Default) - `minseo`
  - **Shasha** - `shasha`
- **Male Voices**:
  - **Movie Choi** - `movie_choi`
  - **Mammom the Devil** - `mammom`

#### English (en-US)
- **Female Voices**:
  - **Anna** - `anna`
  - **Clara** - `clara`
- **Male Voice**:
  - **Matt** (Default) - `matt`

### 2. Voice Selection Levels

#### Global Settings
- Users can select a default voice and language in the Settings screen
- This becomes the default for all new documents
- Changes here don't affect existing documents with their own voice settings

#### Per-Document Settings (PDF Viewer)
- Users can select a different voice for each specific document
- Voice and speed settings are saved with the document
- When reopening a document, it uses its saved voice settings
- If no document-specific settings exist, it uses global settings

### 3. Voice Persistence

The voice selection system maintains two levels of persistence:

1. **Global Settings** (stored in DataStore preferences):
   - `voiceId`: Selected voice ID (default: "matt")
   - `language`: Language code (default: "en-US")
   - `speed`: Playback speed (default: 1.0f)
   - `pitch`: Voice pitch (default: 1.0f)

2. **Per-Document Settings** (stored in Room database):
   - Each document can have its own:
     - `voiceId`: Specific voice for this document
     - `language`: Specific language for this document
     - `speed`: Specific playback speed for this document
   - These fields are nullable; null means "use global settings"

## Technical Implementation

### New Files Created

1. **`domain/model/Voice.kt`**
   - `TTSLanguage` enum: Korean and English languages
   - `TTSVoice` enum: All 7 available voices with metadata
   - `VoiceGender` enum: Female/Male classification
   - `VoiceConfiguration` data class: Voice configuration wrapper

2. **`ui/common/VoiceSelectionDialog.kt`**
   - Reusable dialog component for voice selection
   - Shows language selection with FilterChips
   - Lists voices grouped by gender (Female/Male)
   - Used in global settings

3. **`data/local/database/Migrations.kt`**
   - Database migration from version 1 to 2
   - Adds three new columns to the documents table:
     - `voiceId TEXT`
     - `language TEXT`
     - `speed REAL`

### Modified Files

1. **`domain/model/ReadingDocument.kt`**
   - Added: `voiceId`, `language`, `speed` fields (nullable)

2. **`data/local/entity/DocumentEntity.kt`**
   - Added: `voiceId`, `language`, `speed` columns (nullable)
   - Updated: `toDomain()` and `toEntity()` extension functions

3. **`data/local/database/VoiceReaderDatabase.kt`**
   - Updated: Database version from 1 to 2

4. **`di/DatabaseModule.kt`**
   - Added: Migration support (MIGRATION_1_2)

5. **`domain/model/VoiceSettings.kt`**
   - Updated: Default values (matt, en-US)
   - Added: Helper methods for defaults

6. **`data/local/preferences/VoiceSettingsPreferences.kt`**
   - Updated: Default values to match new system

7. **`ui/settings/SettingsViewModel.kt`**
   - Added: `updateVoiceAndLanguage()` method
   - Added: `updateLanguage()` method

8. **`ui/settings/SettingsScreen.kt`**
   - Added: Voice selection card with VoiceSelectionDialog
   - Shows: Current voice and language
   - Updated: UI to display language-aware voice information

9. **`ui/pdfreader/PDFViewerViewModel.kt`**
   - Added: `selectedLanguage` to UI state
   - Added: `setVoiceAndLanguage()` method
   - Updated: `loadSavedDocument()` to load voice settings
   - Updated: `saveDocumentAfterOCR()` to save voice settings
   - Updated: `setPlaybackSpeed()` to persist to database
   - Added: Dependency on `GetVoiceSettingsUseCase`
   - Added: Init block to load global settings as defaults

10. **`ui/pdfreader/SpeakerDialog.kt`**
    - Complete rewrite using new TTSVoice enum
    - Added: Language selection chips
    - Updated: Callback signature to include language
    - Deprecated: Old Speaker data class (backward compatibility)

11. **`ui/common/UnifiedReaderScreen.kt`**
    - Added: `selectedLanguage` parameter
    - Updated: `onVoiceChange` callback signature
    - Replaced: Hardcoded voice list with SpeakerSelectionDialog

12. **`ui/pdfreader/PDFViewerScreen.kt`**
    - Added: `selectedLanguage` passed to UnifiedReaderScreen
    - Updated: `onVoiceChange` callback to use new signature

## User Experience Flow

### Setting Global Voice (Settings Screen)

1. User opens Settings
2. Clicks on the Voice card
3. VoiceSelectionDialog appears with:
   - Language selection chips (Korean/English)
   - Voice list grouped by gender
4. User selects language (optional)
5. User selects voice
6. Clicks "Apply"
7. Settings are saved and applied to all new documents

### Setting Document-Specific Voice (PDF Viewer)

1. User opens a document in PDF Viewer
2. Taps the voice icon in the control panel
3. SpeakerSelectionDialog appears
4. User selects language and voice
5. Changes are:
   - Applied immediately to the current playback
   - Saved to the document in the database
   - Persisted for future sessions
6. This voice will be used only for this specific document

### Document Loading Behavior

When a document is loaded:
1. Check if document has specific voice settings
2. If yes → use document's voice, language, and speed
3. If no → use global settings from preferences
4. Generate audio with the selected voice

## Database Schema Changes

### Before (Version 1)
```sql
CREATE TABLE documents (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    type TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    lastReadPosition INTEGER NOT NULL DEFAULT 0
);
```

### After (Version 2)
```sql
CREATE TABLE documents (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    type TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    lastReadPosition INTEGER NOT NULL DEFAULT 0,
    voiceId TEXT DEFAULT NULL,
    language TEXT DEFAULT NULL,
    speed REAL DEFAULT NULL
);
```

### Migration Strategy
- Uses ALTER TABLE statements (non-destructive)
- All new columns are nullable with DEFAULT NULL
- Existing data remains intact
- Fallback to destructive migration if needed

## API Integration

The system is designed to work with the TTS backend API:

### Current API Endpoint
```
POST /api/tts/synthesize
```

### Request Format (TtsRequest)
```kotlin
data class TtsRequest(
    val text: String,
    val language: String = "en-US",
    val voice: String? = null,
    val speed: Float = 1.0f,
    val pitch: Float = 0.0f
)
```

### Voice Mapping
The voice IDs in the app map directly to backend voice IDs:
- `matt` → Backend voice "matt"
- `minseo` → Backend voice "minseo"
- etc.

## Testing Checklist

- [ ] Global voice selection works in Settings
- [ ] Per-document voice selection works in PDF Viewer
- [ ] Voice persistence works after app restart
- [ ] Database migration works without data loss
- [ ] Korean voices play correctly
- [ ] English voices play correctly
- [ ] Speed adjustment persists per document
- [ ] Loading saved documents uses correct voice
- [ ] New documents use global settings
- [ ] Voice dialog shows correct current selection

## Future Enhancements

1. **Voice Preview**: Add ability to preview voices before selecting
2. **More Languages**: Add support for additional languages
3. **Voice Profiles**: Create named voice profiles/presets
4. **Batch Update**: Update voice for multiple documents at once
5. **Voice Recommendations**: Suggest voices based on content language
6. **Custom Voices**: Support for user-uploaded custom voice models

## Notes

- The voice system is fully backward compatible
- Old documents without voice settings will use global defaults
- The system gracefully handles missing or invalid voice IDs
- Language selection automatically filters available voices
- Default voices are carefully chosen (Matt for English, Minseo for Korean)

## Files Summary

### Created Files (4)
1. `android/app/src/main/java/com/example/voicereaderapp/domain/model/Voice.kt`
2. `android/app/src/main/java/com/example/voicereaderapp/ui/common/VoiceSelectionDialog.kt`
3. `android/app/src/main/java/com/example/voicereaderapp/data/local/database/Migrations.kt`
4. `VOICE_SYSTEM_IMPLEMENTATION.md` (this file)

### Modified Files (12)
1. `android/app/src/main/java/com/example/voicereaderapp/domain/model/ReadingDocument.kt`
2. `android/app/src/main/java/com/example/voicereaderapp/data/local/entity/DocumentEntity.kt`
3. `android/app/src/main/java/com/example/voicereaderapp/data/local/database/VoiceReaderDatabase.kt`
4. `android/app/src/main/java/com/example/voicereaderapp/di/DatabaseModule.kt`
5. `android/app/src/main/java/com/example/voicereaderapp/domain/model/VoiceSettings.kt`
6. `android/app/src/main/java/com/example/voicereaderapp/data/local/preferences/VoiceSettingsPreferences.kt`
7. `android/app/src/main/java/com/example/voicereaderapp/ui/settings/SettingsViewModel.kt`
8. `android/app/src/main/java/com/example/voicereaderapp/ui/settings/SettingsScreen.kt`
9. `android/app/src/main/java/com/example/voicereaderapp/ui/pdfreader/PDFViewerViewModel.kt`
10. `android/app/src/main/java/com/example/voicereaderapp/ui/pdfreader/SpeakerDialog.kt`
11. `android/app/src/main/java/com/example/voicereaderapp/ui/common/UnifiedReaderScreen.kt`
12. `android/app/src/main/java/com/example/voicereaderapp/ui/pdfreader/PDFViewerScreen.kt`

---

**Implementation Date**: January 2025
**Version**: Database v2, App v1.0

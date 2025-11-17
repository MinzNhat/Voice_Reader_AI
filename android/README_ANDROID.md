# Voice Reader AI - Android App Guide

Complete Android app for PDF OCR with real-time TTS and word highlighting.

## Architecture

```
android/app/src/main/java/com/example/voicereaderapp/
├── data/
│   ├── remote/
│   │   ├── api/
│   │   │   └── VoiceReaderAPI.kt          # Retrofit API interface
│   │   ├── model/
│   │   │   ├── OCRResponse.kt              # OCR data models
│   │   │   ├── TTSResponse.kt              # TTS data models
│   │   │   └── TimingResponse.kt           # Timing data models
│   │   └── NetworkModule.kt                # Retrofit configuration
│   └── repository/
│       ├── OCRRepositoryImpl.kt            # OCR implementation
│       └── TTSRepositoryImpl.kt            # TTS implementation
├── domain/
│   └── repository/
│       ├── OCRRepository.kt                # OCR interface
│       └── TTSRepository.kt                # TTS interface
├── ui/
│   └── pdfreader/
│       ├── PDFViewerViewModel.kt           # ViewModel with OCR/TTS logic
│       └── PDFViewerScreen.kt              # Compose UI with overlay
└── utils/
    ├── Result.kt                           # Result wrapper
    └── PDFHelper.kt                        # PDF utilities
```

## Features Implemented

### 1. Backend Integration
- **Retrofit API** - All 4 endpoints connected
- **OCR Upload** - PDF and image support with multipart upload
- **TTS Generation** - Text-to-speech with NAVER TTS Premium
- **Word Timing** - Real-time timing data for highlighting

### 2. PDF Rendering
- **PdfRenderer** - Native Android PDF rendering
- **Zoom & Pan** - Gesture support for PDF navigation
- **Multi-page** - Ready for page navigation (TODO)

### 3. OCR Processing
- **Automatic Upload** - Send PDF/image to backend
- **Bounding Boxes** - Parse and display word coordinates
- **Normalized Format** - Clean data structure from backend

### 4. Real-time Highlighting
- **Canvas Overlay** - Draw bounding boxes on PDF
- **Word Sync** - Highlight current word during TTS playback
- **Color Coding** - Gold for active word, green for all words
- **Smooth Updates** - 50ms progress tracking

### 5. Audio Playback
- **MediaPlayer** - Native Android audio playback
- **Base64 Decode** - Convert backend response to audio
- **Controls** - Play, Pause, Resume, Stop
- **Progress Tracking** - Real-time position updates

## Setup Instructions

### 1. Update Backend URL

Edit `NetworkModule.kt`:

```kotlin
// For Android Emulator
private const val BASE_URL = "http://10.0.2.2:3000/"

// For physical device on same network
private const val BASE_URL = "http://YOUR_COMPUTER_IP:3000/"
```

### 2. Add Dependencies to `build.gradle`

```gradle
dependencies {
    // Retrofit & Networking
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.11.0"

    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"

    // Compose
    implementation "androidx.compose.material3:material3:1.1.2"
    implementation "androidx.compose.material:material-icons-extended:1.5.4"

    // ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2"
}
```

### 3. Add Permissions to `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

### 4. Network Security Config (for HTTP)

If using HTTP (development), add to `AndroidManifest.xml`:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

Create `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

## Usage Example

### Using PDFViewerScreen

```kotlin
@Composable
fun MyApp() {
    val viewModel = remember {
        PDFViewerViewModel(
            ocrRepository = OCRRepositoryImpl(NetworkModule.api),
            ttsRepository = TTSRepositoryImpl(NetworkModule.api)
        )
    }

    val pdfFile = File("/path/to/document.pdf")

    PDFViewerScreen(
        pdfFile = pdfFile,
        viewModel = viewModel,
        onBack = { /* navigate back */ }
    )
}
```

### Workflow

1. **Open PDF** - Display using PDFViewerScreen
2. **Run OCR** - Tap OCR button → uploads to backend → displays bounding boxes
3. **Generate Speech** - Tap TTS button → generates audio + timing
4. **Play Audio** - Tap Play → audio plays with synchronized highlighting

## Key Components

### PDFViewerViewModel

```kotlin
// Perform OCR
viewModel.performOCR(file)

// Generate speech (requires OCR first)
viewModel.generateSpeech(speaker = "nara")

// Control playback
viewModel.playAudio()
viewModel.pauseAudio()
viewModel.stopAudio()
```

### UI State

```kotlin
data class PDFViewerUiState(
    val ocrText: String?,          // Extracted text
    val ocrWords: List<OCRWord>,   // Words with bounding boxes
    val audioBase64: String?,       // Generated audio
    val wordTimings: List<WordTiming>, // Word timing data
    val currentWordIndex: Int,      // Currently highlighted word
    val isPlaying: Boolean          // Playback state
)
```

### Bounding Box Overlay

The `PDFWithOCROverlay` composable:
- Renders PDF using PdfRenderer
- Draws OCR bounding boxes using Canvas
- Highlights current word in gold
- Supports zoom and pan gestures

## API Response Format

All responses match backend specification in CLAUDE.md:

**OCR Response:**
```json
{
  "text": "full text",
  "words": [
    { "text": "word", "bbox": {...}, "index": 0 }
  ]
}
```

**TTS Response:**
```json
{
  "audio": "base64_mp3_data..."
}
```

**Timing Response:**
```json
{
  "timings": [
    { "word": "Hello", "index": 0, "startMs": 0, "endMs": 320 }
  ]
}
```

## Real-time Highlighting Logic

1. **Generate Timing** - Backend calculates word start/end times
2. **Track Progress** - MediaPlayer reports current position (50ms updates)
3. **Find Current Word** - Match position to timing array
4. **Update UI** - Change `currentWordIndex` in state
5. **Recompose Canvas** - Highlight new word in gold

## Troubleshooting

**Error: "Unable to resolve host"**
- Check BASE_URL in NetworkModule
- Verify backend is running
- Test with `http://10.0.2.2:3000` for emulator

**OCR not working:**
- Check file permissions
- Verify backend OCR endpoint is responding
- Check Logcat for network errors

**Audio not playing:**
- Verify TTS generated successfully
- Check audio permissions
- Ensure base64 decoding works

**Bounding boxes misaligned:**
- OCR coordinates are absolute pixels
- Check scaling calculations in Canvas
- Verify PDF render size matches OCR input

## Next Steps

1. **Multi-page Support** - Add page navigation
2. **Crop Tool** - UI for selecting crop regions
3. **Speaker Selection** - Dropdown for TTS voices
4. **Offline Mode** - Cache OCR results
5. **Export** - Save OCR text to file

## Testing

Run the backend first:
```bash
cd backend
npm start
```

Then run Android app and test workflow:
1. Select PDF
2. Run OCR
3. Generate Speech
4. Play and verify highlighting

---

All components follow CLAUDE.md specifications. Ready for hackathon demo!

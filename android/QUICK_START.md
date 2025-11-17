# Voice Reader AI - Quick Start Guide

## How to Use the App

### Step 1: Update MainActivity

Replace the content in `MainActivity.kt`:

```kotlin
package com.example.voicereaderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.voicereaderapp.ui.pdfreader.PDFReaderNavigation
import com.example.voicereaderapp.ui.theme.VoiceReaderAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceReaderAppTheme {
                // Use the PDF Reader Navigation
                PDFReaderNavigation()
            }
        }
    }
}
```

### Step 2: Update NetworkModule BASE_URL

Edit `data/remote/NetworkModule.kt`:

```kotlin
// For Android Emulator (recommended for testing)
private const val BASE_URL = "http://10.0.2.2:3000/"

// OR for physical device (use your computer's IP)
// private const val BASE_URL = "http://192.168.x.x:3000/"
```

### Step 3: Add Required Dependencies

Add to your `app/build.gradle`:

```gradle
dependencies {
    // Retrofit for API calls
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    implementation "com.squareup.okhttp3:logging-interceptor:4.11.0"

    // Navigation for Compose
    implementation "androidx.navigation:navigation-compose:2.7.5"

    // Material Icons Extended
    implementation "androidx.compose.material:material-icons-extended:1.5.4"
}
```

### Step 4: Add Permissions

Add to `AndroidManifest.xml` (inside `<manifest>` tag):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
    android:minSdkVersion="33" />
```

### Step 5: Network Security Config (for HTTP)

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

Then add to `AndroidManifest.xml` (inside `<application>` tag):

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

### Step 6: Start Backend Server

```bash
cd backend
npm start
```

Make sure it's running on http://localhost:3000

### Step 7: Build and Run App

1. Connect your emulator or device
2. Click "Run" in Android Studio
3. App will open to the Document Picker screen

---

## How to Use the App

### Screen 1: Document Picker

When you open the app, you'll see:

```
┌─────────────────────────────────────┐
│  Voice Reader AI              [Top] │
├─────────────────────────────────────┤
│                                     │
│  Welcome to Voice Reader AI         │
│                                     │
│  Upload a PDF or image:             │
│  1. Tap PDF or Image button         │
│  2. Select file from device         │
│  3. Tap to open and start OCR+TTS   │
│                                     │
│                                     │
│                                     │
│                            [📄] PDF │
│                            [🖼️] IMG │
└─────────────────────────────────────┘
```

**Actions:**
- Tap **PDF button** (bottom right) → Opens file picker for PDFs
- Tap **Image button** → Opens file picker for images
- Select a file → It appears in the list
- Tap the file card → Opens PDF Viewer

---

### Screen 2: PDF Viewer

After selecting a file:

```
┌─────────────────────────────────────┐
│ [←] PDF Reader    [🔍OCR] [🔊TTS]  │
├─────────────────────────────────────┤
│                                     │
│     [PDF/Image Content]             │
│     with bounding boxes             │
│     highlighted in GREEN            │
│     Current word in GOLD            │
│                                     │
│                                     │
├─────────────────────────────────────┤
│       [⏹️]    [▶️/⏸️]               │
└─────────────────────────────────────┘
```

**Actions:**

1. **Run OCR** - Tap 🔍 button
   - Uploads file to backend
   - Shows green boxes around detected text
   - Text appears in state

2. **Generate Speech** - Tap 🔊 button (after OCR)
   - Sends text to TTS API
   - Generates audio + timing
   - Audio controls appear at bottom

3. **Play Audio** - Tap ▶️ button
   - Plays generated speech
   - Current word highlights in **GOLD**
   - Real-time sync as audio plays

4. **Pause/Resume** - Tap ⏸️ button
   - Pauses playback
   - Tap again to resume

5. **Stop** - Tap ⏹️ button
   - Stops playback
   - Resets highlighting

---

## Complete Workflow Example

### Example: Reading a PDF

```
1. Open app → Document Picker Screen
   ↓
2. Tap PDF button (bottom right)
   ↓
3. Select "my_document.pdf"
   ↓
4. File appears in list
   ↓
5. Tap file card → Opens PDF Viewer
   ↓
6. Tap 🔍 OCR button
   → PDF uploads to backend
   → Green boxes appear around text
   → "Processing..." → "OCR Complete"
   ↓
7. Tap 🔊 TTS button
   → Text converts to speech
   → Audio controls appear
   → "Generating..." → "Ready to Play"
   ↓
8. Tap ▶️ Play button
   → Audio starts playing
   → Words highlight in GOLD in real-time
   → "Hello" (gold) → "world" (gold) → ...
   ↓
9. Tap ⏸️ to pause or ⏹️ to stop
```

---

## Troubleshooting

### App opens but crashes when picking file
- Check permissions in AndroidManifest.xml
- For Android 13+, grant "Photos and Media" permission

### OCR button does nothing
- Check backend is running (http://localhost:3000)
- Check BASE_URL in NetworkModule.kt
- Check network_security_config.xml
- Open Logcat for network errors

### Green boxes don't appear after OCR
- Check Logcat for response errors
- Verify backend returned proper format
- Test backend with http://localhost:3000 test panel

### Audio doesn't play
- Check speaker permissions
- Verify TTS generated successfully
- Check Logcat for MediaPlayer errors

### Highlighting not syncing with audio
- Timing is approximate (300ms + 50ms per character)
- Works best with English text
- Backend timing calculation can be adjusted

---

## App Flow Diagram

```
MainActivity
    ↓
PDFReaderNavigation
    ↓
    ├─→ DocumentPickerScreen (start)
    │      ↓ (user picks file)
    │      │
    └─→ PDFViewerScreen
           ↓
       PDFViewerViewModel
           ├─→ OCRRepository → POST /ocr
           └─→ TTSRepository → POST /tts
                              → POST /tts/timing
```

---

## File Structure Created

```
android/app/src/main/java/com/example/voicereaderapp/
└── ui/pdfreader/
    ├── DocumentPickerScreen.kt    ← Pick PDF/image
    ├── PDFReaderNavigation.kt     ← Navigation logic
    ├── PDFViewerScreen.kt          ← View PDF with overlay
    └── PDFViewerViewModel.kt       ← OCR/TTS logic
```

---

## Testing Checklist

- [ ] Backend running on port 3000
- [ ] App builds without errors
- [ ] Document picker screen appears
- [ ] Can tap PDF button and see file picker
- [ ] Can select a PDF file
- [ ] File appears in list
- [ ] Tapping file opens PDF viewer
- [ ] Can see PDF rendered
- [ ] OCR button uploads and shows boxes
- [ ] TTS button generates audio
- [ ] Play button plays audio
- [ ] Words highlight in gold during playback

---

## What's Included

✅ Document picker with file selection
✅ PDF rendering with zoom/pan
✅ OCR with bounding box overlay
✅ TTS generation with NAVER API
✅ Real-time word highlighting
✅ Audio playback controls
✅ Error handling
✅ Clean architecture (MVVM + Repository)

---

## Next Features to Add

- Multi-page PDF navigation
- Manual crop tool
- Speaker voice selection
- Speed/pitch controls
- Save OCR results
- History of read documents

---

**You're ready to demo!** 🚀

Just update MainActivity, add dependencies, and run the app.

# Complete Setup - Copy/Paste Ready

## 1. Update MainActivity.kt

**Location:** `app/src/main/java/com/example/voicereaderapp/MainActivity.kt`

**Replace entire file with:**

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
                PDFReaderNavigation()
            }
        }
    }
}
```

---

## 2. Add Dependencies to build.gradle

**Location:** `app/build.gradle`

**Add inside `dependencies {}`:**

```gradle
// Retrofit & OkHttp
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-gson:2.9.0"
implementation "com.squareup.okhttp3:okhttp:4.11.0"
implementation "com.squareup.okhttp3:logging-interceptor:4.11.0"

// Compose Navigation
implementation "androidx.navigation:navigation-compose:2.7.5"

// Material Icons Extended
implementation "androidx.compose.material:material-icons-extended:1.5.4"
```

**Then click "Sync Now"**

---

## 3. Update AndroidManifest.xml

**Location:** `app/src/main/AndroidManifest.xml`

**Add BEFORE `<application>` tag:**

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

**Update `<application>` tag to include:**

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... (keep other attributes)
```

---

## 4. Create Network Security Config

**Location:** `app/src/main/res/xml/network_security_config.xml`

**Create new file with:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

---

## 5. Update NetworkModule BASE_URL

**Location:** `app/src/main/java/com/example/voicereaderapp/data/remote/NetworkModule.kt`

**Find this line:**

```kotlin
private const val BASE_URL = "http://10.0.2.2:3000/"
```

**For emulator:** Keep as is: `http://10.0.2.2:3000/`

**For physical device:** Change to: `http://YOUR_COMPUTER_IP:3000/`

To find your IP:
- Windows: `ipconfig` → Look for "IPv4 Address"
- Mac/Linux: `ifconfig` → Look for "inet"

---

## 6. Start Backend Server

```bash
cd backend
npm start
```

**Verify it's running:**
Open browser → http://localhost:3000

---

## 7. Build & Run

1. Click **Build** → **Rebuild Project**
2. Wait for build to finish
3. Click **Run** (green play button)
4. Select emulator or device
5. App will launch!

---

## Expected Result

### When App Opens:

```
┌──────────────────────────────────┐
│ Voice Reader AI            [Top] │
├──────────────────────────────────┤
│                                  │
│  Welcome to Voice Reader AI      │
│                                  │
│  Upload a PDF or image:          │
│  1. Tap PDF or Image button      │
│  2. Select file                  │
│  3. Tap to open                  │
│                                  │
│                                  │
│                      [PDF] [IMG] │
└──────────────────────────────────┘
```

### After Selecting PDF and Running OCR:

```
┌──────────────────────────────────┐
│ [←] PDF Reader  [OCR] [TTS] [Top]│
├──────────────────────────────────┤
│                                  │
│  [PDF with GREEN boxes           │
│   around detected text]          │
│                                  │
│  Current word in GOLD            │
│                                  │
├──────────────────────────────────┤
│     [STOP]    [PLAY]       [Bot] │
└──────────────────────────────────┘
```

---

## Full Usage Flow

```
1. Tap PDF button (bottom right)
   ↓
2. Choose "document.pdf"
   ↓
3. File appears → Tap it
   ↓
4. PDF opens → Tap 🔍 OCR button
   ↓
5. Green boxes appear around text
   ↓
6. Tap 🔊 TTS button
   ↓
7. Audio controls appear
   ↓
8. Tap ▶️ Play
   ↓
9. Words highlight GOLD in real-time!
```

---

## Common Issues

### Issue: "Cannot resolve symbol 'PDFReaderNavigation'"
**Fix:** Build → Rebuild Project

### Issue: "Network error" or "Connection refused"
**Fix:**
1. Check backend is running (port 3000)
2. Verify BASE_URL is correct
3. Check network_security_config.xml exists

### Issue: File picker doesn't open
**Fix:** Grant storage permissions in device settings

### Issue: OCR fails
**Fix:**
1. Test backend at http://localhost:3000
2. Check NAVER credentials in backend/.env
3. Check Logcat for errors

---

## Verify Setup Checklist

- [ ] MainActivity.kt updated
- [ ] Dependencies added to build.gradle
- [ ] Project synced successfully
- [ ] Permissions in AndroidManifest.xml
- [ ] network_security_config.xml created
- [ ] BASE_URL updated in NetworkModule.kt
- [ ] Backend running on port 3000
- [ ] App builds without errors
- [ ] App launches to Document Picker screen

---

**Ready to demo!** 🎉

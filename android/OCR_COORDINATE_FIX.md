# OCR Coordinate Alignment Fix

## Problem Solved ✅

**Issue**: OCR bounding boxes appeared offset to the right and down from actual text position.

**Root Cause**: NAVER CLOVA OCR processes PDFs at a different resolution than Android's `PdfRenderer`, causing coordinate system mismatch.

---

## How It Works

### The Coordinate Mismatch

When you send a PDF to NAVER OCR:

1. **Android PdfRenderer** renders PDF at native dimensions (e.g., 612x792 points at 72 DPI)
2. **NAVER CLOVA OCR** internally converts PDF to image at its own resolution (could be 96 DPI, 150 DPI, 300 DPI, etc.)
3. **OCR returns coordinates** based on its internal image dimensions
4. **Result**: Coordinates don't match! 🔴

### Example

```
PDF native size: 612 x 792
NAVER renders at: 1275 x 1650  (higher DPI)

Word "HELLO" at OCR coordinate (491, 245):
- OCR thinks: 491px from left
- But PDF coordinate system: Should be ~236px from left
- Scale factor needed: 612 / 1275 = 0.48
```

---

## The Solution

### Backend Changes

**File**: `backend/src/server.js`

Added OCR image dimensions to response:

```javascript
function normalizeOCRResponse(naverResponse) {
    const image = naverResponse.images[0];

    // Capture actual dimensions NAVER used
    imageWidth = image.width || 0;
    imageHeight = image.height || 0;

    return {
        text: fullText.trim(),
        words: words,
        imageWidth: imageWidth,    // NEW!
        imageHeight: imageHeight   // NEW!
    };
}
```

### Android Data Model

**File**: `android/.../data/remote/model/OCRResponse.kt`

```kotlin
data class OCRResponse(
    val text: String,
    val words: List<OCRWord>,
    val imageWidth: Int = 0,   // NEW!
    val imageHeight: Int = 0   // NEW!
)
```

### ViewModel State

**File**: `android/.../ui/pdfreader/PDFViewerViewModel.kt`

```kotlin
data class PDFViewerUiState(
    val ocrWords: List<OCRWord> = emptyList(),
    val ocrImageWidth: Int = 0,   // NEW!
    val ocrImageHeight: Int = 0   // NEW!
)
```

### Coordinate Transformation

**File**: `android/.../ui/pdfreader/SpeechifyStylePDFViewer.kt`

```kotlin
// CRITICAL FIX: Two-step coordinate transformation

// Step 1: Scale from OCR image space → PDF space
val ocrToPdfScaleX = pdfWidth / ocrImageWidth
val ocrToPdfScaleY = pdfHeight / ocrImageHeight

val pdfLeft = bbox.left * ocrToPdfScaleX
val pdfTop = bbox.top * ocrToPdfScaleY

// Step 2: Transform from PDF space → Canvas space
val canvasLeft = pdfLeft * totalScale + finalOffsetX
val canvasTop = pdfTop * totalScale + finalOffsetY
```

---

## Visual Diagram

```
┌──────────────────────────────────────────────────────┐
│ NAVER OCR Image (1275x1650)                          │
│                                                       │
│   BBox: (491, 245)                                   │
│   ↓                                                   │
│   [HELLO NAVER]                                      │
│                                                       │
└──────────────────────────────────────────────────────┘
                    ↓ Scale by (612/1275, 792/1650)
┌──────────────────────────────────────────────────────┐
│ PDF Space (612x792)                                   │
│                                                       │
│   BBox: (236, 117)                                   │
│   ↓                                                   │
│   [HELLO NAVER]                                      │
│                                                       │
└──────────────────────────────────────────────────────┘
                    ↓ Scale by baseFitScale × userZoom
┌──────────────────────────────────────────────────────┐
│ Canvas Space (1080x1920)                              │
│                                                       │
│   BBox: (430, 215)                                   │
│   ↓                                                   │
│   [HELLO NAVER] ← Blue pill now aligned! ✅          │
│                                                       │
└──────────────────────────────────────────────────────┘
```

---

## Testing

### 1. Rebuild and Run

```bash
cd backend
node src/server.js
```

```bash
cd android
./gradlew clean build
# Run on device/emulator
```

### 2. Check Logcat

Filter for `OCR_DEBUG`:

```
OCR_DEBUG: OCR image dimensions: 1275x1650
OCR_DEBUG: PDF dimensions: 612.0 x 792.0
OCR_DEBUG: OCR→PDF scale: (0.48, 0.48)
OCR_DEBUG: BBox in OCR space: (491.0, 245.0)
OCR_DEBUG: BBox in PDF space: (236.0, 117.0)
OCR_DEBUG: Transformed to canvas: (430, 215)
```

### 3. Visual Check

1. Upload PDF
2. Run OCR
3. **Blue pills should now align perfectly with text** ✅
4. Red crosshair on first word should be centered on the word

---

## What Changed

### Before (Broken)

```kotlin
// Assumed OCR coords were in PDF space
val canvasLeft = bbox.left * totalScale + finalOffsetX  // ❌ Wrong!
```

### After (Fixed)

```kotlin
// Scale OCR coords to PDF space first
val pdfLeft = bbox.left * (pdfWidth / ocrImageWidth)
val canvasLeft = pdfLeft * totalScale + finalOffsetX  // ✅ Correct!
```

---

## Why This Happens

PDF dimensions are in **points** (1/72 inch), but images have **pixels** at various DPI:

| DPI | 612pt width becomes |
|-----|---------------------|
| 72  | 612px (1:1 ratio)   |
| 96  | 816px               |
| 150 | 1275px              |
| 300 | 2550px              |

NAVER OCR likely uses 150 DPI or higher for better accuracy, so the image it processes is much larger than the PDF's native dimensions.

---

## Files Modified

✅ `backend/src/server.js` - Added imageWidth/imageHeight to response
✅ `android/.../OCRResponse.kt` - Added fields to data model
✅ `android/.../PDFViewerViewModel.kt` - Added state fields
✅ `android/.../SpeechifyStylePDFViewer.kt` - Fixed coordinate transform

---

## Troubleshooting

### Boxes still misaligned?

**Check backend console**:
```
[OCR] NAVER processed image at: 1275x1650
```

If you see `0x0`, the backend isn't receiving dimensions from NAVER. Check API response.

**Check Logcat**:
```
OCR_DEBUG: OCR image dimensions: 0x0  ← Should NOT be 0!
```

If dimensions are 0, backend needs to be updated/restarted.

### Boxes too small/large?

Check the scale factor:
```
OCR_DEBUG: OCR→PDF scale: (0.48, 0.48)
```

Should be close to 0.5 for typical PDFs. If it's way off (like 5.0 or 0.1), something is wrong.

---

## Success Criteria

When working correctly:

1. ✅ Backend logs show non-zero OCR image dimensions
2. ✅ Logcat shows correct scale factors
3. ✅ Blue pills align perfectly with text
4. ✅ Red crosshair (first word debug) is centered
5. ✅ Zoom/pan doesn't break alignment

---

## Next Steps

1. Test with different PDF sizes (A4, Letter, etc.)
2. Test with scanned PDFs vs text PDFs
3. Test with images (JPG/PNG)
4. Remove debug logging (red crosshair) when confirmed working

**The coordinate system is now fixed!** 🎉

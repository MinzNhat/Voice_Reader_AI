# BBox Alignment Debug Guide

## Your Issue
OCR bounding boxes are offset to the middle and right. The API returns:
```json
{
  "text": "Adobe",
  "bbox": {
    "x1": 424, "y1": 147,
    "x2": 516, "y2": 147,
    "x3": 516, "y3": 171,
    "x4": 424, "y4": 171
  }
}
```

But the blue pills appear in the wrong position.

---

## Debug Steps

### 1. Build and Run with Debug Logs
```bash
./gradlew clean build
# Run on device/emulator
```

### 2. Check Logcat
Open Logcat and filter for:
- `PDF_DEBUG` - Shows PDF page dimensions
- `OCR_DEBUG` - Shows coordinate transformations

**Look for:**
```
PDF_DEBUG: PDF Page Size: 612 x 792
OCR_DEBUG: Word: Adobe
OCR_DEBUG: BBox in PDF space: (424, 147) to (516, 171)
OCR_DEBUG: PDF dimensions: 612 x 792
OCR_DEBUG: Total scale: 1.76
OCR_DEBUG: Final offset: (234, 564)
OCR_DEBUG: Transformed to canvas: (980, 823) to (1142, 865)
```

---

## Diagnosis

### Check 1: PDF Dimensions Match?
**Expected:** OCR API used the same PDF dimensions as PdfRenderer

**Compare:**
- Logcat: `PDF Page Size: 612 x 792`
- OCR bbox: `x1: 424` (should be < 612) ✓
- OCR bbox: `y1: 147` (should be < 792) ✓

**If bbox > PDF dimensions:**
- OCR used different DPI or page size
- Need to scale coordinates

---

### Check 2: Coordinate System

#### Test A: Are coordinates normalized (0-1)?
If bbox values are like `0.693, 0.185`, they're normalized.

**Fix:**
```kotlin
val canvasLeft = bbox.left * pdfWidth * totalScale + finalOffsetX
```

#### Test B: Is Y-axis inverted?
PDF coordinate systems can have origin at top-left OR bottom-left.

**Fix:**
```kotlin
val canvasTop = (pdfHeight - bbox.bottom) * totalScale + finalOffsetY
val canvasBottom = (pdfHeight - bbox.top) * totalScale + finalOffsetY
```

#### Test C: Different DPI?
NAVER OCR might render at different DPI than PdfRenderer.

**Fix:**
```kotlin
val dpiScale = 72f / 96f  // PDF typically 72 DPI, OCR might use 96
val canvasLeft = bbox.left * dpiScale * totalScale + finalOffsetX
```

---

## Common Issues & Solutions

### Issue 1: Boxes shifted right and down
**Cause:** Extra offset being added

**Check:**
```
finalOffsetX should be: (canvasWidth - displayedWidth) / 2
```

**Test:** At 1x zoom, centered PDF:
```kotlin
val expectedOffsetX = (canvasWidth - (pdfWidth * baseFitScale)) / 2
Log.d("DEBUG", "Expected offset: $expectedOffsetX")
Log.d("DEBUG", "Actual offset: $finalOffsetX")
```

---

### Issue 2: Boxes too large/small
**Cause:** Scale mismatch

**Check:**
```kotlin
Log.d("DEBUG", "Base fit scale: $baseFitScale")
Log.d("DEBUG", "User zoom: $userZoom")
Log.d("DEBUG", "Total scale: $totalScale")
```

**Expected:**
- baseFitScale: ~1.76 (for 612px PDF on 1080px screen)
- userZoom: 1.0 (default)
- totalScale: 1.76

---

### Issue 3: Boxes in completely wrong place
**Cause:** Coordinate space mismatch

**Possible fixes:**

**A. OCR returns screen coordinates, not PDF coordinates:**
```kotlin
// Don't transform, use directly:
val canvasLeft = bbox.left
val canvasTop = bbox.top
```

**B. OCR returns percentage of image:**
```kotlin
val canvasLeft = (bbox.left / 100) * displayedWidth + finalOffsetX
```

**C. OCR returns coordinates for different page orientation:**
Check if PDF was rotated during OCR.

---

## Quick Test

Add this to your code after OCR completes:

```kotlin
// In PDFViewerViewModel after OCR success:
_uiState.value.ocrWords.firstOrNull()?.let { firstWord ->
    Log.d("TEST", "===== FIRST WORD TEST =====")
    Log.d("TEST", "Word: ${firstWord.text}")
    Log.d("TEST", "BBox: ${firstWord.bbox}")
    Log.d("TEST", "Expected: Should be near top-left of '${firstWord.text}'")
}
```

Then visually check:
1. Does the first word's bbox match its position in the PDF?
2. Is it scaled correctly?
3. Is the offset correct?

---

## Manual Coordinate Check

Use this formula to check if transform is correct:

### For "Adobe" at (424, 147):

**Given:**
- PDF page: 612 x 792
- Canvas: 1080 x 1920
- baseFitScale = 1080 / 612 = 1.76
- userZoom = 1.0
- totalScale = 1.76

**Transform:**
```
canvasLeft = 424 * 1.76 + finalOffsetX
canvasTop = 147 * 1.76 + finalOffsetY
```

**Expected:**
```
canvasLeft = 746 + offsetX
canvasTop = 259 + offsetY
```

**Check offsetX:**
```
displayedWidth = 612 * 1.76 = 1077
offsetX = (1080 - 1077) / 2 = 1.5 (nearly centered)
```

**Final position:**
```
canvasLeft ≈ 747 pixels from left
canvasTop ≈ 260 pixels from top
```

Measure on screen: Does "Adobe" appear at (747, 260)?

---

## Force Alignment Test

Try this temporary hack to see if coordinates are correct:

```kotlin
// Ignore transform, draw at raw coordinates
val canvasLeft = bbox.left  // No scaling!
val canvasTop = bbox.top     // No offset!
```

If boxes NOW align → Transform is wrong
If boxes STILL don't align → OCR coordinates are wrong

---

## Backend Check

The OCR coordinates come from NAVER CLOVA. Check:

1. **What image was sent to OCR?**
   - Same PDF page at same resolution?
   - Different resolution = coordinates won't match

2. **What page dimensions did NAVER use?**
   - Backend might resize before sending
   - Check backend logs for image dimensions

3. **Format of the OCR request**
   - PDF sent directly? Or converted to image first?
   - Conversion might change dimensions

---

## Solution Template

Based on Logcat output, apply the appropriate fix:

### If coordinates are correct but offset:
```kotlin
// Already correct in code
val canvasLeft = bbox.left * totalScale + finalOffsetX
```

### If Y-axis inverted:
```kotlin
val canvasTop = (pdfHeight - bbox.bottom) * totalScale + finalOffsetY
val canvasBottom = (pdfHeight - bbox.top) * totalScale + finalOffsetY
```

### If DPI mismatch:
```kotlin
val scale = (pdfWidth / ocrImageWidth)  // Adjust based on actual dimensions
val canvasLeft = bbox.left * scale * totalScale + finalOffsetX
```

### If normalized (0-1):
```kotlin
val canvasLeft = bbox.left * pdfWidth * totalScale + finalOffsetX
```

---

## Next Steps

1. **Run app with debug logs**
2. **Upload the same PDF you tested in backend**
3. **Check Logcat for debug output**
4. **Compare:**
   - PDF dimensions vs OCR bbox values
   - Expected canvas position vs actual
5. **Apply the appropriate fix from above**

Send me the Logcat output and I'll tell you exactly which fix to apply! 🎯

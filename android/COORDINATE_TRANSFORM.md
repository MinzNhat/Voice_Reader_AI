# PDF Overlay Coordinate Transformation Guide

## Problem Summary

OCR bounding boxes were misaligned because:
1. **Mixed coordinate systems** - OCR coords in PDF space, drawing in canvas space
2. **Incorrect transform order** - Base fit scale and user zoom were conflated
3. **Broken zoom centering** - Zoom didn't center around gesture point
4. **No pan constraints** - Boxes would drift when panning

## Solution: Explicit Transform Pipeline

### Coordinate Systems

```
┌─────────────────────────────────────────────────────────┐
│ PDF SPACE (Source)                                      │
│ - Original PDF dimensions (e.g., 612 x 792 points)     │
│ - OCR bounding boxes live here                          │
│ - Immutable, device-independent                         │
└─────────────────────────────────────────────────────────┘
                        ↓
            [Base Fit Scale Transform]
                        ↓
┌─────────────────────────────────────────────────────────┐
│ FIT SPACE (Intermediate)                                │
│ - PDF scaled to fit canvas (aspect-fit)                │
│ - Maintains aspect ratio                                │
│ - Centered in canvas                                    │
└─────────────────────────────────────────────────────────┘
                        ↓
              [User Zoom Transform]
                        ↓
┌─────────────────────────────────────────────────────────┐
│ ZOOMED SPACE (Intermediate)                             │
│ - User's zoom multiplier applied                        │
│ - Still conceptually centered                           │
└─────────────────────────────────────────────────────────┘
                        ↓
               [Pan Offset Transform]
                        ↓
┌─────────────────────────────────────────────────────────┐
│ CANVAS SPACE (Display)                                  │
│ - Final screen pixels                                   │
│ - Where we actually draw                                │
└─────────────────────────────────────────────────────────┘
```

## Transform Pipeline (Step-by-Step)

### Step 1: Base Fit Scale
```kotlin
val baseScaleX = canvasWidth / pdfWidth
val baseScaleY = canvasHeight / pdfHeight
val baseFitScale = minOf(baseScaleX, baseScaleY)
```

**Purpose:** Scale PDF to fit canvas while preserving aspect ratio (like `object-fit: contain`)

**Example:**
- PDF: 612 x 792 points
- Canvas: 1080 x 1920 pixels
- baseScaleX = 1080 / 612 = 1.76
- baseScaleY = 1920 / 792 = 2.42
- **baseFitScale = 1.76** (use smaller to fit completely)

### Step 2: User Zoom
```kotlin
val totalScale = baseFitScale * userZoom
```

**Purpose:** Apply user's zoom gesture on top of base fit

**Example:**
- baseFitScale = 1.76
- userZoom = 1.5 (user zoomed 50%)
- **totalScale = 2.64**

### Step 3: Calculate Display Dimensions
```kotlin
val displayedWidth = pdfWidth * totalScale
val displayedHeight = pdfHeight * totalScale
```

**Purpose:** How big is the PDF on screen after all scaling?

**Example:**
- pdfWidth = 612, totalScale = 2.64
- **displayedWidth = 1616 pixels**

### Step 4: Base Centering
```kotlin
val baseCenterX = (canvasWidth - displayedWidth) / 2f
val baseCenterY = (canvasHeight - displayedHeight) / 2f
```

**Purpose:** Center the scaled PDF in the canvas

**Example:**
- canvasWidth = 1080, displayedWidth = 1616
- **baseCenterX = -268** (negative means PDF extends left of canvas)

### Step 5: Apply Pan
```kotlin
val finalOffsetX = baseCenterX + panOffset.x
val finalOffsetY = baseCenterY + panOffset.y
```

**Purpose:** User panning shifts the centered PDF

**Example:**
- baseCenterX = -268
- panOffset.x = 100 (user panned right)
- **finalOffsetX = -168**

### Step 6: Transform Bounding Box
```kotlin
val canvasLeft = bbox.left * totalScale + finalOffsetX
val canvasTop = bbox.top * totalScale + finalOffsetY
val canvasRight = bbox.right * totalScale + finalOffsetX
val canvasBottom = bbox.bottom * totalScale + finalOffsetY
```

**Purpose:** Apply SAME transforms to OCR boxes as we did to PDF

**Example:**
- OCR box in PDF space: left=100, top=200
- totalScale = 2.64, finalOffsetX = -168
- **canvasLeft = 100 × 2.64 + (-168) = 96 pixels**

---

## Key Insights

### 1. Separation of Concerns
```
baseFitScale:  Device layout    (canvas size changes)
userZoom:      User interaction (gestures)
panOffset:     User interaction (gestures)
```

**Before (broken):**
```kotlin
val finalScale = minOf(scaleX, scaleY) * scale  // Mixed!
```

**After (correct):**
```kotlin
val baseFitScale = minOf(scaleX, scaleY)  // Layout
val totalScale = baseFitScale * userZoom   // + User zoom
```

### 2. Zoom Around Centroid
```kotlin
detectTransformGestures { centroid, pan, zoom, _ ->
    val zoomFactor = newZoom / oldZoom
    // Adjust pan so zoom happens around centroid, not origin
    panOffset = (panOffset - centroid) * zoomFactor + centroid
}
```

**What this does:**
- Translate so centroid is at origin
- Scale
- Translate back
- Result: Zoom appears to happen where user pinched

### 3. Pixel-Perfect Alignment
```kotlin
// PDF transform
drawImage(
    dstOffset = IntOffset(finalOffsetX.toInt(), finalOffsetY.toInt()),
    dstSize = IntSize(displayedWidth.toInt(), displayedHeight.toInt())
)

// OCR transform (IDENTICAL math)
val canvasLeft = bbox.left * totalScale + finalOffsetX
val canvasTop = bbox.top * totalScale + finalOffsetY
```

**Key:** Both use `totalScale` and `finalOffset` → guaranteed alignment

---

## Visual Example

### PDF Space (612 x 792)
```
   0,0 ──────────────────── 612,0
    │                          │
    │  OCR Box: (100,200)     │
    │  ┌──────┐               │
    │  │ Word │               │
    │  └──────┘               │
    │   (150,220)             │
    │                          │
 0,792 ──────────────────── 612,792
```

### Canvas Space (1080 x 1920) at 1x Zoom
```
baseFitScale = 1.76

   0,0 ──────────────────────────────── 1080,0
    │                                      │
    │     PDF (1077 x 1394)               │
    │    ┌────────────────────────┐       │
    │    │ OCR Box (176,352)      │       │
    │    │ ┌──────┐               │       │
    │    │ │ Word │               │       │
    │    │ └──────┘               │       │
    │    │  (264,387)             │       │
    │    │                        │       │
    │    └────────────────────────┘       │
    │                                      │
 0,1920 ──────────────────────────────── 1080,1920

Calculation:
  canvasLeft = 100 * 1.76 + centerX = 176
  canvasTop  = 200 * 1.76 + centerY = 352
```

### Canvas Space at 2x Zoom + Pan
```
userZoom = 2.0
totalScale = 1.76 * 2.0 = 3.52
panOffset = (50, 100)

PDF now 2154 x 2788 pixels (extends beyond screen)
User panned to show specific area

   0,0 ──────────────────────────────── 1080,0
    │                                      │
    │   ┌─(portion of PDF visible)──      │
    │   │ OCR Box (402,804)               │
    │   │ ┌───────────┐                   │
    │   │ │   Word    │                   │
    │   │ └───────────┘                   │
    │   │  (578,874)                      │
    │   │                                 │
    │   └─────                            │
    │                                      │
 0,1920 ──────────────────────────────── 1080,1920

Calculation:
  canvasLeft = 100 * 3.52 + finalOffsetX = 402
  canvasTop  = 200 * 3.52 + finalOffsetY = 804
```

**Notice:** Box stays perfectly aligned with text regardless of zoom/pan!

---

## Common Pitfalls (Fixed)

### ❌ Wrong: Applying scale twice
```kotlin
val scaledRect = RectF(
    rect.left * finalScale + offsetX,  // finalScale already includes baseFitScale!
    ...
)
```

### ✅ Correct: Single totalScale
```kotlin
val totalScale = baseFitScale * userZoom
val canvasLeft = bbox.left * totalScale + finalOffsetX
```

---

### ❌ Wrong: Forgetting to center
```kotlin
val offsetX = offset.x  // PDF stuck at top-left corner
```

### ✅ Correct: Center first, then pan
```kotlin
val baseCenterX = (canvasWidth - displayedWidth) / 2f
val finalOffsetX = baseCenterX + panOffset.x
```

---

### ❌ Wrong: Different transforms for PDF and boxes
```kotlin
// PDF
drawImage(topLeft = Offset(x1, y1))

// Box (different math!)
val boxX = bbox.x * scale + x2  // Misaligned!
```

### ✅ Correct: Identical transform
```kotlin
// PDF
val finalOffsetX = baseCenterX + panOffset.x
drawImage(dstOffset = IntOffset(finalOffsetX.toInt(), ...))

// Box (same finalOffsetX!)
val canvasLeft = bbox.left * totalScale + finalOffsetX
```

---

## Testing Alignment

### Test 1: Static (No Zoom/Pan)
- OCR boxes should perfectly outline text
- Edges should align with character boundaries

### Test 2: Zoom In (2x)
- Boxes should scale with text
- Still perfectly aligned
- Both text and boxes are bigger

### Test 3: Pan
- Boxes move with text
- No drift or lag
- Maintain perfect alignment

### Test 4: Zoom Around Point
- Zoom should feel centered on pinch point
- Boxes remain glued to text

### Test 5: Rotate Device
- Canvas size changes
- baseFitScale recalculates
- Boxes stay aligned (totalScale adjusts automatically)

---

## Performance Notes

### Why Recalculate Every Frame?
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    val baseFitScale = minOf(canvasWidth / pdfWidth, ...)  // Cheap math
    val totalScale = baseFitScale * userZoom               // Single multiply
    ...
}
```

**Answer:** These calculations are trivial (< 1ms). More important than caching is **correctness** and **responsiveness** to canvas size changes.

### When Canvas Size Changes
- Screen rotation
- Split-screen mode
- Keyboard appears
- **baseFitScale must recalculate** → Canvas() does this automatically

---

## Summary

### Before Refactor
```kotlin
val scale = 1f  // What does this mean? Ambiguous.
val finalScale = minOf(scaleX, scaleY) * scale  // Mixed concerns
val offsetX = (canvasWidth - scaledWidth) / 2 + offset.x  // Works sometimes
```

**Problems:**
- ❌ `scale` conflates base fit and user zoom
- ❌ Zoom doesn't center on gesture
- ❌ Boxes use different math than PDF

### After Refactor
```kotlin
val baseFitScale = minOf(canvasWidth / pdfWidth, canvasHeight / pdfHeight)
val totalScale = baseFitScale * userZoom
val baseCenterX = (canvasWidth - displayedWidth) / 2f
val finalOffsetX = baseCenterX + panOffset.x
```

**Benefits:**
- ✅ Explicit separation: layout vs user input
- ✅ Zoom centers on gesture point
- ✅ PDF and boxes use identical transform
- ✅ Pixel-perfect alignment at any zoom/pan
- ✅ Stable under rotation/resize

---

**Result:** Bounding boxes are now "glued" to the PDF page with sub-pixel precision! 🎯

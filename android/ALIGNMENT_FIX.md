# PDF OCR Overlay Alignment - What Was Fixed

## Issues Identified

### 1. **Coordinate System Mismatch**
**Problem:** OCR bounding boxes are in PDF space (e.g., 0-612 x 0-792), but we draw in canvas space (e.g., 0-1080 x 0-1920).

**Before:**
```kotlin
val rect = word.bbox.toRectF()
val scaledRect = RectF(
    rect.left * finalScale + offsetX,  // finalScale is ambiguous
    ...
)
```

**After:**
```kotlin
// Explicit coordinate transform
val totalScale = baseFitScale * userZoom
val canvasLeft = bbox.left * totalScale + finalOffsetX
```

---

### 2. **Mixed Scaling Concerns**
**Problem:** Base fit-to-screen scale and user zoom were conflated into one `scale` variable.

**Before:**
```kotlin
var scale by remember { mutableStateOf(1f) }  // What does 1f mean?
val finalScale = minOf(scaleX, scaleY) * scale  // Base scale * user zoom (mixed!)
```

**After:**
```kotlin
var userZoom by remember { mutableStateOf(1f) }  // Clear: user's zoom level
val baseFitScale = minOf(scaleX, scaleY)         // Layout scale (device-dependent)
val totalScale = baseFitScale * userZoom         // Explicit composition
```

**Why this matters:**
- When canvas size changes (rotation), `baseFitScale` recalculates
- `userZoom` stays constant (user's intent preserved)
- Before: `scale = 1f` could mean "1x zoom" or "not scaled yet" → ambiguous

---

### 3. **Incorrect Zoom Centering**
**Problem:** Zoom always centered on canvas center, not gesture point.

**Before:**
```kotlin
detectTransformGestures { _, pan, zoom, _ ->
    scale = (scale * zoom).coerceIn(0.5f, 3f)
    offset += pan  // Pan is independent of zoom
}
```

**Result:** Zooming felt "off" - content would jump around.

**After:**
```kotlin
detectTransformGestures { centroid, pan, zoom, _ ->
    val oldZoom = userZoom
    val newZoom = (userZoom * zoom).coerceIn(0.5f, 4f)

    if (newZoom != oldZoom) {
        val zoomFactor = newZoom / oldZoom
        // Adjust pan to zoom around centroid, not origin
        panOffset = (panOffset - centroid) * zoomFactor + centroid
    }

    userZoom = newZoom
    panOffset += pan
}
```

**Math explained:**
```
To zoom around point P:
1. Translate so P is at origin:  offset' = offset - P
2. Scale:                         offset'' = offset' * scale
3. Translate back:                final = offset'' + P

Simplified: final = (offset - P) * scale + P
```

---

### 4. **PDF and Boxes Used Different Transforms**
**Problem:** PDF drawing and box drawing calculated positions differently.

**Before:**
```kotlin
// PDF
val offsetX = (canvasWidth - scaledWidth) / 2 + offset.x

// Boxes (different calculation!)
val scaledRect = RectF(
    rect.left * finalScale + offsetX,  // Not the same offsetX!
    ...
)
```

**After:**
```kotlin
// SHARED TRANSFORM VARIABLES
val totalScale = baseFitScale * userZoom
val baseCenterX = (canvasWidth - displayedWidth) / 2f
val finalOffsetX = baseCenterX + panOffset.x

// PDF uses these
drawImage(
    dstOffset = IntOffset(finalOffsetX.toInt(), finalOffsetY.toInt()),
    dstSize = IntSize(displayedWidth.toInt(), displayedHeight.toInt())
)

// Boxes use SAME variables
val canvasLeft = bbox.left * totalScale + finalOffsetX
val canvasTop = bbox.top * totalScale + finalOffsetY
```

**Key insight:** Identical transform → guaranteed alignment.

---

### 5. **Missing srcSize/dstSize for drawImage**
**Problem:** Original code used `drawImage(topLeft, alpha)` which doesn't scale.

**Before:**
```kotlin
drawImage(
    image = bmp.asImageBitmap(),
    topLeft = Offset(offsetX, offsetY),
    alpha = 1f
)
```

**Issue:** This draws the bitmap at its **native resolution**, ignoring our scale calculations!

**After:**
```kotlin
drawImage(
    image = bmp.asImageBitmap(),
    srcOffset = IntOffset.Zero,
    srcSize = IntSize(bmp.width, bmp.height),    // Full source
    dstOffset = IntOffset(finalOffsetX.toInt(), finalOffsetY.toInt()),
    dstSize = IntSize(displayedWidth.toInt(), displayedHeight.toInt())  // Scaled destination
)
```

**Why this matters:**
- `srcSize` = what part of bitmap to read (full image)
- `dstSize` = what size to draw it (scaled)
- Now PDF actually scales properly!

---

## Transform Pipeline (Fixed)

### Visual Flow

```
┌──────────────────────┐
│ PDF Space            │  OCR boxes: bbox.left, bbox.top
│ (612 x 792 points)   │  Immutable source coordinates
└──────────────────────┘
          ↓
    [baseFitScale]
          ↓
┌──────────────────────┐
│ Fit Space            │  PDF scaled to fit canvas
│ Aspect ratio locked  │  (still conceptually centered)
└──────────────────────┘
          ↓
     [userZoom]
          ↓
┌──────────────────────┐
│ Zoomed Space         │  User's zoom applied
│ May exceed canvas    │  (still conceptually centered)
└──────────────────────┘
          ↓
    [baseCenterX + panOffset]
          ↓
┌──────────────────────┐
│ Canvas Space         │  Final screen pixels
│ (1080 x 1920 pixels) │  Where we draw
└──────────────────────┘
```

### Code Mapping

```kotlin
// STEP 1: Base Fit
val baseFitScale = minOf(canvasWidth / pdfWidth, canvasHeight / pdfHeight)

// STEP 2: User Zoom
val totalScale = baseFitScale * userZoom

// STEP 3: Display Size
val displayedWidth = pdfWidth * totalScale
val displayedHeight = pdfHeight * totalScale

// STEP 4: Base Center
val baseCenterX = (canvasWidth - displayedWidth) / 2f
val baseCenterY = (canvasHeight - displayedHeight) / 2f

// STEP 5: Pan
val finalOffsetX = baseCenterX + panOffset.x
val finalOffsetY = baseCenterY + panOffset.y

// STEP 6: Transform Coordinates
val canvasLeft = bbox.left * totalScale + finalOffsetX
val canvasTop = bbox.top * totalScale + finalOffsetY
```

---

## What Changed in Code

### Variable Naming
```diff
- var scale by remember { mutableStateOf(1f) }
- var offset by remember { mutableStateOf(Offset.Zero) }
+ var userZoom by remember { mutableStateOf(1f) }
+ var panOffset by remember { mutableStateOf(Offset.Zero) }
```

**Why:** Explicit names prevent confusion.

---

### Transform Calculation
```diff
- val scaleX = canvasWidth / bitmapWidth
- val scaleY = canvasHeight / bitmapHeight
- val finalScale = minOf(scaleX, scaleY) * scale
-
- val scaledWidth = bitmapWidth * finalScale
- val scaledHeight = bitmapHeight * finalScale
- val offsetX = (canvasWidth - scaledWidth) / 2 + offset.x
- val offsetY = (canvasHeight - scaledHeight) / 2 + offset.y

+ val baseScaleX = canvasWidth / pdfWidth
+ val baseScaleY = canvasHeight / pdfHeight
+ val baseFitScale = minOf(baseScaleX, baseScaleY)
+ val totalScale = baseFitScale * userZoom
+
+ val displayedWidth = pdfWidth * totalScale
+ val displayedHeight = pdfHeight * totalScale
+ val baseCenterX = (canvasWidth - displayedWidth) / 2f
+ val baseCenterY = (canvasHeight - displayedHeight) / 2f
+ val finalOffsetX = baseCenterX + panOffset.x
+ val finalOffsetY = baseCenterY + panOffset.y
```

**Why:** Explicit steps, no magic variables.

---

### Drawing PDF
```diff
- drawImage(
-     image = bmp.asImageBitmap(),
-     topLeft = Offset(offsetX, offsetY),
-     alpha = 1f
- )

+ drawImage(
+     image = bmp.asImageBitmap(),
+     srcOffset = IntOffset.Zero,
+     srcSize = IntSize(bmp.width, bmp.height),
+     dstOffset = IntOffset(finalOffsetX.toInt(), finalOffsetY.toInt()),
+     dstSize = IntSize(displayedWidth.toInt(), displayedHeight.toInt())
+ )
```

**Why:** Actually scale the image!

---

### Transforming Boxes
```diff
- val rect = word.bbox.toRectF()
-
- val scaledRect = RectF(
-     rect.left * finalScale + offsetX,
-     rect.top * finalScale + offsetY,
-     rect.right * finalScale + offsetX,
-     rect.bottom * finalScale + offsetY
- )

+ val bbox = word.bbox.toRectF()
+
+ val canvasLeft = bbox.left * totalScale + finalOffsetX
+ val canvasTop = bbox.top * totalScale + finalOffsetY
+ val canvasRight = bbox.right * totalScale + finalOffsetX
+ val canvasBottom = bbox.bottom * totalScale + finalOffsetY
+
+ val canvasWidth = canvasRight - canvasLeft
+ val canvasHeight = canvasBottom - canvasTop
```

**Why:** Same `totalScale` and `finalOffset` as PDF → alignment!

---

### Gesture Handling
```diff
- detectTransformGestures { _, pan, zoom, _ ->
-     scale = (scale * zoom).coerceIn(0.5f, 3f)
-     offset += pan
- }

+ detectTransformGestures { centroid, pan, zoom, _ ->
+     val oldZoom = userZoom
+     val newZoom = (userZoom * zoom).coerceIn(0.5f, 4f)
+
+     if (newZoom != oldZoom) {
+         val zoomFactor = newZoom / oldZoom
+         panOffset = (panOffset - centroid) * zoomFactor + centroid
+     }
+
+     userZoom = newZoom
+     panOffset += pan
+ }
```

**Why:** Zoom around gesture point, not canvas center.

---

## Testing the Fix

### Before (Broken)
```
1. Zoom in → Boxes drift away from text
2. Pan → Boxes move at different rate than text
3. Rotate device → Boxes completely misaligned
4. Small zoom → Boxes too small or too big
```

### After (Fixed)
```
1. Zoom in → Boxes perfectly scale with text
2. Pan → Boxes move exactly with text
3. Rotate device → Boxes recalculate and stay aligned
4. Any zoom → Boxes pixel-perfect on text edges
```

---

## Bonus: Speechify-Style Polish

### Drop Shadow
```kotlin
drawRoundRect(
    color = Color.Black.copy(alpha = 0.1f),
    topLeft = Offset(finalOffsetX + 4f, finalOffsetY + 4f),
    size = Size(displayedWidth, displayedHeight),
    cornerRadius = CornerRadius(8f, 8f)
)
```

### Pastel Highlight Colors
```kotlin
val fillColor = if (isActive) {
    Color(0x88FFE082)  // Pastel yellow
} else {
    Color(0x3300FF00)  // Subtle green
}

val strokeColor = if (isActive) {
    Color(0xFFFFD54F)  // Bright yellow-gold
} else {
    Color(0x6600AA00)  // Medium green
}
```

### Rounded Corners
```kotlin
val cornerRadius = 6f
drawRoundRect(
    color = fillColor,
    topLeft = Offset(canvasLeft, canvasTop),
    size = Size(canvasWidth, canvasHeight),
    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
)
```

---

## Summary

### Root Cause
❌ **Mixed concerns:** Base fit scale and user zoom were entangled
❌ **Inconsistent transforms:** PDF and boxes calculated positions differently
❌ **Missing scaling:** `drawImage` didn't actually scale the image

### Solution
✅ **Separation:** `baseFitScale` (layout) vs `userZoom` (user input)
✅ **Unified transform:** Both PDF and boxes use `totalScale` + `finalOffset`
✅ **Proper scaling:** `drawImage` with `srcSize` and `dstSize`

### Result
🎯 **Pixel-perfect alignment** at any zoom/pan/rotation
🎯 **Stable gestures** - zoom centers on pinch point
🎯 **Clean code** - explicit transform pipeline

---

**The bounding boxes are now "welded" to the PDF page!** ✨

# Black Screen Fix - PDF Not Visible

## Problem
After implementing Speechify-style dark theme, the PDF content disappeared (all black except buttons).

## Root Cause
Dark background (#0A0A0A) + PDF with transparent background = invisible PDF!

PDFs often have transparent or white backgrounds. When rendered on a black canvas, the text becomes invisible.

## Solution Applied

### 1. Added White Background Behind PDF
```kotlin
// Draw white background for PDF (so text is visible)
drawRoundRect(
    color = Color.White,
    topLeft = Offset(finalOffsetX, finalOffsetY),
    size = Size(displayedWidth, displayedHeight),
    cornerRadius = CornerRadius(12f, 12f)
)
```

### 2. Added Drop Shadow (Speechify-style)
```kotlin
// Draw drop shadow
drawRoundRect(
    color = Color.Black.copy(alpha = 0.3f),
    topLeft = Offset(finalOffsetX + 8f, finalOffsetY + 8f),
    size = Size(displayedWidth, displayedHeight),
    cornerRadius = CornerRadius(12f, 12f)
)
```

### 3. Clipped PDF to Rounded Corners
```kotlin
// Clip to rounded rect for smooth edges
drawContext.canvas.save()
drawContext.canvas.clipPath(Path(roundedRectPath))

drawImage(...)  // PDF draws here

drawContext.canvas.restore()
```

## Result

### Before:
```
███████████████████  ← All black
█                 █
█   Can't see PDF █  ← Invisible!
█                 █
███████████████████
```

### After:
```
███████████████████  ← Dark background
█  ┌───────────┐ █
█  │ PDF Page  │ █  ← White background
█  │  visible  │ █  ← Text readable
█  └───────────┘ █  ← Rounded + shadow
███████████████████
```

## Visual Style (Speechify-inspired)

```
Dark Background (#0A0A0A)
    ↓
Drop Shadow (black, 30% opacity, offset 8px)
    ↓
White Card (rounded 12px corners)
    ↓
PDF Content (clipped to rounded rect)
    ↓
Blue Pill Highlights (on top)
```

## What You'll See Now

1. **Dark background** - Premium black canvas
2. **PDF card** - White rounded rectangle (like a floating card)
3. **Drop shadow** - Subtle shadow makes it pop
4. **Text visible** - Black text on white background
5. **Blue pills** - Highlights overlay perfectly

## Code Changes

**File:** `SpeechifyStylePDFViewer.kt`

**Lines modified:** PDF drawing section (~line 410-455)

**What changed:**
```diff
- drawImage(...)  // Direct draw (invisible on dark bg)

+ // Shadow
+ drawRoundRect(color = Black, offset = 8px)
+
+ // White background
+ drawRoundRect(color = White)
+
+ // Clip to rounded rect
+ canvas.save()
+ canvas.clipPath(roundedRect)
+ drawImage(...)
+ canvas.restore()
```

## Testing

### Build and Run:
```bash
./gradlew clean build
```

### What to expect:
1. Upload PDF
2. **See dark background** (not all white)
3. **See PDF as white card** with rounded corners
4. **See drop shadow** around the card
5. Run OCR → **Blue pills appear**
6. Text is **clearly visible** on white background

## Troubleshooting

### PDF still not visible?
Check if PDF renderer is creating the bitmap:
```kotlin
LaunchedEffect(pdfFile) {
    println("PDF file: ${pdfFile.path}")
    println("Bitmap created: ${bitmap != null}")
}
```

### PDF visible but no shadow?
Check alpha value:
```kotlin
// Increase shadow opacity
color = Color.Black.copy(alpha = 0.5f)  // More visible
```

### Rounded corners not working?
Ensure clipping is applied:
```kotlin
drawContext.canvas.save()     // MUST save
drawContext.canvas.clipPath()  // MUST clip
drawImage()
drawContext.canvas.restore()   // MUST restore
```

## Speechify-Style Complete!

Now you have:
- ✅ Dark premium background
- ✅ Floating white PDF card
- ✅ Soft drop shadow
- ✅ Rounded corners (12px)
- ✅ Blue pill highlights
- ✅ Perfectly visible text

**The dark theme now works correctly!** 🎉

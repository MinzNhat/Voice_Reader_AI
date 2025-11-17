📄 Unified Reader Screen Refactor — Issue Description & Requirements

This document describes the current issues in the app architecture and UI, and the required refactor to unify the PDF reader and text reader into a single screen without changing their existing logic.

## ❗ 1. PDF Reader and Text Reader Are Split Into Two Separate Screens

Currently the app has:

ReaderView → for text files (TTS + working seekbar)

PDFViewerScreen → for PDF files (OCR + TTS, UI different)

➤ Problem:

When a user imports a PDF file, the app opens:

✔ PDFViewerScreen
But when the user opens the same file from Continue Listening, the app opens:
✘ ReaderView

This causes:

completely different UI

different behavior

the seekbar in PDF mode does NOT work

header/title layout mismatch

user feels like they’re using two different apps

## ❗ 2. File Name Behavior Is Incorrect

After importing:

The file name is replaced with “upload……” instead of keeping the original file name

Long file names do not scroll (no marquee)

Icons (back & settings) overlap the text

ReaderView and PDFViewer use different header layouts

This produces inconsistent UX.

## ❗ 3. The Audio Seekbar in PDF Reader Does Not Work

In ReaderView, the seekbar works correctly

In PDFViewerScreen, the seekbar cannot seek audio

This happens because:

PDFViewer uses a separate TTS + timing logic

It is not synchronized with the ReaderViewModel

User expectation: If audio is playing, seeking should always work, regardless of file type.

## ❗ 4. Fragmented UI Will Break Future Features

Later you plan to add:

reading from images

reading from OCR output

reading from clipboard

reading from multiple file formats

If PDF/Text/Image keep separate screens:

→ you will need to rewrite UI multiple times
→ rewrite TTS logic multiple times
→ rewrite seekbar logic multiple times
→ Continue Listening must handle multiple formats individually
→ Maintenance becomes very difficult

This architecture will eventually break.

# 🎯 Proposed Solution: Merge Both Screens Into One

You need to merge ReaderView and PDFViewerScreen into a single, unified screen:

# ✔ UnifiedReaderScreen

This unified screen MUST:

keep all existing logic for PDF (ViewModel untouched)

keep all existing logic for text (ViewModel untouched)

unify ONLY the UI (header, controls, seekbar, layout)

do not rewrite TTS logic

do not rewrite OCR logic

do not break existing architecture

This is a UI merge, not a logic rewrite.

## 🟦 Unified Navigation Route

Use a single route for all reading types:

reader?fileUri={uri}&type={pdf|text}

Example:

When importing a PDF:

reader?fileUri=content://xxx&type=pdf


When opening from Continue Listening:

reader?fileUri=content://xxx&type=pdf


or

reader?fileUri=content://xxx&type=text


This ensures both open the exact same screen.

## 🟩 UnifiedReaderScreen Responsibilities
Feature	PDF	Text
Load file	✔	✔
Extract text	✔ (OCR)	✔
TTS playback	✔	✔
Working seekbar	✔	✔
Header with marquee	✔	✔
Continue Listening sync	✔	✔

Result: One screen, one UI, two different data sources.

## 🟧 Required UI Behavior
✔ 1. Header Requirements

Back icon (left)

Marquee file name (center)

Settings icon (right)

If the file name is too long:

It should scroll left → right (marquee)

The beginning portion can be hidden behind the icons

✔ 2. Seekbar Requirements

Must always be seekable (same as ReaderView)

Uses unified TTS timing logic

Smooth dragging + gesture detection

✔ 3. Layout Requirements

Use the UI layout of ReaderView (cleaner + more modern)

PDF/Text only change how they load the text internally

All controls (play, pause, next, speed, voice) must be unified

**NOTE:** DO NOT CHANGE ANY LOGIC, JUST SYNC THE UI
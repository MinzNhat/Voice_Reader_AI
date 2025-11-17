📘 UI Migration Specification
Convert PdfReaderScreen UI → match ReaderScreen UI

(keep logic identical, only change UI layout)

✅ 1. Target Goal

Rewrite the UI of:

PdfReaderScreen.kt

PDFViewerScreen.kt

so that their UI layout matches the exact style and structure of:

ReaderScreen.kt 

ReaderViewModel

But:

Keep all existing logic from

PdfReaderViewModel.kt

PDFViewerViewModel.kt

And any PDF-related state & functions

Do NOT merge ViewModels

Do NOT remove PDF logic

Only rewrite the UI layer so it looks and behaves like ReaderScreen UI.

📌 2. Files Provided
PDF-related (must keep logic):

DocumentPickerScreen.kt

PdfReaderScreen.kt

PdfReaderViewModel.kt

PDFViewerScreen.kt

PDFViewerViewModel.kt

Reader UI reference (copy layout here):

ReaderScreen.kt ← main UI reference 

ReaderViewModel

ReaderViewModel.kt ← reference reading logic (do not merge) 

ReaderViewModel

TtsManager.kt ← reference for audio styling, but not for logic 

TtsManager

🎨 3. UI Components that must be duplicated (from ReaderScreen)

Claude must replicate the following UI components exactly:

✔ Top App Bar

Title centered

Back button

Settings / voice / search icons if present

✔ Content Layout

Full-screen scrollable text area

Adaptive padding

Highlight current spoken word

Selection tap areas

Same typography, colors, spacing

✔ Bottom Player Bar

Play / Pause

Forward / Rewind

Speed control

Seek by tapping a word

Same rounded shape, same shadows, same animations

✔ Colors & Theme

Use your global Theme (from theme.kt)

Same background gradient or surface colors as ReaderScreen

❗ Must remove:

Grey blank area

Default PDF layout
(the new layout must not look like a PDF canvas)

📄 A rewritten PDFViewerScreen.kt (if needed)

If this screen is still used, UI must also match ReaderScreen layout.

If PDFViewerScreen is only a viewer, then convert it into:

A simple PDF preview at top (optional)

Extracted text reader below using ReaderScreen style

🔁 5. What must NOT be changed

Claude must keep the following exactly as is:

🚫 No logic changes

No modification to ViewModel states

No modification to PDF extraction flow

No modification to TTS logic

No modification to repository/usecases

🚫 No merging ViewModels

ReaderViewModel and PdfReaderViewModel stay separate.

🚫 No new business logic added

Only UI changes.
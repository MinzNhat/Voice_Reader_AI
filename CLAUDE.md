# 1️⃣ Your Current Android Structure (branch: baonhi)

Your project already follows a lightweight version of Clean Architecture:

android/app/src/main/java/com/example/voicereaderapp/

├── domain/
│ ├── model/
│ │ ├── ReadingDocument.kt
│ │ └── DocumentType.kt
│ ├── usecase/
│ │ ├── GetAllDocumentsUseCase.kt
│ │ ├── GetDocumentByIdUseCase.kt
│ │ └── UpdateReadPositionUseCase.kt
│
├── data/
│ ├── local/
│ │ ├── dao/DocumentDao.kt
│ │ ├── entity/DocumentEntity.kt
│ │ └── AppDatabase.kt
│ └── repository/DocumentRepositoryImpl.kt
│
├── ui/
│ ├── index/ (Home screen)
│ │ ├── IndexScreen.kt
│ │ └── IndexViewModel.kt
│ ├── reader/ (Main reading screen)
│ │ ├── ReaderScreen.kt
│ │ └── ReaderViewModel.kt
│ ├── scanner/
│ ├── common/
│ │ ├── VerticalReaderPanel.kt
│ │ └── Other UI components
│ └── pdfreader/ (old version)
│
└── navigation/

markdown
Copy code

### **Your app flow:**

IndexScreen (Home)
↓ Import File
Create ReadingDocument (local)
↓
ReaderScreen(documentId)

yaml
Copy code

### ❌ BEFORE (your version):

- No OCR
- No TTS backend
- Imported PDF was only read as bytes
- Fake text was displayed (`"File imported from device..."`)

---

# 2️⃣ Your Friend’s Structure (PDF Reader + OCR uploader)

New folder your friend added:

android/app/src/main/java/com/example/voicereaderapp/ui/pdfreader/

├── DocumentPickerScreen.kt
├── PDFReaderNavigation.kt
├── PDFViewerScreen.kt
├── PDFViewerViewModel.kt
├── PdfReaderScreen.kt
├── PdfReaderViewModel.kt
├── SpeakerDialog.kt
└── SpeechifyStylePDFViewer.kt

perl
Copy code

### What these files do:

| File | Purpose |
|------|---------|
| `DocumentPickerScreen.kt` | Pick PDF from device |
| `PDFViewerScreen.kt` | Preview PDF, upload to backend |
| `SpeechifyStylePDFViewer.kt` | Fancy viewer like Speechify |
| `PDFViewerViewModel.kt` | Calls backend OCR + prepares text |
| `SpeakerDialog.kt` | Choose voice |
| `PDFReaderNavigation.kt` | Custom nav graph |

### ✔ FRIEND'S FLOW:

Pick PDF
↓
Upload to backend /ocr
↓
Receive extracted text
↓
View PDF in Speechify-style UI
↓
Send to /tts → audio

yaml
Copy code

---
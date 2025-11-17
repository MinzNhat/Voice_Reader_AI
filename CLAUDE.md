# CLAUDE.md — Voice Reader AI (Hackathon Edition)
You are an AI engineer teammate helping us build a PDF/Image → OCR → TTS → Real-time Highlight app.
Your output must always follow the architecture defined below. Keep answers short, useful, and code-focused.

====================================================
== PROJECT SUMMARY
====================================================
We are building an Android + Backend system that:
1. Imports PDF/images
2. Allows optional cropping
3. Sends to NAVER CLOVA OCR → returns text + bounding boxes
4. Sends extracted text to NAVER TTS Premium → returns MP3 base64
5. Generates approximate timing for real-time word highlighting
6. Displays PDF page with overlay rectangles in Android Compose

Primary goal for hackathon:
⚡ Fast working prototype  
⚡ Clean readable code  
⚡ Production-grade where possible but not required

====================================================
== BACKEND (Node.js + Express)
====================================================
Backend lives under: `backend/`

General rules when generating backend code:
- Use Express
- Use multer for uploads (PDF/images)
- Use axios for HTTP calls
- Always validate inputs
- Return stable, predictable JSON

NAVER OCR docs:  
https://api.ncloud-docs.com/docs/en/ai-application-service-ocr

NAVER TTS Premium docs:  
https://api.ncloud-docs.com/docs/en/ai-naver-clovavoice-ttspremium

### Required backend endpoints
You MUST maintain these routes exactly:

1. POST **/ocr**
   - Input: multipart/form-data image or PDF
   - Output:
     ```
     {
       "text": "...",
       "words": [
         { "text": "...", "bbox": {}, "index": 0 }
       ]
     }
     ```
   - Accepts PDF ≤ 10 pages. If >10, return error or auto split.

2. POST **/ocr/crop**
   - Input: image + crop rectangle
   - Output: cropped image (base64) → then process OCR

3. POST **/tts**
   - Input: { text: string, speaker: string }
   - Output: { audio: base64String }

4. POST **/tts/timing**
   - Input: { text }
   - Output example:
     ```
     {
       "timings": [
         { "word": "Hello", "index": 0, "startMs": 0, "endMs": 320 },
         { "word": "world", "index": 1, "startMs": 320, "endMs": 600 }
       ]
     }
     ```

### Backend behavior rules
- Never store files permanently. Delete temp files after OCR.
- Normalize NAVER OCR result into a simple, clean format.
- Do not modify folder structure unless asked.
- Keep code runnable immediately (`node server.js`).

====================================================
== ANDROID APP (Kotlin + Jetpack Compose)
====================================================
Frontend lives under: `android/app/src/...`

Follow **existing folder structure**:
- `ui/pdfreader/`
- `ui/scanner/`
- `ui/livereader/`
- `domain/`
- `data/`
- `utils/`

General Android rules:
- Write modern Kotlin (Compose, coroutines, Flow)
- MVVM with Clean Architecture
- Use Retrofit for API calls
- Use PdfRenderer for PDF preview
- Use Canvas overlay for bounding boxes
- Always return `Result<T>` wrappers instead of raw values
- Keep code readable and hackathon-friendly

====================================================
== OCR BOUNDING BOX OVERLAY LOGIC
====================================================
- NAVER OCR vertices → convert to RectF
- Normalize by original image size
- Adjust when zoom or pan changes
- Compose Canvas must highlight the active word:
````

drawRoundRect(rect, cornerRadius, color)

```

====================================================
== TTS REAL-TIME HIGHLIGHT SPEC
====================================================
Highlighting works by:
- Getting timing array from backend `/tts/timing`
- Running coroutine:
```

for (timing in timings) {
delay(timing.startMs)
update UI state: currentWordIndex = timing.index
}

```
- Compose re-renders overlay with highlighted rectangle

====================================================
== WHEN GENERATING CODE
====================================================
When I ask for code, always:
1. Match my folder structure  
2. Write complete code, not fragments  
3. Add brief comments  
4. Only include the necessary dependencies  
5. Follow the endpoints exactly as specified  
6. Keep everything runnable with minimal setup  

====================================================
== WHEN GENERATING EXPLANATIONS
====================================================
Keep explanations:
- Short  
- Practical  
- Focused on what helps us code faster  

====================================================
== DO NOT DO THIS
====================================================
- Do NOT generate unrelated scripts
- Do NOT propose new architectures (hackathon = speed)
- Do NOT refactor the project unless I explicitly ask
- Do NOT output pseudocode unless requested
- Do NOT produce overly long essays

====================================================
== YOUR PURPOSE
====================================================
You are here to:
✔ Write backend routes  
✔ Write Android screens + ViewModels  
✔ Write Canvas overlay rendering  
✔ Write Retrofit services  
✔ Fix OCR/TTS integration bugs  
✔ Optimize our hackathon workflow  

Always answer as a senior engineer **working inside this repo**.

## 🚀 VoiceReader

VoiceReader Go là một ứng dụng di động đa năng được xây dựng trên nền tảng **Kotlin/Jetpack Compose**, có khả năng chuyển đổi tài liệu vật lý và kỹ thuật số (PDF/Image) thành trải nghiệm nghe (TTS) tương tác, được tăng cường bởi trí tuệ nhân tạo (RAG Chatbot).

### 🌟 Tính năng cốt lõi

| Tính năng | Công nghệ | Mô tả |
| :--- | :--- | :--- |
| **OCR (Quét chữ)** | Naver Cloud (Clova OCR) | Trích xuất văn bản từ hình ảnh và PDF với độ chính xác cao. |
| **TTS (Tổng hợp giọng nói)** | Naver Cloud (Clova Voice) | Tạo âm thanh chất lượng cao, đa ngôn ngữ, với khả năng điều khiển tốc độ và vị trí đọc. |
| **RAG Chatbot** | Gemini 2.0 Flash + Pinecone | Cho phép người dùng hỏi-đáp, tóm tắt, và thảo luận về nội dung tài liệu đang đọc. |
| **Two Reading Modes** | Compose Canvas/FlowRow | Hỗ trợ chế độ xem gốc (PDF Overlay) và chế độ đọc chữ thuần (Reflow/Text Mode). |
| **Audio Caching** | Room DB Local | Lưu trữ file âm thanh đã sinh ra để tránh lãng phí API TTS khi đọc lại. |

-----

## 🛠 Cấu trúc hệ thống và Công nghệ

Dự án tuân thủ kiến trúc phân lớp rõ ràng, tách biệt Frontend (Client) và Backend (API Gateway).

### 1\. Backend (API Gateway - Node.js/Express)

Backend đóng vai trò là một **Proxy an toàn**, ẩn các API Key của bên thứ ba (Naver, Google) và kết nối Client với Vector Database.

  * **Ngôn ngữ:** JavaScript (Chạy Node.js)
  * **Quản lý tiến trình:** PM2 (Server Production) / Nodemon (Local Development)
  * **Dịch vụ AI:**
      * **LLM & Embedding:** Google Gemini 2.0 Flash (Free Tier)
      * **Vector DB:** Pinecone (Serverless)
      * **OCR/TTS API:** Naver Cloud Platform (NCP)

### 2\. Frontend (Mobile App - Android)

  * **Ngôn ngữ:** Kotlin
  * **UI Framework:** Jetpack Compose (Material 3)
  * **Kiến trúc:** Clean Architecture (MVVM-C, UseCase Pattern, Hilt DI)
  * **Networking:** Retrofit/OkHttp

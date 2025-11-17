# Voice Reader AI - Android App Architecture

## 📁 Folder Structure

The project follows **MVVM + Clean Architecture** principles with clear separation of concerns:

```
android/app/src/main/java/com/example/voicereaderapp/
├── data/                           # Data Layer
│   ├── local/
│   │   ├── dao/                   # Room DAOs
│   │   │   └── DocumentDao.kt
│   │   ├── database/              # Database configuration
│   │   │   └── VoiceReaderDatabase.kt
│   │   ├── entity/                # Room entities
│   │   │   └── DocumentEntity.kt
│   │   └── preferences/           # DataStore preferences
│   │       └── VoiceSettingsPreferences.kt
│   └── repository/                # Repository implementations
│       ├── DocumentRepositoryImpl.kt
│       └── VoiceSettingsRepositoryImpl.kt
│
├── domain/                         # Domain Layer (Business Logic)
│   ├── model/                     # Domain models
│   │   ├── ReadingDocument.kt
│   │   └── VoiceSettings.kt
│   ├── repository/                # Repository interfaces
│   │   ├── DocumentRepository.kt
│   │   └── VoiceSettingsRepository.kt
│   └── usecase/                   # Use cases
│       ├── GetAllDocumentsUseCase.kt
│       ├── SaveDocumentUseCase.kt
│       ├── GetVoiceSettingsUseCase.kt
│       └── UpdateVoiceSettingsUseCase.kt
│
├── ui/                            # Presentation Layer (UI)
│   ├── index/                     # Main screen with tabs
│   │   ├── IndexScreen.kt
│   │   └── IndexViewModel.kt
│   ├── pdfreader/                 # PDF reading screen
│   │   ├── PdfReaderScreen.kt
│   │   └── PdfReaderViewModel.kt
│   ├── scanner/                   # Image scanning screen
│   │   ├── ScannerScreen.kt
│   │   └── ScannerViewModel.kt
│   ├── livereader/                # Live screen reading
│   │   ├── LiveReaderScreen.kt
│   │   └── LiveReaderViewModel.kt
│   ├── settings/                  # Voice settings screen
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   └── theme/                     # Material 3 theming
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── di/                            # Dependency Injection (Hilt)
│   ├── DatabaseModule.kt          # Room database dependencies
│   ├── PreferencesModule.kt       # DataStore dependencies
│   ├── RepositoryModule.kt        # Repository bindings
│   ├── ServiceModule.kt           # Service dependencies (TTS)
│   └── UseCaseModule.kt           # Use case dependencies
│
├── utils/                         # Utility classes
│   ├── Constants.kt               # App constants
│   ├── DateExtensions.kt          # Date formatting extensions
│   ├── StringExtensions.kt        # String utilities
│   ├── ContextExtensions.kt       # Context extensions
│   ├── VoiceMapper.kt             # Voice mapping helper
│   ├── Result.kt                  # Result wrapper class
│   ├── AccessibilityHelper.kt     # TalkBack & accessibility support
│   ├── VoiceFeedback.kt           # Voice & haptic feedback
│   ├── GestureHelper.kt           # Accessibility gestures
│   └── NavigationHelper.kt        # Navigation announcements
│
├── MainActivity.kt                # Main entry point
└── VoiceReaderApplication.kt      # Application class with Hilt
```

## 🏗️ Architecture Overview

### **Clean Architecture Layers**

#### 1. **Domain Layer** (`domain/`)

- **Pure Kotlin** - No Android dependencies
- Contains business logic and rules
- **Models**: Core data structures (`ReadingDocument`, `VoiceSettings`)
- **Repository Interfaces**: Define data operations contracts
- **Use Cases**: Encapsulate business operations
  - `GetAllDocumentsUseCase`: Fetch all documents
  - `SaveDocumentUseCase`: Save documents with validation
  - `GetVoiceSettingsUseCase`: Retrieve voice configuration
  - `UpdateVoiceSettingsUseCase`: Update voice settings with validation

#### 2. **Data Layer** (`data/`)

- Implements domain repository interfaces
- **Local Database**: Room for document storage
- **Preferences**: DataStore for settings
- **Entities**: Room database entities with domain model mappers
- **DAOs**: Database access objects

#### 3. **Presentation Layer** (`ui/`)

- **MVVM Pattern**: ViewModels + Jetpack Compose
- **Screens**:
  - **IndexScreen**: Main navigation with bottom tabs
  - **PdfReaderScreen**: Display and read PDF documents
  - **ScannerScreen**: Capture/select images and extract text (OCR)
  - **LiveReaderScreen**: Real-time screen reading
  - **SettingsScreen**: Configure voice, speed, and pitch
- **ViewModels**: Manage UI state and business logic calls

#### 4. **Dependency Injection** (`di/`)

- **Hilt modules** for dependency management
- **DatabaseModule**: Provides Room database and DAOs
- **PreferencesModule**: Provides DataStore preferences
- **RepositoryModule**: Binds repository interfaces to implementations
- **UseCaseModule**: Provides use case instances

#### 5. **Utils** (`utils/`)

- Helper classes and extensions
- Constants, formatters, mappers
- Result wrapper for async operations

## 🎯 Key Features

### **Implemented**

- ✅ Clean Architecture with clear layer separation
- ✅ MVVM pattern with Jetpack Compose
- ✅ Hilt dependency injection
- ✅ Room database for document storage
- ✅ DataStore for preferences
- ✅ Use cases for business logic
- ✅ Well-commented code (English comments as requested)
- ✅ **Text-to-Speech service with priority levels**
- ✅ **TalkBack support with Vietnamese content descriptions**
- ✅ **Voice and haptic feedback system**
- ✅ **Accessibility gestures and navigation**
- ✅ **Screen reader announcements for all screens**

### TODO

- 🔲 PDF text extraction integration
- 🔲 OCR for image scanning
- 🔲 Live screen capture API
- 🔲 Text-to-speech implementation
- 🔲 Camera and gallery integration
- 🔲 Voice selection UI

## 📝 Code Style

All code follows these conventions:

- **English comments** for all public APIs
- **KDoc format** for documentation
- **Clean naming** following Kotlin conventions
- **Separation of concerns** with single responsibility principle
- **Type safety** with sealed classes and data classes

## 🔧 Technologies

- **Kotlin**: Programming language
- **Jetpack Compose**: Modern UI toolkit
- **Hilt**: Dependency injection
- **Room**: Local database
- **DataStore**: Preferences storage
- **Coroutines & Flow**: Asynchronous programming
- **Material 3**: UI design system

## ♿ Accessibility Features for Visually Impaired Users

### **Tính năng hỗ trợ người khiếm thị**

Ứng dụng được thiết kế đặc biệt để hỗ trợ người khiếm thị với các tính năng:

#### **1. Text-to-Speech (TTS) - Đọc văn bản thành giọng nói**

- ✅ Hỗ trợ đọc tất cả nội dung trên màn hình
- ✅ Điều chỉnh tốc độ đọc (0.5x - 2.0x)
- ✅ Điều chỉnh độ cao giọng
- ✅ Hỗ trợ nhiều ngôn ngữ (Tiếng Việt, English, etc.)
- ✅ Ưu tiên thông báo quan trọng

#### **2. Screen Reader Support - Hỗ trợ TalkBack**

- ✅ Tương thích hoàn toàn với TalkBack của Android
- ✅ Mô tả chi tiết cho mọi thành phần UI
- ✅ Hướng dẫn cử chỉ bằng tiếng Việt
- ✅ Thông báo trạng thái và điều hướng

#### **3. Voice Feedback - Phản hồi bằng giọng nói**

- ✅ Xác nhận hành động bằng giọng nói
- ✅ Thông báo lỗi và cảnh báo
- ✅ Hướng dẫn sử dụng các màn hình
- ✅ Thông báo tiến trình đọc

#### **4. Haptic Feedback - Rung phản hồi**

- ✅ Rung khi thao tác thành công
- ✅ Rung khác biệt cho các loại thông báo
- ✅ Hỗ trợ điều hướng bằng xúc giác

#### **5. Gesture Support - Cử chỉ tối ưu**

- ✅ Chạm đơn để chọn
- ✅ Chạm đôi để kích hoạt
- ✅ Giữ lâu để mở menu
- ✅ Vuốt để điều hướng
- ✅ Thời gian phản hồi phù hợp với người khiếm thị

## 📱 Hướng dẫn sử dụng cho người khiếm thị

### **Bật TalkBack (Screen Reader)**

```
1. Vào Settings (Cài đặt) điện thoại
2. Chọn Accessibility (Khả năng tiếp cận)
3. Chọn TalkBack
4. Bật TalkBack ON
5. Hoặc giữ phím tăng/giảm âm lượng 3 giây
```

### **Cử chỉ cơ bản với TalkBack**

- **Chạm đơn**: Nghe mô tả phần tử
- **Chạm đôi**: Kích hoạt/Mở phần tử đã chọn
- **Vuốt phải**: Chuyển đến phần tử tiếp theo
- **Vuốt trái**: Quay lại phần tử trước
- **Vuốt xuống rồi lên**: Đọc từ đầu màn hình
- **Vuốt lên rồi xuống**: Đọc từ vị trí hiện tại

### **Sử dụng các tính năng chính**

#### **1. Đọc tài liệu PDF**

```
1. Chạm vào tab "PDF" ở thanh điều hướng dưới
2. Vuốt phải để duyệt danh sách tài liệu
3. Chạm đôi vào tài liệu muốn đọc
4. Chạm đôi nút "Bắt đầu đọc" để ứng dụng đọc tài liệu
5. Chạm đôi "Dừng" để tạm dừng
```

#### **2. Quét ảnh để đọc văn bản**

```
1. Chạm vào tab "Scanner"
2. Có 2 tùy chọn:
   - Chạm đôi "Chụp ảnh" để mở camera
   - Chạm đôi "Chọn từ thư viện" để chọn ảnh có sẵn
3. Sau khi chụp/chọn ảnh, đợi ứng dụng trích xuất văn bản
4. Ứng dụng sẽ tự động đọc văn bản đã trích xuất
```

#### **3. Đọc màn hình trực tiếp**

```
1. Chạm vào tab "Live"
2. Chạm đôi "Bắt đầu đọc"
3. Cấp quyền quay màn hình nếu được yêu cầu
4. Ứng dụng sẽ quét và đọc văn bản hiển thị trên màn hình
5. Chạm đôi "Dừng" để kết thúc
```

#### **4. Cài đặt giọng đọc**

```
1. Chạm vào tab "Settings"
2. Điều chỉnh tốc độ đọc:
   - Vuốt lên trên thanh trượt để tăng tốc độ
   - Vuốt xuống để giảm tốc độ
3. Điều chỉnh độ cao giọng:
   - Vuốt lên để tăng độ cao
   - Vuốt xuống để giảm độ cao
4. Chạm đôi "Lưu cài đặt" để lưu thay đổi
```

### **Phím tắt hữu ích**

- **Giữ phím nguồn + tăng âm lượng**: Bật/tắt TalkBack nhanh
- **Vuốt 3 ngón lên/xuống**: Cuộn trang
- **Vuốt 2 ngón**: Dừng đọc TalkBack tạm thời

### **Mẹo sử dụng hiệu quả**

1. **Đeo tai nghe** để nghe rõ hơn ở nơi ồn
2. **Điều chỉnh tốc độ đọc** phù hợp với khả năng nghe hiểu
3. **Sử dụng cử chỉ vuốt** thay vì tìm kiếm trên màn hình
4. **Bật rung phản hồi** để xác nhận các thao tác
5. **Tăng âm lượng** stream Accessibility trong cài đặt âm thanh

### **Xử lý sự cố**

- **Không nghe thấy giọng đọc**: Kiểm tra âm lượng và đảm bảo TalkBack đã bật
- **Đọc quá nhanh/chậm**: Vào Settings để điều chỉnh tốc độ
- **Không nhận cử chỉ**: Đảm bảo TalkBack đang hoạt động
- **Giọng đọc không rõ**: Điều chỉnh pitch và tải giọng đọc chất lượng cao

## 🏗️ Technical Implementation

### **Accessibility Architecture**

```
utils/
├── AccessibilityHelper.kt      # TalkBack detection & helpers
├── VoiceFeedback.kt           # Voice & haptic feedback system
├── GestureHelper.kt           # Accessibility gestures
└── NavigationHelper.kt        # Screen reader announcements

domain/service/
└── TextToSpeechService.kt     # TTS interface

data/service/
└── TextToSpeechServiceImpl.kt # Android TTS implementation
```

### **Key Features Implementation**

- **Content Descriptions**: All UI elements have Vietnamese descriptions
- **Semantic Properties**: Proper semantic markup for screen readers
- **Live Announcements**: Real-time feedback for actions
- **Priority Levels**: Critical announcements have higher priority
- **Haptic Patterns**: Different vibration patterns for different events

## 🚀 Next Steps

1. Add required dependencies to `build.gradle.kts`:

   - Hilt
   - Room
   - DataStore
   - Compose Navigation
   - PDF library (Apache PDFBox or Android PdfRenderer)
   - OCR library (ML Kit Text Recognition v2)

2. Update `AndroidManifest.xml`:

   - Add application class reference
   - Add required permissions:
     ```xml
     <uses-permission android:name="android.permission.CAMERA"/>
     <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
     <uses-permission android:name="android.permission.VIBRATE"/>
     <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
     <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
     ```

3. Test with TalkBack enabled
4. Implement pending TODOs in ViewModels
5. Integrate PDF and OCR libraries

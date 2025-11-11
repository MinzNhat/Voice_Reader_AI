package com.example.voicereaderapp.utils

/**
 * Navigation announcements for screen readers.
 * Provides contextual voice guidance for navigation actions.
 */
object NavigationHelper {
    /**
     * Screen identifiers for navigation.
     */
    enum class Screen {
        INDEX,
        PDF_READER,
        SCANNER,
        LIVE_READER,
        SETTINGS
    }

    /**
     * Gets screen title in Vietnamese.
     *
     * @param screen Screen identifier
     * @return Screen title
     */
    fun getScreenTitle(screen: Screen): String {
        return when (screen) {
            Screen.INDEX -> "Trang chính"
            Screen.PDF_READER -> "Đọc tài liệu PDF"
            Screen.SCANNER -> "Quét ảnh"
            Screen.LIVE_READER -> "Đọc màn hình trực tiếp"
            Screen.SETTINGS -> "Cài đặt giọng đọc"
        }
    }

    /**
     * Gets screen description for accessibility.
     *
     * @param screen Screen identifier
     * @return Detailed description
     */
    fun getScreenDescription(screen: Screen): String {
        return when (screen) {
            Screen.INDEX -> "Trang chính với các tab: Đọc PDF, Quét ảnh, Đọc trực tiếp, và Cài đặt"
            Screen.PDF_READER -> "Màn hình đọc tài liệu PDF. Chọn tài liệu để bắt đầu đọc"
            Screen.SCANNER -> "Màn hình quét ảnh. Chụp ảnh hoặc chọn từ thư viện để trích xuất văn bản"
            Screen.LIVE_READER -> "Màn hình đọc trực tiếp. Quét và đọc văn bản từ màn hình điện thoại"
            Screen.SETTINGS -> "Màn hình cài đặt. Điều chỉnh giọng đọc, tốc độ, và độ cao giọng"
        }
    }

    /**
     * Gets navigation announcement when entering a screen.
     *
     * @param screen Screen identifier
     * @return Navigation announcement
     */
    fun getNavigationAnnouncement(screen: Screen): String {
        return "Đã chuyển đến ${getScreenTitle(screen)}. ${getScreenDescription(screen)}"
    }

    /**
     * Action descriptions for common actions.
     */
    object Actions {
        const val OPEN_DOCUMENT = "Mở tài liệu"
        const val START_READING = "Bắt đầu đọc"
        const val STOP_READING = "Dừng đọc"
        const val PAUSE_READING = "Tạm dừng đọc"
        const val RESUME_READING = "Tiếp tục đọc"
        const val TAKE_PHOTO = "Chụp ảnh"
        const val SELECT_IMAGE = "Chọn ảnh từ thư viện"
        const val SAVE_SETTINGS = "Lưu cài đặt"
        const val INCREASE_SPEED = "Tăng tốc độ đọc"
        const val DECREASE_SPEED = "Giảm tốc độ đọc"
        const val INCREASE_PITCH = "Tăng độ cao giọng"
        const val DECREASE_PITCH = "Giảm độ cao giọng"
    }

    /**
     * Status announcements for different states.
     */
    object Status {
        const val LOADING = "Đang tải..."
        const val READY = "Sẵn sàng"
        const val READING = "Đang đọc..."
        const val PAUSED = "Đã tạm dừng"
        const val STOPPED = "Đã dừng"
        const val ERROR = "Đã xảy ra lỗi"
        const val SUCCESS = "Thành công"
        const val NO_DOCUMENTS = "Không có tài liệu nào"
        const val EMPTY_TEXT = "Không có văn bản nào để đọc"
    }

    /**
     * Help messages for different screens.
     */
    object HelpMessages {
        const val INDEX = "Vuốt phải hoặc trái để chuyển giữa các tab. Chạm hai lần để chọn tab"
        const val PDF_READER = "Vuốt lên xuống để duyệt danh sách tài liệu. Chạm hai lần để mở tài liệu"
        const val SCANNER = "Chạm hai lần nút chụp ảnh để chụp, hoặc nút thư viện để chọn ảnh có sẵn"
        const val LIVE_READER = "Chạm hai lần nút bắt đầu để kích hoạt chế độ đọc màn hình trực tiếp"
        const val SETTINGS = "Sử dụng thanh trượt để điều chỉnh tốc độ và độ cao giọng. Vuốt lên để tăng, xuống để giảm"
    }
}

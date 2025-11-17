package com.example.voicereaderapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream

/**
 * Helper utilities for PDF handling
 */
object PDFHelper {

    /**
     * Convert PDF page to bitmap
     * @param pdfFile PDF file to render
     * @param pageIndex Page number (0-indexed)
     * @return Bitmap of the rendered page
     */
    fun renderPDFPage(pdfFile: File, pageIndex: Int = 0): Bitmap? {
        return try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val pdfRenderer = PdfRenderer(fileDescriptor)

            if (pageIndex >= pdfRenderer.pageCount) {
                pdfRenderer.close()
                fileDescriptor.close()
                return null
            }

            val page = pdfRenderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(
                page.width,
                page.height,
                Bitmap.Config.ARGB_8888
            )

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            pdfRenderer.close()
            fileDescriptor.close()

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get number of pages in PDF
     * @param pdfFile PDF file
     * @return Number of pages
     */
    fun getPageCount(pdfFile: File): Int {
        return try {
            val fileDescriptor = ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val count = pdfRenderer.pageCount
            pdfRenderer.close()
            fileDescriptor.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * Copy URI content to temporary file
     * Useful for handling PDF from file picker
     * @param context Android context
     * @param uri URI of the PDF
     * @return Temporary file with PDF content
     */
    fun copyUriToTempFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("pdf_temp", ".pdf", context.cacheDir)

            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

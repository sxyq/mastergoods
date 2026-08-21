package com.zhihuiji.feature.sales

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.zhihuiji.core.model.v2.order.SaleOrderV2Dto
import java.io.File
import java.io.FileOutputStream

/** Sends the server-generated sales receipt PDF to Android's system print service. */
internal object SaleReceiptExporter {
    fun printPdf(context: Context, order: SaleOrderV2Dto, pdf: ByteArray) {
        check(pdf.isNotEmpty()) { "小票 PDF 内容为空" }
        val receiptDirectory = File(context.cacheDir, "sale-receipts").apply { mkdirs() }
        val fileName = "sale-receipt-${order.id}-${safeFileName(order.orderNo)}.pdf"
        val pdfFile = File(receiptDirectory, fileName)
        FileOutputStream(pdfFile).use { output -> output.write(pdf) }
        val printManager = context.getSystemService(PrintManager::class.java)
            ?: error("系统打印服务不可用")
        printManager.print(
            "销售单-${safeFileName(order.orderNo)}",
            SaleReceiptPdfPrintAdapter(pdfFile, fileName),
            null,
        )
    }

    private fun safeFileName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9_-]"), "_").take(48).ifBlank { "sale-order" }

    private class SaleReceiptPdfPrintAdapter(
        private val pdfFile: File,
        private val fileName: String,
    ) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?,
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder(fileName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build(),
                oldAttributes != newAttributes,
            )
        }

        override fun onWrite(
            pages: Array<PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback,
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
                return
            }
            try {
                pdfFile.inputStream().use { input ->
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                if (cancellationSignal.isCanceled) {
                    callback.onWriteCancelled()
                } else {
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            } catch (error: Exception) {
                callback.onWriteFailed(error.message ?: "小票 PDF 写入失败")
            }
        }
    }
}

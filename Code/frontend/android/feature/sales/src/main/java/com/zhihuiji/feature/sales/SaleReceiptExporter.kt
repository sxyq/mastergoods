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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Sends the server-generated sales receipt PDF to Android's system print service. */
internal object SaleReceiptExporter {

    private const val STALE_RECEIPT_MAX_AGE_MS = 60L * 60L * 1000L

    /**
     * 写小票 PDF 到缓存（IO），再交给系统打印。文件写不得留在主线程。
     */
    suspend fun printPdf(context: Context, order: SaleOrderV2Dto, pdf: ByteArray) {
        check(pdf.isNotEmpty()) { "小票 PDF 内容为空" }
        val fileName = "sale-receipt-${order.id}-${safeFileName(order.orderNo)}.pdf"
        val receiptDirectory = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "sale-receipts").apply { mkdirs() }
            directory.listFiles()?.forEach { stale ->
                if (System.currentTimeMillis() - stale.lastModified() > STALE_RECEIPT_MAX_AGE_MS) {
                    stale.delete()
                }
            }
            directory
        }
        writePdfForPrint(File(receiptDirectory, fileName), pdf) { pdfFile ->
            val printManager = context.getSystemService(PrintManager::class.java)
                ?: error("系统打印服务不可用")
            printManager.print(
                "销售单-${safeFileName(order.orderNo)}",
                SaleReceiptPdfPrintAdapter(pdfFile, fileName),
                null,
            )
        }
    }

    /**
     * 写入 [target] 并交给 [submitPrint] 提交打印作业。
     * 打印作业成功提交之前的写入失败、提交失败或协程取消，都会删除本次生成的文件；
     * 提交成功后不再删除，缓存文件交由打印作业生命周期（SaleReceiptPdfPrintAdapter.onFinish）清理。
     */
    internal suspend fun writePdfForPrint(
        target: File,
        pdf: ByteArray,
        submitPrint: suspend (File) -> Unit,
    ) {
        var writeStarted = false
        try {
            withContext(Dispatchers.IO) {
                writeStarted = true
                FileOutputStream(target).use { output -> output.write(pdf) }
            }
            submitPrint(target)
        } catch (error: Throwable) {
            if (writeStarted) target.delete()
            throw error
        }
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
            // onWrite 在主线程回调，文件复制放到后台线程，避免阻塞 UI。
            // 不在此处删除 pdfFile：系统打印服务可能再次调用 onWrite。
            Thread({
                var cancelled = false
                var failure: String? = null
                try {
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                        if (cancellationSignal.isCanceled) {
                            cancelled = true
                        } else {
                            pdfFile.inputStream().use { input ->
                                val buffer = ByteArray(COPY_BUFFER_SIZE)
                                while (true) {
                                    if (cancellationSignal.isCanceled) {
                                        cancelled = true
                                        break
                                    }
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    output.write(buffer, 0, read)
                                }
                            }
                        }
                    }
                } catch (error: Exception) {
                    failure = error.message ?: "小票 PDF 写入失败"
                }
                when {
                    cancelled || cancellationSignal.isCanceled ->
                        callback.onWriteCancelled()
                    failure != null ->
                        callback.onWriteFailed(failure)
                    else ->
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            }, "sale-receipt-print").start()
        }

        override fun onFinish() {
            // 打印作业结束（成功/取消/失败）后清理缓存文件。
            pdfFile.delete()
        }

        private companion object {
            const val COPY_BUFFER_SIZE = 8 * 1024
        }
    }
}

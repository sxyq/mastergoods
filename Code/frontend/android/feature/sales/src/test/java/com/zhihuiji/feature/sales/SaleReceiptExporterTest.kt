package com.zhihuiji.feature.sales

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 只覆盖 printPdf 中与缓存 PDF 生命周期直接相关的部分：
 * 打印作业成功提交之前必须清理，提交成功之后交由打印作业生命周期清理。
 * 系统打印服务的回调时序不在本测试范围内。
 */
class SaleReceiptExporterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val pdf = "%PDF-1.7 sale receipt".toByteArray()

    @Test
    fun writeFailure_beforeSubmit_leavesNoFileAndSkipsSubmit() {
        runBlocking {
            val target = File(File(folder.root, "missing-dir"), "sale-receipt-write-fail.pdf")
            var submitCalled = false

            try {
                SaleReceiptExporter.writePdfForPrint(target, pdf) { submitCalled = true }
                fail("writePdfForPrint should rethrow the write failure")
            } catch (e: IOException) {
                // 预期：写入失败
            }

            assertFalse(submitCalled)
            assertFalse(target.exists())
        }
    }

    @Test
    fun submitFailure_beforePrintJob_deletesWrittenFile() {
        runBlocking {
            val target = File(folder.root, "sale-receipt-submit-fail.pdf")

            try {
                SaleReceiptExporter.writePdfForPrint(target, pdf) { error("系统打印服务不可用") }
                fail("writePdfForPrint should rethrow the submit failure")
            } catch (e: IllegalStateException) {
                // 预期：打印作业未提交
            }

            assertFalse(target.exists())
        }
    }

    @Test
    fun cancelBeforeSubmit_deletesWrittenFile() {
        runBlocking {
            val target = File(folder.root, "sale-receipt-cancelled.pdf")
            val submitStarted = CompletableDeferred<Unit>()

            val job = launch {
                SaleReceiptExporter.writePdfForPrint(target, pdf) {
                    submitStarted.complete(Unit)
                    awaitCancellation()
                }
            }
            submitStarted.await()
            assertTrue(target.exists())

            job.cancelAndJoin()

            assertFalse(target.exists())
        }
    }

    @Test
    fun cancelWhileWritingFile_leavesNoPartialFile() {
        runBlocking {
            val target = File(folder.root, "sale-receipt-cancel-during-write.pdf")
            val payload = ByteArray(64 * 1024 * 1024) { 1 }
            val job = launch(Dispatchers.Default) {
                SaleReceiptExporter.writePdfForPrint(target, payload) { awaitCancellation() }
            }
            // 文件一出现即取消：此时打印作业尚未接管，本次生成的 PDF 必须被清理。
            while (!target.exists()) yield()
            job.cancelAndJoin()

            assertFalse(target.exists())
        }
    }

    @Test
    fun submitSuccess_keepsFileForPrintLifecycle() {
        runBlocking {
            val target = File(folder.root, "sale-receipt-submitted.pdf")
            var handedOff: File? = null

            SaleReceiptExporter.writePdfForPrint(target, pdf) { handedOff = it }

            assertTrue(target.exists())
            assertArrayEquals(pdf, target.readBytes())
            assertTrue(target == handedOff)
        }
    }
}

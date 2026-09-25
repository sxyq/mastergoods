package com.zhihuiji.feature.products

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductEditUploadImageReadTest {

    @Test
    fun uploadReadFailure_usesFixedUiMessage() {
        assertEquals("读取图片失败", UploadReadUiMessages.READ_FAILURE)
    }

    @Test
    fun uploadReadFailureMapping_neverExposesRawExceptionMessage() {
        val raw = IllegalStateException("java.io.FileNotFoundException: /data/secret.png")
        assertEquals("读取图片失败", uploadReadFailureUiMessage(raw))
        assertEquals("读取图片失败", uploadReadFailureUiMessage(null))
    }
}

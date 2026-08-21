package com.zhihuiji.core.common

data class UiMessage(
    val text: String,
    val type: Type = Type.ERROR,
) {
    enum class Type { ERROR, WARNING, INFO, SUCCESS }

    companion object {
        fun fromThrowable(throwable: Throwable): UiMessage {
            val text = throwable.message ?: "未知错误"
            return UiMessage(text = text, type = Type.ERROR)
        }

        fun error(text: String) = UiMessage(text = text, type = Type.ERROR)
        fun warning(text: String) = UiMessage(text = text, type = Type.WARNING)
        fun info(text: String) = UiMessage(text = text, type = Type.INFO)
        fun success(text: String) = UiMessage(text = text, type = Type.SUCCESS)
    }
}

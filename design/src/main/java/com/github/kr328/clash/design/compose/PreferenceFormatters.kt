package com.github.kr328.clash.design.compose

fun nullableTextSummary(
    value: String?,
    placeholder: String?,
    empty: String?,
): String? {
    return when {
        value == null -> placeholder
        value.isEmpty() -> empty
        else -> value
    }
}

fun collectionSummary(
    size: Int?,
    placeholder: String,
    empty: String,
    formatElements: (Int) -> String,
): String {
    return when {
        size == null -> placeholder
        size == 0 -> empty
        else -> formatElements(size)
    }
}

fun dependencyEnabled(value: Boolean?): Boolean {
    return value != false
}

fun portToText(value: Int?): String? {
    if (value == null) {
        return null
    }

    return if (value > 0) value.toString() else ""
}

fun textToPort(value: String?): Int? {
    if (value == null) {
        return null
    }

    return value.toIntOrNull() ?: 0
}

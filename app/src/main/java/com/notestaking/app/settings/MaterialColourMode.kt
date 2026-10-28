package com.notestaking.app.settings

enum class MaterialColourMode(val id: String) {
    SYSTEM("system"), SINGLE("single"), DUAL("dual");

    companion object {
        fun fromId(id: String): MaterialColourMode? = entries.firstOrNull { it.id == id }
    }
}

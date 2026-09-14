package com.pgigi.pumpkintoolkit.animation

enum class PredictiveBackAnimation(val displayName: String) {
    None("无"),
    MiuixDefault("Miuix"),
    AOSP("AOSP"),
    Scale("缩放"),
    Classic("经典");

    companion object {
        fun fromName(name: String?): PredictiveBackAnimation =
            entries.find { it.name == name } ?: None
    }
}

enum class PredictiveBackExitDirection(val displayName: String) {
    FOLLOW_GESTURE("跟随手势"),
    ALWAYS_RIGHT("始终向右"),
    ALWAYS_LEFT("始终向左");

    companion object {
        fun fromName(name: String?): PredictiveBackExitDirection =
            entries.find { it.name == name } ?: FOLLOW_GESTURE
    }
}

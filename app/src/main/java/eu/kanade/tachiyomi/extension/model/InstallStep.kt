package eu.kanade.tachiyomi.extension.model

internal enum class InstallStep {
    Idle,
    Pending,
    Downloading,
    Installing,
    Installed,
    Error,
    ;

    fun isCompleted(): Boolean = this == Installed || this == Error || this == Idle
}

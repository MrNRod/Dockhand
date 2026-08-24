package com.mrnrod45.nstk.platform

import kotlinx.cinterop.toKString
import platform.posix.getenv

actual fun getDefaultDownloadsPath(): String {
    val home = getenv("HOME")?.toKString() ?: "/tmp"
    return "$home/Downloads"
}

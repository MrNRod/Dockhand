package com.mrnrod45.nstk.platform

actual fun getDefaultDownloadsPath(): String {
    return System.getProperty("user.home") + java.io.File.separator + "Downloads"
}

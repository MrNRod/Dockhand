package com.mrnrod45.nstk.platform

actual fun getDefaultDownloadsPath(): String {
    return android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).absolutePath
}

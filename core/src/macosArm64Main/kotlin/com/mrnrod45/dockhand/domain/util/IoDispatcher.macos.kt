package com.mrnrod45.dockhand.domain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Kotlin/Native has no dedicated IO dispatcher; Default is backed by a real
// multithreaded pool under the new memory model, which is fine for blocking I/O here.
actual val IoDispatcher: CoroutineDispatcher = Dispatchers.Default

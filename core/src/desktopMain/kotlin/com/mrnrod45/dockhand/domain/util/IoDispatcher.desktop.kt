package com.mrnrod45.dockhand.domain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val IoDispatcher: CoroutineDispatcher = Dispatchers.IO

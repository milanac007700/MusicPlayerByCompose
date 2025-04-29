package com.milanac007.demo.musicplayerbycompose.utils

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class Signal {
    private var continuation: Continuation<Unit>? = null

    suspend fun customWait() = suspendCancellableCoroutine<Unit> {
        continuation = it
    }

    fun customNotify() {
        continuation?.resume(Unit)
        continuation = null
    }
}
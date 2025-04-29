package com.milanac007.demo.musicplayerbycompose.utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 自定义一个可暂停、从剩余时间恢复的delay方法
 */
class PausableDelay {
    private var delayJob: Job? = null
    private var remainingTime: Long = 0
    private val signal: Signal = Signal()

    suspend fun delay(scope: CoroutineScope, timeMills: Long) {
        remainingTime = timeMills
        while (remainingTime > 0) {
            delayJob = scope.launch {
                println("@@@ PausableDelay, delayJob[$delayJob] need delay: $remainingTime ms")
                val startTime = System.currentTimeMillis()
                try {
                    kotlinx.coroutines.delay(remainingTime)
                    remainingTime = 0
                } catch (e: CancellationException) {
                    // 记录剩余时间
                    remainingTime -= System.currentTimeMillis() - startTime
                    println("@@@ PausableDelay,delayJob[$delayJob], e: ${e.message}, remainingTime: $remainingTime ms")
                }
            }

            delayJob?.join() // 等待job完成或被取消

            if (remainingTime > 0) {
                signal.customWait() // 被暂停，等待恢复
            }
        }
    }

    fun pause() {
        println("@@@ PausableDelay, pause delayJob: $delayJob")
        delayJob?.cancel()
    }

    fun resume() {
        if (remainingTime > 0) {
            signal.customNotify() // 重新启动delay
            println("@@@ PausableDelay, resume")
        }
    }
}
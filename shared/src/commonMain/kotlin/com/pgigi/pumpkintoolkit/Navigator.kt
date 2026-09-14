package com.pgigi.pumpkintoolkit

import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Simple navigation helper that owns a back stack and result channels.
 * Supports push/replace/pop/popUntil and result APIs: navigateForResult/setResult/observeResult/clearResult.
 */
open class Navigator(
    val backStack: NavBackStack,
) {
    private val resultBus = mutableMapOf<String, MutableSharedFlow<Any>>()

    /**
     * Optional interceptor for back navigation.
     * When set, [pop] calls the interceptor first. If it returns true, the pop is consumed
     * and the back stack is not modified. If it returns false, normal pop proceeds.
     */
    var onBackIntercept: (() -> Boolean)? = null

    /**
     * Push a key onto the back stack.
     */
    open fun push(key: NavKey) {
        backStack.add(key)
    }

    /**
     * Replace the top key, or push if the stack is empty.
     */
    open fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    /**
     * Pop the top key if present.
     */
    open fun pop() {
        if (onBackIntercept?.invoke() == true) return
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    /**
     * Pop until predicate matches the top key.
     */
    open fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.isNotEmpty() && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    /**
     * Navigate expecting a result. Caller should subscribe via observeResult(requestKey).
     */
    open fun navigateForResult(route: Route, requestKey: String) {
        ensureChannel(requestKey)
        push(route)
    }

    /**
     * Set a result for the given request and then pop.
     */
    open fun <T : Any> setResult(requestKey: String, value: T) {
        ensureChannel(requestKey).tryEmit(value)
        pop()
    }

    /**
     * Emit a result value for the given request without popping.
     */
    open fun <T : Any> emitResult(requestKey: String, value: T) {
        ensureChannel(requestKey).tryEmit(value)
    }

    /**
     * Observe results for a given request key as a SharedFlow.
     */
    @Suppress("UNCHECKED_CAST")
    open fun <T : Any> observeResult(requestKey: String): SharedFlow<T> = ensureChannel(requestKey) as SharedFlow<T>

    /**
     * Clear the last emitted result for the request key.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    open fun clearResult(requestKey: String) {
        ensureChannel(requestKey).resetReplayCache()
    }

    private fun ensureChannel(key: String): MutableSharedFlow<Any> = resultBus.getOrPut(key) { MutableSharedFlow(replay = 1, extraBufferCapacity = 0) }
}

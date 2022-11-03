package com.anagorny.gpxanimatorbot.helpers

import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T : Any> Stream<T>.middle(): Optional<T> {
    val list = this.collect(Collectors.toList())
    if (list.isEmpty()) return Optional.empty<T>()
    if (list.size == 1) return Optional.of(list.first())
    return Optional.of(list[(list.size / 2) + (list.size % 2)])
}

fun allIsNotNull(vararg args: Any?): Boolean {
    return args.asSequence()
        .filter { it != null }
        .count() == args.size
}

fun allIsNull(vararg args: Any?): Boolean {
    return args.asSequence()
        .filter { it == null }
        .count() == args.size
}


fun allIsEquals(vararg args: Any?): Boolean {
    return args.asSequence()
        .toSet().size == 1
}

fun <T> measureTimeMillis(block: () -> T): Pair<Long, T> {
    val start = System.currentTimeMillis()
    val result = block()
    return (System.currentTimeMillis() - start) to result
}

suspend fun <T> io(block: CoroutineScope.() -> T) = withContext(Dispatchers.IO + MDCContext()) { block() }
fun <T> CoroutineScope.runAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
) = async(context + MDCContext(), start, block)

fun CoroutineScope.launchAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit,
) = launch(context + MDCContext(), start, block)

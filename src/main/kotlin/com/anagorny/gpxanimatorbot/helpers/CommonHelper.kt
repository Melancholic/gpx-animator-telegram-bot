package com.anagorny.gpxanimatorbot.helpers

import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

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
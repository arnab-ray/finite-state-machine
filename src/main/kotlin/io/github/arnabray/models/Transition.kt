package io.github.arnabray.models

/**
 * This class defines a transition from one STATE to another. Tags the transition as Valid or Invalid.
 */
sealed class Transition<out T, out U, out V> {
    abstract val fromState: T
    abstract val event: U

    data class Valid<out T, out U, out V> internal constructor(
        override val fromState: T,
        override val event: U,
        val toState: T,
        val transitionEffect: V?
    ) : Transition<T, U, V>()

    data class Invalid<out T, out U, out V> internal constructor(
        override val fromState: T,
        override val event: U
    ) : Transition<T, U, V>()
}

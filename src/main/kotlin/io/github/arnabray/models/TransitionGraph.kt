package io.github.arnabray.models

/**
 * A directed graph of states. The nodes are the states and transition are the edges.
 */
data class TransitionGraph<T, U, V>(
    val initialState: T,
    val matcherStateMap: Map<EventMatcher<T, T>, State<T, U, V>>,
    val onTransitionListeners: Collection<suspend (Transition<T, U, V>) -> Unit>
) {
    class State<T, U, V> internal constructor() {
        val onEntryListeners = mutableListOf<suspend (T, U) -> Unit>()
        val transitions = linkedMapOf<EventMatcher<U, U>, (T, U) -> TransitionTo<T, V>>()
        val onExitListeners = mutableListOf<suspend (T, U) -> Unit>()

        data class TransitionTo<out T, out V> internal constructor(val toState: T, val transitionEffect: V?)
    }
}
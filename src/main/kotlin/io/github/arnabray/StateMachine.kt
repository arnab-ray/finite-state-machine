package io.github.arnabray

import io.github.arnabray.models.TransitionGraph
import io.github.arnabray.models.TransitionGraphBuilder
import io.github.arnabray.models.Transition
import java.util.concurrent.atomic.AtomicReference

/**
 * The finite state machine
 * T corresponds to State of the state machine
 * U corresponds to Event processed by the state machine
 * V corresponds to the effect of the transition
 */
class StateMachine<T : Any, U : Any, V : Any> private constructor(private val transitionGraph: TransitionGraph<T, U, V>) {

    companion object {
        fun <T : Any, U : Any, V : Any> create(init: TransitionGraphBuilder<T, U, V>.() -> Unit): StateMachine<T, U, V> {
            return create(null, init)
        }

        private fun <T : Any, U : Any, V : Any> create(
            transitionGraph: TransitionGraph<T, U, V>?,
            init: TransitionGraphBuilder<T, U, V>.() -> Unit
        ): StateMachine<T, U, V> {
            return StateMachine(TransitionGraphBuilder(transitionGraph).apply(init).build())
        }
    }

    private val stateReference = AtomicReference(transitionGraph.initialState)

    val state: T
        get() = stateReference.get()

    /**
     * This method processes an event and notifies the listeners to act on it
     */
    suspend fun processEvent(event: U): Transition<T, U, V> {
        val transition = synchronized(this) {
            val fromState = stateReference.get()
            val transition = fromState.getTransition(event)
            if (transition is Transition.Valid) {
                stateReference.set(transition.toState)
            } else {
                throw IllegalArgumentException("Invalid transition from ${fromState.getName()} state")
            }
            transition
        }
        transition.notifyOnTransition()
        with(transition) {
            with(fromState) {
                notifyOnExit(event)
            }
            with(toState) {
                notifyOnEntry(event)
            }
        }
        return transition
    }

    fun with(init: TransitionGraphBuilder<T, U, V>.() -> Unit): StateMachine<T, U, V> {
        return create(transitionGraph.copy(initialState = state), init)
    }

    /**
     * Fetches the transition effect for a given event
     */
    private fun T.getTransition(event: U): Transition<T, U, V> {
        getDefinition().transitions.forEach { (eventMatcher, createTransitionTo) ->
            if (eventMatcher.matches(event)) {
                val (toState, transitionEffect) = createTransitionTo(this, event)
                return Transition.Valid(this, event, toState, transitionEffect)
            }
        }
        return Transition.Invalid(this, event)
    }

    private fun T.getName(): String {
        return this.javaClass.simpleName
    }

    /**
     * Fetches the state definition
     */
    private fun T.getDefinition() = transitionGraph.matcherStateMap
        .filter { it.key.matches(this) }
        .map { it.value }
        .firstOrNull() ?: error("Missing definition for state ${this.javaClass.simpleName}!")

    /**
     * Notifies listener to the state transition on entry into a state
     */
    private suspend fun T.notifyOnEntry(cause: U) {
        getDefinition().onEntryListeners.forEach { it(this, cause) }
    }

    /**
     * Notifies listener to the state transition on making the transition
     */
    private suspend fun Transition<T, U, V>.notifyOnTransition() {
        transitionGraph.onTransitionListeners.forEach { it(this) }
    }

    /**
     * Notifies listener to the state transition on exiting a state
     */
    private suspend fun T.notifyOnExit(cause: U) {
        getDefinition().onExitListeners.forEach { it(this, cause) }
    }
}
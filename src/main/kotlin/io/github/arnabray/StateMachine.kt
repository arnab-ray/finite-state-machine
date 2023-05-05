package io.github.arnabray

import io.github.arnabray.models.TransitionGraph
import io.github.arnabray.models.TransitionGraphBuilder
import io.github.arnabray.models.Transition
import java.util.concurrent.atomic.AtomicReference

class StateMachine<T : Any, U : Any, V : Any> private constructor(
    private val transitionGraph: TransitionGraph<T, U, V>
) {

    companion object {
        fun <T : Any, U : Any, V : Any> create(
            init: TransitionGraphBuilder<T, U, V>.() -> Unit
        ): StateMachine<T, U, V> {
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

    private fun T.getTransition(event: U): Transition<T, U, V> {
        for ((eventMatcher, createTransitionTo) in getDefinition().transitions) {
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

    private fun T.getDefinition() = transitionGraph.matcherStateMap
        .filter { it.key.matches(this) }
        .map { it.value }
        .firstOrNull() ?: error("Missing definition for state ${this.javaClass.simpleName}!")

    private suspend fun T.notifyOnEntry(cause: U) {
        getDefinition().onEntryListeners.forEach { it(this, cause) }
    }

    private suspend fun T.notifyOnExit(cause: U) {
        getDefinition().onExitListeners.forEach { it(this, cause) }
    }

    private suspend fun Transition<T, U, V>.notifyOnTransition() {
        transitionGraph.onTransitionListeners.forEach { it(this) }
    }
}
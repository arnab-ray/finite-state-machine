package io.github.arnabray.models

class TransitionGraphBuilder<T, U, V>(
    transitionGraph: TransitionGraph<T, U, V>? = null
) {
    private var initialState = transitionGraph?.initialState
    private val stateDefinitions = LinkedHashMap(transitionGraph?.matcherStateMap ?: emptyMap())
    private val onTransitionListeners = ArrayList(transitionGraph?.onTransitionListeners ?: emptyList())

    fun initialState(initialState: T) {
        this.initialState = initialState
    }

    fun <S : T> state(
        stateEventMatcher: EventMatcher<T, S>,
        init: StateDefinitionBuilder<S>.() -> Unit
    ) {
        stateDefinitions[stateEventMatcher] = StateDefinitionBuilder<S>().apply(init).build()
    }

    inline fun <reified S : T> state(noinline init: StateDefinitionBuilder<S>.() -> Unit) {
        state(EventMatcher.any(), init)
    }

    inline fun <reified S : T> state(state: S, noinline init: StateDefinitionBuilder<S>.() -> Unit) {
        state(EventMatcher.eq<T, S>(state), init)
    }

    fun onTransition(listener: suspend (Transition<T, U, V>) -> Unit) {
        onTransitionListeners.add(listener)
    }

    fun build(): TransitionGraph<T, U, V> {
        return TransitionGraph(requireNotNull(initialState), stateDefinitions.toMap(), onTransitionListeners.toList())
    }

    inner class StateDefinitionBuilder<S : T> {

        private val stateDefinition = TransitionGraph.State<T, U, V>()

        inline fun <reified E : U> any(): EventMatcher<U, E> = EventMatcher.any()

        inline fun <reified R : U> eq(value: R): EventMatcher<U, R> = EventMatcher.eq(value)

        fun onEntry(listener: S.(U) -> Unit) = with(stateDefinition) {
            onEntryListeners.add { state, cause ->
                @Suppress("UNCHECKED_CAST")
                listener(state as S, cause)
            }
        }

        inline fun <reified E : U> on(
            event: E,
            noinline createTransitionTo: S.(E) -> TransitionGraph.State.TransitionTo<T, V>
        ) {
            return on(eq(event), createTransitionTo)
        }

        fun <E : U> on(
            eventMatcher: EventMatcher<U, E>,
            createTransitionTo: S.(E) -> TransitionGraph.State.TransitionTo<T, V>
        ) {
            stateDefinition.transitions[eventMatcher] = { state, event ->
                @Suppress("UNCHECKED_CAST")
                createTransitionTo((state as S), event as E)
            }
        }

        inline fun <reified E : U> on(
            noinline createTransitionTo: S.(E) -> TransitionGraph.State.TransitionTo<T, V>
        ) {
            return on(any(), createTransitionTo)
        }

        fun onExit(listener: S.(U) -> Unit) = with(stateDefinition) {
            onExitListeners.add { state, cause ->
                @Suppress("UNCHECKED_CAST")
                listener(state as S, cause)
            }
        }

        fun transitionTo(state: T, transitionEffect: V? = null) =
            TransitionGraph.State.TransitionTo(state, transitionEffect)

        fun S.doNotTransition(transitionEffect: V? = null) = transitionTo(this, transitionEffect)

        fun build() = stateDefinition
    }
}
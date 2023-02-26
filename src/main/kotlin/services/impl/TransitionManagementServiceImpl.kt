package services.impl

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import models.Event
import models.State
import models.Transition
import services.TransitionManagementService

class TransitionManagementServiceImpl : TransitionManagementService {

    private val transitions = HashMultimap.create<State, Transition>()

    override fun addTransition(state: State, transition: Transition) {
        transitions.put(state, transition)
    }

    override fun getTransition(state: State, event: Event): Transition? {
        return transitions.get(state).firstOrNull { it.event == event }
    }

    override fun getAllPossibleTransitions(): Multimap<State, Transition> {
        return this.transitions
    }
}
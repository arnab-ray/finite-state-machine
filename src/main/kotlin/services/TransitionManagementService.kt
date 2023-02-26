package services

import com.google.common.collect.Multimap
import models.Event
import models.State
import models.Transition

interface TransitionManagementService {
    fun addTransition(state: State, transition: Transition)

    fun getTransition(state: State, event: Event): Transition?

    fun getAllPossibleTransitions(): Multimap<State, Transition>
}
package services.impl

import models.State
import services.StateManagementService

class StateManagementServiceImpl : StateManagementService {

    private val endStates = mutableSetOf<State>()
    private val from: State

    override fun setFrom(state: State) {
        this.from = state
    }

    override fun getFrom(): String {
        this.from
    }

    override fun setEndStates(states: Set<State>) {
        this.endStates.addAll(states)
    }

    override fun getEndStates(): Set<State> {
        this.endStates
    }
}
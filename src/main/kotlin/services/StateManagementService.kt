package services

import models.State

interface StateManagementService {

    fun setFrom(state: State)

    fun getFrom(): String

    fun setEndStates(states: Set<State>)

    fun getEndStates(): Set<State>
}
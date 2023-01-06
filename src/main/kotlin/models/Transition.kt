package models

/**
 * The concrete class that defines a transition from one state to another
 */
data class Transition(
    val from: State,
    val to: State,
    val event: Event
)

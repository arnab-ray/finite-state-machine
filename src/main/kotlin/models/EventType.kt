package models

enum class EventType {
    /**
     * Gets invoked before any transition. Default behaviour is no action.
     */
    BEFORE_TRANSITION_ANY,

    /**
     * Gets invoked during any state transition. Default behaviour is no action.
     */
    TRANSITION_ANY,

    /**
     * Gets invoked after any state transition. Default behaviour is no action.
     */
    AFTER_TRANSITION_ANY,

    /**
     * Gets invoked before a state transition. Default behaviour is no action.
     */
    BEFORE_TRANSITION,

    /**
     * Gets invoked during a state transition on a certain event. Default behaviour is no action.
     */
    TRANSITION,

    /**
     * Gets invoked after a state transition. Default behaviour is no action.
     */
    AFTER_TRANSITION,

    /**
     * Gets triggered on a failure.
     */
    AFTER_FAILURE
}
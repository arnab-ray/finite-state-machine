package io.github.arnabray

import io.github.arnabray.models.Transition
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.fest.assertions.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GenericStateMachineTest {

    private companion object {
        private sealed class State {
            object A : State()
            object B : State()
            object C : State()
            object D : State()

            override fun toString(): String {
                return this.javaClass.simpleName
            }
        }

        private sealed class Event {
            object E1 : Event()
            object E2 : Event()
            object E3 : Event()
            object E4 : Event()

            override fun toString(): String {
                return this.javaClass.simpleName
            }
        }

        private sealed class TransitionEffect {
            object Effect : TransitionEffect()
        }

        interface Logger {
            fun log(message: String)
        }
    }

    private val logger = mockk<Logger>()

    private val stateMachine = StateMachine.create<State, Event, TransitionEffect> {
        initialState(State.A)
        state<State.A> {
            on<Event.E1> {
                transitionTo(State.B)
            }
            on<Event.E2> {
                transitionTo(State.C)
            }
            on<Event.E4> {
                transitionTo(State.D)
            }
            onExit {
                logger.log("Exiting state $this on event $it")
            }
        }
        state<State.B> {
            on<Event.E3> {
                transitionTo(State.C, TransitionEffect.Effect)
            }
        }
        state<State.C> {
            onEntry {
                logger.log("Entered state $this on event $it")
            }
            on<Event.E4> {
                doNotTransition()
            }
        }
        onTransition {
            val validTransition = it as? Transition.Valid ?: return@onTransition
            when (validTransition.transitionEffect) {
                else -> {}
            }
        }
    }

    @BeforeEach
    fun setUp() {
        coEvery {
            logger.log("Exiting state A on event E2")
        } just Runs
        coEvery {
            logger.log("Entered state C on event E2")
        } just Runs
        coEvery {
            logger.log("Exiting state A on event E1")
        } just Runs
        coEvery {
            logger.log("Entered state C on event E3")
        } just Runs
        coEvery {
            logger.log("Entered state C on event E4")
        } just Runs
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should throw error if FSM created without initial state`() {
        val thrown = Assertions.assertThrows(IllegalArgumentException::class.java) {
            StateMachine.create<State, Event, TransitionEffect> {}
        }

        assertThat(thrown.message).isEqualTo("Required value was null.")
    }

    @Test
    fun `should not allow transition for invalid event`() {
        val thrown = Assertions.assertThrows(IllegalArgumentException::class.java) {
            runBlocking { stateMachine.processEvent(Event.E3) }
        }

        assertThat(thrown.message).isEqualTo("Invalid transition from A state")
    }

    @Test
    fun `should throw illegal state exception for unknown state`() {
        val thrown = Assertions.assertThrows(IllegalArgumentException::class.java) {
            runBlocking { stateMachine.processEvent(Event.E3) }
        }

        assertThat(thrown.message).isEqualTo("Invalid transition from A state")
    }

    @Test
    fun `should return initial state`() {
        val state = stateMachine.state
        assertThat(state).isEqualTo(State.A)
    }

    @Test
    fun `should perform valid transition given a valid event and state`() {
        val transitionFromStateAToStateB = runBlocking { stateMachine.processEvent(Event.E1) }
        assertThat(transitionFromStateAToStateB).isEqualTo(Transition.Valid(State.A, Event.E1, State.B, null))
        assertThat(stateMachine.state).isEqualTo(State.B)

        val transitionFromStateBToStateC = runBlocking { stateMachine.processEvent(Event.E3) }
        assertThat(transitionFromStateBToStateC).isEqualTo(Transition.Valid(State.B, Event.E3, State.C, TransitionEffect.Effect))
        assertThat(stateMachine.state).isEqualTo(State.C)
    }

    @Test
    fun `should trigger listener on state change`() {
        runBlocking { stateMachine.processEvent(Event.E1) }
        coVerify {
            logger.log("Exiting state A on event E1")
        }

        val transitionFromStateBToStateC = runBlocking { stateMachine.processEvent(Event.E3) }
        assertThat(transitionFromStateBToStateC).isEqualTo(Transition.Valid(State.B, Event.E3, State.C, TransitionEffect.Effect))
        assertThat(stateMachine.state).isEqualTo(State.C)
        coVerify {
            logger.log("Entered state C on event E3")
        }

        val transition = runBlocking { stateMachine.processEvent(Event.E4) }
        assertThat(transition).isEqualTo(Transition.Valid(State.C, Event.E4, State.C, null))
        coVerify {
            logger.log("Entered state C on event E4")
        }
    }

    @Test
    fun `should trigger entry listeners on transition`() {
        runBlocking { stateMachine.processEvent(Event.E2) }
        verify {
            logger.log("Entered state C on event E2")
        }
    }

    @Test
    fun `should trigger exit listeners on transition`() {
        runBlocking { stateMachine.processEvent(Event.E2) }
        coVerify {
            logger.log("Exiting state A on event E2")
        }
    }
}
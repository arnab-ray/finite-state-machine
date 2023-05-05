package io.github.arnabray

import io.github.arnabray.models.Transition
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.fest.assertions.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class StateOfMatterStateMachineTest {

    companion object {
        const val ON_VAPORIZED_MESSAGE = "Vaporized!"
        const val ON_MELTED_MESSAGE = "Melted!"
        const val ON_CONDENSED_MESSAGE = "Condensed!"
        const val ON_FROZEN_MESSAGE = "Froze!"

        sealed class State {
            object Solid : State()
            object Liquid : State()
            object Gas : State()
        }

        sealed class Event {
            object Melt : Event()
            object Freeze : Event()
            object Vaporize : Event()
            object Condense : Event()
        }

        sealed class TransitionEffect {
            object LogMelted : TransitionEffect()
            object LogFrozen : TransitionEffect()
            object LogVaporized : TransitionEffect()
            object LogCondensed : TransitionEffect()
            object LogEntryToCondensed : TransitionEffect()
        }

        interface Logger {
            fun log(message: String)
        }
    }

    private val logger = mockk<Logger>()

    private val stateMachine = StateMachine.create<State, Event, TransitionEffect> {
        setInitialState(State.Solid)
        state<State.Solid> {
            on<Event.Melt> {
                transitionTo(State.Liquid, TransitionEffect.LogMelted)
            }
        }
        state<State.Liquid> {
            on<Event.Freeze> {
                transitionTo(State.Solid, TransitionEffect.LogFrozen)
            }
            on<Event.Vaporize> {
                transitionTo(State.Gas, TransitionEffect.LogVaporized)
            }
        }
        state<State.Gas> {
            on<Event.Condense> {
                transitionTo(State.Liquid, TransitionEffect.LogCondensed)
            }
        }
        onTransition {
            val validTransition = it as? Transition.Valid ?: return@onTransition
            when (validTransition.transitionEffect) {
                TransitionEffect.LogMelted -> logger.log(ON_MELTED_MESSAGE)
                TransitionEffect.LogFrozen -> logger.log(ON_FROZEN_MESSAGE)
                TransitionEffect.LogVaporized -> logger.log(ON_VAPORIZED_MESSAGE)
                TransitionEffect.LogCondensed -> withContext(Dispatchers.IO) { dumpLogs() }
                else -> {}
            }
        }
    }

    private suspend fun dumpLogs() {
        logger.log(ON_CONDENSED_MESSAGE)
    }



    @Test
    fun `initial state should be solid`() {
        assertThat(stateMachine.state).isEqualTo(State.Solid)
    }

    @Test
    fun `should transition to liquid state from solid state on melting`() {
        every {
            logger.log(ON_MELTED_MESSAGE)
        } just Runs

        val stateMachine = getStateMachineWithInitialState(State.Solid)

        val transition = runBlocking { stateMachine.processEvent(Event.Melt) }

        assertThat(stateMachine.state).isEqualTo(State.Liquid)
        assertThat(transition).isEqualTo(
            Transition.Valid(State.Solid, Event.Melt, State.Liquid, TransitionEffect.LogMelted)
        )

        verify {
            logger.log(ON_MELTED_MESSAGE)
        }
    }

    @Test
    fun `should not allow transition to gaseous state from solid state`() {
        val stateMachine = getStateMachineWithInitialState(State.Solid)

        val thrown = Assertions.assertThrows(IllegalArgumentException::class.java) {
            runBlocking { stateMachine.processEvent(Event.Vaporize) }
        }

        assertThat(thrown.message).isEqualTo("Invalid transition from Solid state")
    }

    @Test
    fun `should transition to solid state from liquid state on freezing`() {
        every {
            logger.log(ON_FROZEN_MESSAGE)
        } just Runs

        val stateMachine = getStateMachineWithInitialState(State.Liquid)

        val transition = runBlocking { stateMachine.processEvent(Event.Freeze) }

        assertThat(stateMachine.state).isEqualTo(State.Solid)
        assertThat(transition)
            .isEqualTo(Transition.Valid(State.Liquid, Event.Freeze, State.Solid, TransitionEffect.LogFrozen))

        verify {
            logger.log(ON_FROZEN_MESSAGE)
        }
    }

    @Test
    fun `should transition to gaseous state from liquid on vaporizing`() {
        every {
            logger.log(ON_VAPORIZED_MESSAGE)
        } just Runs

        val stateMachine = getStateMachineWithInitialState(State.Liquid)

        val transition = runBlocking { stateMachine.processEvent(Event.Vaporize) }

        assertThat(stateMachine.state).isEqualTo(State.Gas)
        assertThat(transition)
            .isEqualTo(Transition.Valid(State.Liquid, Event.Vaporize, State.Gas, TransitionEffect.LogVaporized))

        verify {
            logger.log(ON_VAPORIZED_MESSAGE)
        }
    }

    @Test
    fun `should transition to liquid state from gaseous state on condensation`() {
        every {
            logger.log(ON_CONDENSED_MESSAGE)
        } just Runs

        val stateMachine = getStateMachineWithInitialState(State.Gas)

        val transition = runBlocking { stateMachine.processEvent(Event.Condense) }

        assertThat(stateMachine.state).isEqualTo(State.Liquid)
        assertThat(transition)
            .isEqualTo(Transition.Valid(State.Gas, Event.Condense, State.Liquid, TransitionEffect.LogCondensed))

        verify {
            logger.log(ON_CONDENSED_MESSAGE)
        }
    }

    private fun getStateMachineWithInitialState(state: State): StateMachine<State, Event, TransitionEffect> {
        return stateMachine.with { setInitialState(state) }
    }
}
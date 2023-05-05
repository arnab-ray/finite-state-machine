# Finite State Machine

A finite state machine library

## Usage

Dependency

```
<dependency>
    <groupId>io.github.arnab</groupId>
    <artifactId>finite-state-machine</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Sample code usage

We can define states, events and side effects as below

```kotlin
sealed class State {
    object Solid : State()
    object Liquid : State()
    object Gas : State()
    
    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

sealed class Event {
    object Melt : Event()
    object Freeze : Event()
    object Vaporize : Event()
    object Condense : Event()
    
    override fun toString(): String {
        return this.javaClass.simpleName
    }
}

sealed class TransitionEffect {
    object LogMelted : TransitionEffect()
    object LogFrozen : TransitionEffect()
    object LogVaporized : TransitionEffect()
    object LogCondensed : TransitionEffect()
}
```

A variant of state definition can be
```kotlin
enum class State {
    A {
        override fun toString(): String {
            return "A"
        }
    },
    B {
        override fun toString(): String {
            return "B"
        }
    },
    C {
        override fun toString(): String {
            return "C"
        }
    },
    D {
        override fun toString(): String {
            return "D"
        }
    };
}

enum class Event {
    E1 {
        override fun toString(): String {
            return "E1"
        }
    },
    E2 {
        override fun toString(): String {
            return "E2"
        }
    },
    E3 {
        override fun toString(): String {
            return "E3"
        }
    },
    E4 {
        override fun toString(): String {
            return "E4"
        }
    };
}
private const val TRANSITION_EFFECT = "hello!"
```

The state machine can be defined as

```kotlin
private val stateMachine = StateMachine.create<State, Event, TransitionEffect> {
    initialState(State.Solid)
    state<State.Solid> {
        on<Event.Melt> {
            transitionTo(State.Liquid, TransitionEffect.LogMelted)
        }
        onExit {
           logger.log("Exiting state $this on event $it")
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
        onEntry {
            logger.log("Entered state $this on event $it")
        }
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
```

Events can be processed as

In a blocking function it can be invoked as

```kotlin
runBlocking { stateMachine.processEvent(Event.Melt) }
```

In a suspended method we can invoke it as

```kotlin
stateMachine.processEvent(Event.Melt)
```
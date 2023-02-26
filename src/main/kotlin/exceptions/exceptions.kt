package exceptions

class InvalidStateException(message: String) : Exception(message)

class StateNotFoundException(message: String) : Exception(message)

class InvalidTransitionException(message: String) : Exception(message)
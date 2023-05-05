package io.github.arnabray.models

class EventMatcher<T, out R : T> private constructor(private val clazz: Class<R>) {
    companion object {
        fun <T, R : T> any(clazz: Class<R>): EventMatcher<T, R> = EventMatcher(clazz)

        inline fun <T, reified R : T> any(): EventMatcher<T, R> = any(R::class.java)

        inline fun <T, reified R : T> eq(value: R): EventMatcher<T, R> = any<T, R>().where { this == value }
    }

    private val predicates = mutableListOf<(T) -> Boolean>({ clazz.isInstance(it) })

    fun where(predicate: R.() -> Boolean): EventMatcher<T, R> = apply {
        predicates.add {
            @Suppress("UNCHECKED_CAST")
            (it as R).predicate()
        }
    }

    fun matches(value: T) = predicates.all { it(value) }
}
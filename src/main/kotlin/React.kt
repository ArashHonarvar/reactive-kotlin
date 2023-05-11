import kotlin.properties.Delegates

class Reactor<T : Any>() {

    abstract inner class Cell() {
        abstract val value: T
        private val observers = mutableListOf<ComputeCell>()

        internal fun addObserver(observer: ComputeCell) {
            observers.add(observer)
        }

        internal fun notifyObservers() {
            observers.forEach {
                it.compute()
            }
        }

        internal fun fireObserversCallbacks() {
            observers.forEach {
                it.fireCallbacks()
            }
        }
    }

    inner class InputCell(v: T) : Cell() {
        override var value by Delegates.observable(v) { _, _, _ ->
            notifyObservers()
            fireObserversCallbacks()
        }
    }

    inner class ComputeCell(private vararg var cells: Cell, var transform: (List<T>) -> T) :
        Cell() {
        override lateinit var value: T
        private var callbacks = mutableListOf<SubscriptionImpl>()

        init {
            compute()
            cells.forEach { it.addObserver(this) }
        }

        internal fun compute() {
            value = transform(cells.map { it.value })
            notifyObservers()
        }

        private var previousValue = value

        internal fun fireCallbacks() {
            if (value != previousValue) {
                previousValue = value
                callbacks.forEach {
                    it.callback(value)
                }
                fireObserversCallbacks()
            }
        }

        internal fun addCallback(func: (T) -> Boolean): SubscriptionImpl {
            val sub = SubscriptionImpl(func)
            callbacks.add(sub)
            return sub
        }

        inner class SubscriptionImpl(val callback: (T) -> Boolean) : Subscription {
            override fun cancel() {
                callbacks.remove(this)
            }

        }
    }

    interface Subscription {
        fun cancel()
    }
}

package io.nacular.doodle.utils

/**
 * Created by Nicholas Eddy on 3/23/18.
 */
class Path<T>(private val items: List<T>): Iterable<T> {
    constructor(item: T): this(listOf(item))
    constructor(       ): this(listOf())

    val top    = items.firstOrNull()
    val bottom = items.lastOrNull ()
    val depth  = items.size

    private val hashCode = items.hashCode()

    val parent: Path<T>? get() = if (items.size > 1) {
        Path(items.dropLast(1))
    } else null

    operator fun get(index: Int) = items[index]

    infix fun ancestorOf(path: Path<T>): Boolean {
        var parent: Path<T>? = path

        while (parent != null && depth <= parent.depth) {
            if (equals(parent)) {
                return true
            }

            parent = parent.parent
        }

        return false
    }

    operator fun plus(node: T) = Path(items + node)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Path<*>) return false

        // Kotlin list equals is not as efficient
        return items.size == other.items.size && items == other.items
    }

    infix fun intersect(other: Path<T>): Path<T> {
        val result = mutableListOf<T>()

        items.forEachIndexed { index, item ->
            if (other.items[index] == item) {
                result += item
            }
        }

        return Path(result)
    }

    override fun hashCode() = hashCode
    override fun iterator() = items.iterator()
    override fun toString() = items.toString()
}

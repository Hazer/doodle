package io.nacular.doodle.core

import io.nacular.doodle.geometry.Point

/**
 * Created by Nicholas Eddy on 3/2/18.
 *
 * A simple [Container] View that can have children and a [Layout].
 */
open class Box: View(), Container {
    init {
        focusable = false
    }

    override fun iterator() = children.iterator()

    override var insets
        get(   ) = super.insets
        set(new) { super.insets = new }

    override var layout
        get(   ) = super.layout
        set(new) { super.layout = new }

    override var isFocusCycleRoot
        get(   ) = super.isFocusCycleRoot
        set(new) { super.isFocusCycleRoot = new }

    override val children = super.children

    public override fun ancestorOf(view: View) = super.ancestorOf(view)

    public override fun child(at: Point) = super.child(at)

    public override fun relayout() { super.relayout() }
}
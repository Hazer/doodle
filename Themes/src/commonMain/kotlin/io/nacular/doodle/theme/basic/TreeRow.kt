package io.nacular.doodle.theme.basic

import io.nacular.doodle.controls.IndexedItemVisualizer
import io.nacular.doodle.controls.tree.TreeLike
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.AffineTransform.Companion.Identity
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Color.Companion.Black
import io.nacular.doodle.drawing.Color.Companion.Green
import io.nacular.doodle.drawing.Color.Companion.White
import io.nacular.doodle.drawing.ColorBrush
import io.nacular.doodle.event.PointerEvent
import io.nacular.doodle.event.PointerListener
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.ConstraintLayout
import io.nacular.doodle.layout.Constraints
import io.nacular.doodle.layout.HorizontalConstraint
import io.nacular.doodle.layout.Insets
import io.nacular.doodle.layout.MagnitudeConstraint
import io.nacular.doodle.layout.ParentConstraints
import io.nacular.doodle.layout.constrain
import io.nacular.doodle.system.SystemInputEvent.Modifier.Ctrl
import io.nacular.doodle.system.SystemInputEvent.Modifier.Meta
import io.nacular.doodle.system.SystemInputEvent.Modifier.Shift
import io.nacular.doodle.utils.Path
import io.nacular.measured.units.Angle.Companion.degrees
import io.nacular.measured.units.times
import kotlin.math.max

/**
 * Created by Nicholas Eddy on 5/7/19.
 */

private class ConstraintWrapper(delegate: Constraints, parent: (ParentConstraints) -> ParentConstraints): Constraints by delegate {
    override val parent = parent(delegate.parent)
}

private open class ParentConstraintWrapper(delegate: ParentConstraints): ParentConstraints by delegate

abstract class TreeRowIcon: View() {
    abstract var expanded: Boolean
    abstract var selected: Boolean
}

class SimpleTreeRowIcon(private val color: Color = Black, private val selectedColor: Color = White): TreeRowIcon() {
    override var expanded = false
        set (new) {
            field = new
            rerender()
        }

    override var selected = false
        set (new) {
            field = new
            rerender()
        }

    override fun render(canvas: Canvas) {
        val transform = when {
            expanded -> Identity.rotate(Point(width / 2, height / 2), 90 * degrees)
            else     -> Identity
        }

        val centeredRect = bounds.atOrigin.inset(6.0)

        canvas.transform(transform) {
            path(listOf(
                    centeredRect.position,
                    Point(centeredRect.right, centeredRect.y + centeredRect.height / 2),
                    Point(centeredRect.x, centeredRect.bottom)),
                    ColorBrush(if (selected) selectedColor else color))
        }
    }
}

class TreeRow<T>(tree                : TreeLike,
                 node                : T,
             var path                : Path<Int>,
     private var index               : Int,
     private val itemVisualizer      : IndexedItemVisualizer<T>,
     private val selectionColor      : Color? = Green,
     private val selectionBluredColor: Color? = selectionColor,
     private val iconFactory         : () -> TreeRowIcon): View() {

    var positioner: Constraints.() -> Unit = { left = parent.left; centerY = parent.centerY }
        set(new) {
            if (field == new) {
                return
            }

            field = new

            layout = constrain(children[0]) {
                positioner(it)
            }
        }

    private  var icon        = null as TreeRowIcon?
    private  var depth       = -1
    internal var content     = itemVisualizer(node, index, null) { tree.selected(path) }
        set(new) {
            if (field != new) {
                children.batch {
                    remove(field)
                    field = new
                    add(field)
                }
                depth = -1 // force layout
            }
        }

    private  val iconWidth   = 20.0
    private  var pointerOver = false

    private val treeFocusChanged = { _:View, _:Boolean, new:Boolean ->
        if (tree.selected(index)) {
            backgroundColor = if (new) selectionColor else selectionBluredColor
        }
    }

    private lateinit var constraintLayout: ConstraintLayout

    init {
        children     += content
        styleChanged += { rerender() }
        pointerChanged += object: PointerListener {
            private var pressed   = false

            override fun entered(event: PointerEvent) {
                pointerOver = true
            }

            override fun exited(event: PointerEvent) {
                pointerOver = false
            }

            override fun pressed(event: PointerEvent) {
                pressed = true
            }

            override fun released(event: PointerEvent) {
                if (pointerOver && pressed) {
                    setOf(path).also {
                        tree.apply {
                            when {
                                Ctrl  in event.modifiers || Meta in event.modifiers -> toggleSelection(it)
                                Shift in event.modifiers && lastSelection != null -> {
                                    selectionAnchor?.let { rowFromPath(it) }?.let { anchor ->
                                        rowFromPath(path)?.let { current ->
                                            when {
                                                current < anchor  -> setSelection((current .. anchor ).reversed().toSet())
                                                anchor  < current -> setSelection((anchor  .. current).           toSet())
                                            }
                                        }
                                    }
                                }
                                else -> setSelection(it)
                            }
                        }
                    }
                }

                pressed = false
            }
        }

        update(tree, node, path, index)
    }

    private fun constrainLayout(view: View) = constrain(view) { content ->
        positioner(
            // Override the parent for content to confine it within a smaller region
            ConstraintWrapper(content) { parent ->
                object: ParentConstraintWrapper(parent) {
                    override val left  = HorizontalConstraint(this@TreeRow) { iconWidth * (1 + depth) }
                    override val width = MagnitudeConstraint (this@TreeRow) { it.width - iconWidth * (1 + depth) }
                }
            }
        )
    }

    fun update(tree: TreeLike, node: T, path: Path<Int>, index: Int) {
        this.path  = path
        this.index = index

        content = itemVisualizer(node, index, content) { tree.selected(path) }

        val newDepth = (path.depth - if (!tree.rootVisible) 1 else 0)

        if (newDepth != depth) {
            depth            = newDepth
            constraintLayout = constrainLayout(content)

            constrainIcon(icon)

            layout = constraintLayout
        }

        if (tree.isLeaf(this.path)) {
            icon?.let {
                this.children -= it
                constraintLayout.unconstrain(it)
            }
            icon = null
        } else  {
            icon = icon ?: iconFactory().apply {
                width    = iconWidth
                height   = width

                this@TreeRow.children += this

                pointerChanged += object: PointerListener {
                    private var pressed     = false
                    private var pointerOver = false

                    override fun entered(event: PointerEvent) {
                        pointerOver = true
                    }

                    override fun exited(event: PointerEvent) {
                        pointerOver = false
                    }

                    override fun pressed(event: PointerEvent) {
                        pressed     = true
                        pointerOver = true
                    }

                    override fun released(event: PointerEvent) {
                        if (pointerOver && pressed) {
                            when (tree.expanded(this@TreeRow.path)) {
                                true -> tree.collapse(this@TreeRow.path)
                                else -> tree.expand  (this@TreeRow.path)
                            }

                            event.consume()
                        }
                        pressed = false
                    }
                }

                constrainIcon(this)
            }

            icon?.apply {
                expanded = tree.expanded(path)
                selected = tree.selected(path)
            }
        }

        idealSize       = Size(children.map { it.width }.reduce { a, b -> a + b  }, children.map { it.height }.reduce { a, b -> max(a, b) })
        backgroundColor = when {
            tree.selected(index) -> {
                tree.focusChanged += treeFocusChanged

                if (tree.hasFocus) selectionColor else selectionBluredColor
            }
            else                 -> {
                tree.focusChanged -= treeFocusChanged
                null
            }
        }
    }

    override fun render(canvas: Canvas) {
        backgroundColor?.let { canvas.rect(bounds.atOrigin.inset(Insets(top = 1.0)), ColorBrush(it)) }
    }

    private fun constrainIcon(icon: TreeRowIcon?) {
        icon?.let {
            constraintLayout.constrain(it, content) { icon, label ->
                icon.right   = label.left
                icon.centerY = label.centerY
            }
        }
    }
}
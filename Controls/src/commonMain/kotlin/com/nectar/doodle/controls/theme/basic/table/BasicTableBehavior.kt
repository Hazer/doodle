package com.nectar.doodle.controls.theme.basic.table

import com.nectar.doodle.controls.ItemGenerator
import com.nectar.doodle.controls.table.Column
import com.nectar.doodle.controls.table.HeaderGeometry
import com.nectar.doodle.controls.table.MutableTable
import com.nectar.doodle.controls.table.Table
import com.nectar.doodle.controls.table.TableBehavior
import com.nectar.doodle.controls.table.TableBehavior.CellGenerator
import com.nectar.doodle.controls.table.TableBehavior.HeaderCellGenerator
import com.nectar.doodle.controls.table.TableBehavior.HeaderPositioner
import com.nectar.doodle.controls.table.TableBehavior.RowPositioner
import com.nectar.doodle.controls.theme.basic.ListPositioner
import com.nectar.doodle.controls.theme.basic.ListRow
import com.nectar.doodle.controls.theme.basic.SelectableListKeyHandler
import com.nectar.doodle.core.View
import com.nectar.doodle.drawing.Canvas
import com.nectar.doodle.drawing.Color
import com.nectar.doodle.drawing.Color.Companion.green
import com.nectar.doodle.drawing.Color.Companion.lightgray
import com.nectar.doodle.drawing.Color.Companion.white
import com.nectar.doodle.drawing.ColorBrush
import com.nectar.doodle.drawing.stripedBrush
import com.nectar.doodle.event.KeyEvent
import com.nectar.doodle.event.KeyListener
import com.nectar.doodle.event.MouseEvent
import com.nectar.doodle.event.MouseListener
import com.nectar.doodle.focus.FocusManager
import com.nectar.doodle.geometry.Rectangle
import com.nectar.doodle.utils.PropertyObserver
import com.nectar.doodle.utils.SetObserver

/**
 * Created by Nicholas Eddy on 4/8/19.
 */

open class BasicCellGenerator<T>: CellGenerator<T> {
    override fun <A> invoke(table: Table<T, *>, column: Column<A>, cell: A, row: Int, itemGenerator: ItemGenerator<A>, current: View?): View = when (current) {
        is ListRow<*> -> (current as ListRow<A>).apply { update(table, cell, row) }
        else          -> ListRow(table, cell, row, itemGenerator, selectionColor = null)
    }.apply { column.cellAlignment?.let { positioner = it } }
}

open class BasicTableBehavior<T>(
        private val focusManager         : FocusManager?,
        private val rowHeight            : Double = 20.0,
        private val headerColor          : Color? = lightgray,
                    evenRowColor         : Color? = white,
                    oddRowColor          : Color? = lightgray.lighter().lighter(),
        private val selectionColor       : Color? = green.lighter(),
        private val blurredSelectionColor: Color? = lightgray): TableBehavior<T>, MouseListener, KeyListener, SelectableListKeyHandler {

    override var bodyDirty  : ((         ) -> Unit)? = null
    override var headerDirty: ((         ) -> Unit)? = null
    override var columnDirty: ((Column<*>) -> Unit)? = null

    private val selectionChanged: SetObserver<Table<T, *>, Int> = { _,_,_ ->
        bodyDirty?.invoke()
    }

    private val focusChanged: PropertyObserver<View, Boolean> = { _,_,_ ->
        bodyDirty?.invoke()
    }

    private val canvasBrush = stripedBrush(rowHeight, evenRowColor, oddRowColor)

    private val movingColumns = mutableSetOf<Column<*>>()

    override val cellGenerator = BasicCellGenerator<T>()

    override val headerPositioner = object: HeaderPositioner<T> {
        override fun invoke(table: Table<T, *>) = HeaderGeometry(0.0, 1.1 * rowHeight)
    }

    override val rowPositioner = object: RowPositioner<T> {
        private val delegate = ListPositioner(rowHeight)

        override fun invoke(table: Table<T, *>, row: T, index: Int) = delegate(table, table.insets, index)
        override fun rowFor(table: Table<T, *>, y: Double)          = delegate.rowFor(table.insets, y)
    }

    override val headerCellGenerator = object: HeaderCellGenerator<T> {
        override fun <A> invoke(table: Table<T, *>, column: Column<A>) = TableHeaderCell(column, headerColor).apply { column.headerAlignment?.let { positioner = it } }
    }

    override fun renderHeader(table: Table<T, *>, canvas: Canvas) {
        headerColor?.let { canvas.rect(Rectangle(size = canvas.size), ColorBrush(it)) }
    }

    override fun renderBody(table: Table<T, *>, canvas: Canvas) {
        canvas.rect(Rectangle(size = canvas.size), canvasBrush)

        val color = if (table.hasFocus) selectionColor else blurredSelectionColor

        if (color != null) {
            table.selection.map { it to table[it] }.forEach { (index, row) ->
                row?.let {
                    canvas.rect(rowPositioner(table, row, index), ColorBrush(color))
                }
            }
        }
    }

    override fun <A> renderColumnBody(table: Table<T, *>, column: Column<A>, canvas: Canvas) {
        if (column in movingColumns && headerColor != null) {
            canvas.rect(Rectangle(size = canvas.size), ColorBrush(headerColor.with(0.2f)))
        }
    }

    // FIXME: Centralize
    override fun install(view: Table<T, *>) {
        view.keyChanged       += this
        view.mouseChanged     += this
        view.focusChanged     += focusChanged
        view.selectionChanged += selectionChanged
    }

    override fun uninstall(view: Table<T, *>) {
        view.keyChanged       -= this
        view.mouseChanged     -= this
        view.focusChanged     -= focusChanged
        view.selectionChanged -= selectionChanged
    }

    override fun mousePressed(event: MouseEvent) {
        focusManager?.requestFocus(event.source)
    }

    override fun keyPressed(event: KeyEvent) {
        super<SelectableListKeyHandler>.keyPressed(event)
    }

    override fun <A> columnMoveStart(table: Table<T, *>, column: Column<A>) {
        if (headerColor == null) {
            return
        }

        movingColumns += column

        columnDirty?.invoke(column)
    }

    override fun <A> columnMoveEnd(table: Table<T, *>, column: Column<A>) {
        if (headerColor == null) {
            return
        }

        movingColumns -= column

        columnDirty?.invoke(column)
    }
}

class BasicMutableTableBehavior<T>(
        focusManager         : FocusManager?,
        rowHeight            : Double = 20.0,
        headerColor          : Color? = lightgray,
        evenRowColor         : Color? = white,
        oddRowColor          : Color? = lightgray.lighter().lighter(),
        selectionColor       : Color? = green.lighter(),
        blurredSelectionColor: Color? = lightgray): BasicTableBehavior<T>(focusManager, rowHeight, headerColor, evenRowColor, oddRowColor, selectionColor, blurredSelectionColor) {

    override val cellGenerator = object: BasicCellGenerator<T>() {
        override fun <A> invoke(table: Table<T, *>, column: Column<A>, cell: A, row: Int, itemGenerator: ItemGenerator<A>, current: View?) = super.invoke(table, column, cell, row, itemGenerator, current).also {
            if (current !is ListRow<*>) {
                val result = it as ListRow<*>

                it.mouseChanged += object: MouseListener {
                    override fun mouseReleased(event: MouseEvent) {
                        if (event.clickCount == 2) {
                            (table as? MutableTable)?.startEditing(result.index, column)
                        }
                    }
                }
            }
        }
    }
}
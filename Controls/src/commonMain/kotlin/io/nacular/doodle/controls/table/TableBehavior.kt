package io.nacular.doodle.controls.table

import io.nacular.doodle.controls.IndexedItemVisualizer
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.core.Behavior
import io.nacular.doodle.utils.Completable
import io.nacular.doodle.utils.NoOpCompletable
import io.nacular.doodle.utils.Path

/**
 * Created by Nicholas Eddy on 4/6/19.
 */
data class HeaderGeometry(val y: Double, val height: Double)


interface TableBehavior<T>: Behavior<Table<T, *>> {
    interface CellGenerator<T> {
        operator fun <A> invoke(table: Table<T, *>, column: Column<A>, cell: A, row: Int, itemGenerator: IndexedItemVisualizer<A>, current: View? = null): View
    }

    interface RowPositioner<T> {
        operator fun invoke(table: Table<T, *>, row: T, index: Int): Rectangle

        fun rowFor(table: Table<T, *>, y: Double): Int
    }

    interface HeaderPositioner<T> {
        operator fun invoke(table: Table<T, *>): HeaderGeometry
    }

    interface HeaderCellGenerator<T> {
        operator fun <A> invoke(table: Table<T, *>, column: Column<A>): View
    }

    val cellGenerator      : CellGenerator<T>
    val rowPositioner      : RowPositioner<T>
    val headerPositioner   : HeaderPositioner<T>
    val headerCellGenerator: HeaderCellGenerator<T>

    var bodyDirty  : ((         ) -> Unit)?
    var headerDirty: ((         ) -> Unit)?
    var columnDirty: ((Column<*>) -> Unit)?

    fun moveColumn(table: Table<T, *>, block: (Float) -> Unit): Completable = NoOpCompletable.also { block(1f) }

    fun <A> columnMoveStart(table: Table<T, *>, column: Column<A>) {}
    fun <A> columnMoved    (table: Table<T, *>, column: Column<A>) {}
    fun <A> columnMoveEnd  (table: Table<T, *>, column: Column<A>) {}

    fun renderHeader        (table: Table<T, *>,                    canvas: Canvas) {}
    fun renderBody          (table: Table<T, *>,                    canvas: Canvas) {}
    fun <A> renderColumnBody(table: Table<T, *>, column: Column<A>, canvas: Canvas) {}
}


interface TreeTableBehavior<T>: Behavior<TreeTable<T, *>> {
    interface TreeCellGenerator<T> {
        operator fun <A> invoke(table: TreeTable<T, *>, column: Column<A>, cell: A, path: Path<Int>, row: Int, itemGenerator: IndexedItemVisualizer<A>, current: View? = null): View
    }

    interface CellGenerator<T> {
        operator fun <A> invoke(table: TreeTable<T, *>, column: Column<A>, cell: A, path: Path<Int>, row: Int, itemGenerator: IndexedItemVisualizer<A>, current: View? = null): View
    }

    interface RowPositioner<T> {
        operator fun invoke(table: TreeTable<T, *>, path: Path<Int>, row: T, index: Int): Rectangle

        fun rowFor(table: TreeTable<T, *>, y: Double): Int
    }

    interface HeaderPositioner<T> {
        operator fun invoke(table: TreeTable<T, *>): HeaderGeometry
    }

    interface HeaderCellGenerator<T> {
        operator fun <A> invoke(table: TreeTable<T, *>, column: Column<A>): View
    }

    val treeCellGenerator  : TreeCellGenerator<T>
    val cellGenerator      : CellGenerator<T>
    val rowPositioner      : RowPositioner<T>
    val headerPositioner   : HeaderPositioner<T>
    val headerCellGenerator: HeaderCellGenerator<T>

    var bodyDirty  : ((         ) -> Unit)?
    var headerDirty: ((         ) -> Unit)?
    var columnDirty: ((Column<*>) -> Unit)?

    fun moveColumn(table: TreeTable<T, *>, block: (Float) -> Unit): Completable = NoOpCompletable.also { block(1f) }

    fun <A> columnMoveStart(table: TreeTable<T, *>, column: Column<A>) {}
    fun <A> columnMoved    (table: TreeTable<T, *>, column: Column<A>) {}
    fun <A> columnMoveEnd  (table: TreeTable<T, *>, column: Column<A>) {}

    fun renderHeader        (table: TreeTable<T, *>, canvas: Canvas) {}
    fun renderBody          (table: TreeTable<T, *>, canvas: Canvas) {}
    fun <A> renderColumnBody(table: TreeTable<T, *>, column: Column<A>, canvas: Canvas) {}
}
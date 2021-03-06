package io.nacular.doodle.controls.table

import io.nacular.doodle.controls.IndexedItemVisualizer
import io.nacular.doodle.controls.ListModel
import io.nacular.doodle.controls.ListSelectionManager
import io.nacular.doodle.controls.Selectable
import io.nacular.doodle.controls.SelectionModel
import io.nacular.doodle.controls.SimpleListModel
import io.nacular.doodle.controls.list.ListBehavior
import io.nacular.doodle.controls.list.ListLike
import io.nacular.doodle.controls.panels.ScrollPanel
import io.nacular.doodle.core.Box
import io.nacular.doodle.core.Layout
import io.nacular.doodle.core.PositionableContainer
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.event.PointerEvent
import io.nacular.doodle.event.PointerListener
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.layout.Constraints
import io.nacular.doodle.layout.constant
import io.nacular.doodle.layout.constrain
import io.nacular.doodle.system.SystemInputEvent.Modifier.Ctrl
import io.nacular.doodle.system.SystemInputEvent.Modifier.Meta
import io.nacular.doodle.system.SystemInputEvent.Modifier.Shift
import io.nacular.doodle.utils.Completable
import io.nacular.doodle.utils.Pool
import io.nacular.doodle.utils.SetObserver
import io.nacular.doodle.utils.SetPool
import kotlin.math.max

open class Table<T, M: ListModel<T>>(
        protected val model         : M,
        protected val selectionModel: SelectionModel<Int>? = null,
        private   val scrollCache   : Int                  = 10,
                      block         : ColumnFactory<T>.() -> Unit): View(), ListLike, Selectable<Int> by ListSelectionManager(selectionModel, { model.size }) {

    private inner class ColumnFactoryImpl: ColumnFactory<T> {
        override fun <R> column(header: View?, extractor: Extractor<T, R>, cellVisualizer: CellVisualizer<R>, builder: ColumnBuilder.() -> Unit) = ColumnBuilderImpl().run {
            builder(this)

            InternalListColumn(header, headerAlignment, cellVisualizer, cellAlignment, width, minWidth, maxWidth, extractor).also { internalColumns += it }
        }
    }

    internal inner class TableLikeWrapper: TableLike {
        val delegate get() = this@Table

        override val width            get() = this@Table.width
        override val columns          get() = this@Table.columns
        override val internalColumns  get() = this@Table.internalColumns
        override val columnSizePolicy get() = this@Table.columnSizePolicy
        override val header           get() = this@Table.header as Box
        override val panel            get() = this@Table.panel

        override var resizingCol get() = this@Table.resizingCol
            set(new) {
                this@Table.resizingCol = new
            }

        override fun relayout() {
            this@Table.relayout()
        }
    }

    internal inner class TableLikeBehaviorWrapper: TableLikeBehavior<TableLikeWrapper> {
        val delegate get() = this@Table.behavior

        override fun <B: TableLikeBehavior<TableLikeWrapper>, R> columnMoveStart(table: TableLikeWrapper, internalColumn: InternalColumn<TableLikeWrapper, B, R>) {
            behavior?.columnMoveStart(table.delegate, internalColumn)
        }

        override fun <B: TableLikeBehavior<TableLikeWrapper>, R> columnMoveEnd(table: TableLikeWrapper, internalColumn: InternalColumn<TableLikeWrapper, B, R>) {
            behavior?.columnMoveEnd(table.delegate, internalColumn)
        }

        override fun <B: TableLikeBehavior<TableLikeWrapper>, R> columnMoved(table: TableLikeWrapper, internalColumn: InternalColumn<TableLikeWrapper, B, R>) {
            behavior?.columnMoved(table.delegate, internalColumn)
        }

        override fun moveColumn(table: TableLikeWrapper, function: (Float) -> Unit): Completable? = behavior?.moveColumn(table.delegate, function)
    }

    internal inner class InternalListColumn<R>(
            header         : View?,
            headerAlignment: (Constraints.() -> Unit)? = null,
            itemVisualizer : CellVisualizer<R>,
            cellAlignment  : (Constraints.() -> Unit)? = null,
            preferredWidth : Double?                   = null,
            minWidth       : Double                    = 0.0,
            maxWidth       : Double?                   = null,
            extractor      : Extractor<T, R>): InternalColumn<TableLikeWrapper, TableLikeBehaviorWrapper, R>(TableLikeWrapper(), TableLikeBehaviorWrapper(), header, headerAlignment, itemVisualizer, cellAlignment, preferredWidth, minWidth, maxWidth) {

        private inner class FieldModel<A>(private val model: M, private val extractor: Extractor<T, A>): ListModel<A> {
            override val size get() = model.size

            override fun get(index: Int) = model[index]?.let(extractor)

            override fun section(range: ClosedRange<Int>) = model.section(range).map(extractor)

            override fun contains(value: A) = value in model.map(extractor)

            override fun iterator() = model.map(extractor).iterator()
        }

        override val view: io.nacular.doodle.controls.list.List<R, *> = io.nacular.doodle.controls.list.List(FieldModel(model, extractor), object: IndexedItemVisualizer<R> {
            override fun invoke(item: R, index: Int, previous: View?, isSelected: () -> Boolean) = object: View() {}
        }, selectionModel, scrollCache = scrollCache, fitContent = false).apply {
            acceptsThemes = false
        }

        override fun behavior(behavior: TableLikeBehaviorWrapper?) {
            behavior?.delegate?.let {
                view.behavior = object: ListBehavior<R> {
                    override val generator get() = object: ListBehavior.RowGenerator<R> {
                        override fun invoke(list: io.nacular.doodle.controls.list.List<R, *>, row: R, index: Int, current: View?) = it.cellGenerator(this@Table, this@InternalListColumn, row, index, object: IndexedItemVisualizer<R> {
                            override fun invoke(item: R, index: Int, previous: View?, isSelected: () -> Boolean) = this@InternalListColumn.cellGenerator(this@InternalListColumn, item, index, previous, isSelected)
                        }, current)
                    }

                    override val positioner get() = object: ListBehavior.RowPositioner<R> {
                        override fun invoke(list: io.nacular.doodle.controls.list.List<R, *>, row: R, index: Int) = it.rowPositioner.invoke(this@Table, model[index]!!, index).run { Rectangle(0.0, y, list.width, height) }

                        override fun rowFor(list: io.nacular.doodle.controls.list.List<R, *>, y: Double) = it.rowPositioner.rowFor(this@Table, y)
                    }

                    override fun render(view: io.nacular.doodle.controls.list.List<R, *>, canvas: Canvas) {
                        if (this@InternalListColumn != internalColumns.last()) {
                            it.renderColumnBody(this@Table, this@InternalListColumn, canvas)
                        }
                    }
                }
            }
        }
    }

    internal inner class LastColumn: InternalColumn<TableLikeWrapper, TableLikeBehaviorWrapper, Unit>(TableLikeWrapper(), TableLikeBehaviorWrapper(), null, null, object: CellVisualizer<Unit> {
        override fun invoke(column: Column<Unit>, item: Unit, row: Int, previous: View?, isSelected: () -> Boolean) = previous ?: object: View() {}
    }, null, null, 0.0, null) {
        // FIXME: Can this be done by the Table Behavior?
        override val view = object: View() {

            init {
                pointerChanged += object: PointerListener {
                    private var pointerOver    = false
                    private var pointerPressed = false

                    override fun entered(event: PointerEvent) {
                        pointerOver = true
                    }

                    override fun exited(event: PointerEvent) {
                        pointerOver = false
                    }

                    override fun pressed(event: PointerEvent) {
                        pointerPressed = true
                    }

                    override fun released(event: PointerEvent) {
                        if (pointerOver && pointerPressed) {
                            behavior?.delegate?.let {
                                val index = it.rowPositioner.rowFor(this@Table, event.location.y)

                                if (index < this@Table.numRows) {
                                    setOf(index).also {
                                        this@Table.apply {
                                            when {
                                                Ctrl in event.modifiers || Meta in event.modifiers -> toggleSelection(it)
                                                Shift in event.modifiers && lastSelection != null  -> {
                                                    selectionAnchor?.let { anchor ->
                                                        when {
                                                            index < anchor  -> setSelection((index.. anchor ).reversed().toSet())
                                                            anchor  < index -> setSelection((anchor  ..index).           toSet())
                                                        }
                                                    }
                                                }
                                                else                                               -> setSelection(it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        pointerPressed = false
                    }
                }
            }
        }

        private var behavior: TableLikeBehaviorWrapper? = null

        override fun behavior(behavior: TableLikeBehaviorWrapper?) { this.behavior = behavior }
    }

    val numRows get() = model.size
    val isEmpty get() = model.isEmpty

    var columnSizePolicy: ColumnSizePolicy = ConstrainedSizePolicy()
        set(new) {
            field = new

            doLayout()
        }

    var behavior = null as TableBehavior<T>?
        set(new) {
            if (new == behavior) { return }

            field?.let {
                it.bodyDirty   = null
                it.headerDirty = null
                it.columnDirty = null

                it.uninstall(this)
            }

            field = new

            new?.also { behavior ->

                block?.let {
                    factory.apply(it)

                    // Last, unusable column
                    internalColumns += LastColumn()

                    children += listOf(header, panel)

                    block = null
                }

                behavior.bodyDirty   = bodyDirty
                behavior.headerDirty = headerDirty
                behavior.columnDirty = columnDirty

                (internalColumns as MutableList<InternalColumn<TableLikeWrapper, TableLikeBehaviorWrapper, *>>).forEach {
                    it.behavior(TableLikeBehaviorWrapper())
                }

                behavior.install(this)

                header.children.batch {
                    clear()

                    headerItemsToColumns.clear()

                    addAll(internalColumns.dropLast(1).map { column ->
                        behavior.headerCellGenerator(this@Table, column).also {
                            headerItemsToColumns[it] = column
                        }
                    })
                }

                behavior.headerPositioner.invoke(this@Table).apply {
                    header.height = height
                }

                layout = constrain(header, panel) { header, panel ->
                    behavior.headerPositioner.invoke(this@Table).apply {
                        header.top    = header.parent.top + y
                        header.height = constant(height)
                    }

                    panel.top    = header.bottom
                    panel.left   = panel.parent.left
                    panel.right  = panel.parent.right
                    panel.bottom = panel.parent.bottom
                }
            }
        }

    val columns: List<Column<*>> get() = internalColumns.dropLast(1)

    val selectionChanged: Pool<SetObserver<Int>> = SetPool()

    fun contains(value: T) = value in model

    internal val internalColumns = mutableListOf<InternalColumn<*, *, *>>()

    protected open val factory: ColumnFactory<T> = ColumnFactoryImpl()

    private var block: (ColumnFactory<T>.() -> Unit)? = block

    private val headerItemsToColumns = mutableMapOf<View, InternalColumn<*,*,*>>()

    private inner class Header: Box() {
        init {
            layout = object: Layout {
                override fun layout(container: PositionableContainer) {
                    var x          = 0.0
                    var totalWidth = 0.0

                    container.children.forEachIndexed { index, view ->
                        view.bounds = Rectangle(Point(x, 0.0), Size(internalColumns[index].width, container.height))

                        x += view.width
                        totalWidth += view.width
                    }

                    container.width = totalWidth + internalColumns[internalColumns.size - 1].width
                }
            }
        }

        override fun render(canvas: Canvas) {
            behavior?.renderHeader(this@Table, canvas)
        }

        public override fun doLayout() = super.doLayout()
    }

    private val header by lazy { Header() }

    private inner class PanelContainer: Box() {
        init {
            children += internalColumns.map { it.view }

            layout = object: Layout {
                override fun layout(container: PositionableContainer) {
                    var x          = 0.0
                    var height     = 0.0
                    var totalWidth = 0.0

                    container.children.forEachIndexed { index, view ->
                        view.bounds = Rectangle(Point(x, 0.0), Size(internalColumns[index].width, view.minimumSize.height))

                        x          += view.width
                        height      = max(height, view.height)
                        totalWidth += view.width
                    }

                    container.size = Size(max(container.parent!!.width, totalWidth), max(container.parent!!.height, height))

                    container.children.forEach {
                        it.height = container.height
                    }
                }
            }
        }

        override fun render(canvas: Canvas) {
            behavior?.renderBody(this@Table, canvas)
        }

        public override fun doLayout() = super.doLayout()
    }

    private val panel by lazy {
        ScrollPanel(PanelContainer().apply {
            // FIXME: Use two scroll-panels instead since async scrolling makes this look bad
            boundsChanged += { _, old, new ->
                if (old.x != new.x) {
                    header.x = new.x
                }
            }
        })
    }

    @Suppress("PrivatePropertyName")
    protected open val selectionChanged_: SetObserver<Int> = { set,removed,added ->
        (selectionChanged as SetPool).forEach {
            it(set, removed, added)
        }
    }

    init {
        selectionModel?.let { it.changed += selectionChanged_ }
    }

    private val bodyDirty  : (         ) -> Unit = { panel.content?.rerender() }
    private val headerDirty: (         ) -> Unit = { header.rerender        () }
    private val columnDirty: (Column<*>) -> Unit = { (it as? InternalColumn<*,*,*>)?.view?.rerender() }

    operator fun get(index: Int) = model[index]

    override fun removedFromDisplay() {
        selectionModel?.let { it.changed -= selectionChanged_ }

        super.removedFromDisplay()
    }

    public override var insets
        get(   ) = super.insets
        set(new) { super.insets = new }

    private var resizingCol: Int? = null

    override fun doLayout() {
        resizingCol = resizingCol ?: 0
        width       = columnSizePolicy.layout(width, internalColumns, resizingCol?.let { it + 1 } ?: 0)
        resizingCol = null

        super.doLayout()

        header.doLayout()
        (panel.content as? Table<*, *>.PanelContainer)?.doLayout()

        resizingCol = null
    }

    companion object {
        operator fun <T> invoke(
                       values        : List<T>,
                       selectionModel: SelectionModel<Int>? = null,
                       scrollCache   : Int                  = 10,
                       block         : ColumnFactory<T>.() -> Unit): Table<T, ListModel<T>> = Table(SimpleListModel(values), selectionModel, scrollCache, block)
    }
}
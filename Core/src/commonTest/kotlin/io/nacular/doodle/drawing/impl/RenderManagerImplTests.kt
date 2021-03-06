@file:Suppress("FunctionName")

package io.nacular.doodle.drawing.impl

import io.nacular.doodle.JsName
import io.nacular.doodle.accessibility.AccessibilityManager
import io.nacular.doodle.core.Box
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.InternalDisplay
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.AffineTransform.Companion.Identity
import io.nacular.doodle.drawing.Canvas
import io.nacular.doodle.drawing.GraphicsDevice
import io.nacular.doodle.drawing.GraphicsSurface
import io.nacular.doodle.drawing.RenderManager
import io.nacular.doodle.geometry.Rectangle
import io.nacular.doodle.geometry.Size
import io.nacular.doodle.geometry.times
import io.nacular.doodle.scheduler.AnimationScheduler
import io.nacular.doodle.scheduler.Task
import io.nacular.doodle.theme.InternalThemeManager
import io.nacular.doodle.time.Timer
import io.nacular.doodle.utils.ListObserver
import io.nacular.doodle.utils.ObservableList
import io.nacular.doodle.utils.Pool
import io.nacular.doodle.utils.PropertyObserver
import io.nacular.measured.units.Angle.Companion.degrees
import io.nacular.measured.units.Measure
import io.nacular.measured.units.Time
import io.nacular.measured.units.Time.Companion.milliseconds
import io.nacular.measured.units.times
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.Test

/**
 * Created by Nicholas Eddy on 11/6/17.
 */
class RenderManagerImplTests {
    // TODO: Add layout tests
    // TODO: Add tests to make sure things in cleanup list never get rendered
    // TODO: Add tests for display rect handling

    @Test @JsName("rendersAreBatched")
    fun `renders are batched`() {
        val view      = spyk<View>().apply { bounds = Rectangle(size = Size(100, 100)) }
        val scheduler = ManualAnimationScheduler()
        val display   = display(view)

        view.visible = false

        val renderManager = renderManager(display, scheduler = scheduler)

        view.visible  = true
        view.size    *= 2.0

        verify(exactly = 0) { view.render(any()) }

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, view)
    }

    @Test @JsName("laysOutDisplayOnSizeChange")
    fun `lays out display on size change`() {

        val display = display(view())

        val slot = slot<PropertyObserver<Display, Size>>()

        every { display.sizeChanged.plusAssign(capture(slot)) } just Runs

        renderManager(display)

        verify(exactly = 0) { display.relayout() }

        slot.captured(display, display.size, display.size * 2.0)

        verify(exactly = 1) { display.relayout() }
    }

    @Test @JsName("laysOutDisplayOnNewChild")
    fun `lays out display on new child`() {

        val display = display(view())

        val slot = slot<ListObserver<View>>()

        every { display.children.changed.plusAssign(capture(slot)) } just Runs

        renderManager(display)

        verify(exactly = 0) { display.relayout() }

        slot.captured(display.children, emptyMap(), mapOf(1 to view()), emptyMap())

        verify(exactly = 1) { display.relayout() }
    }

    @Test @JsName("laysOutDisplayOnChildBoundsChange")
    fun `lays out display on child bounds change`() {
        val child = spyk<View>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }

        val display = display(child)

        renderManager(display)

        verify(exactly = 0) { display.relayout() }

        child.width += 10

        verify(exactly = 1) { display.relayout() }
    }

    @Test @JsName("renderIgnoresUnknownViews")
    fun `render ignores unknown views`() {
        val view = spyk(view())

        renderManager().first.render(view)

        verify(exactly = 0) { view.render(any()) }
    }

    @Test @JsName("rendersDisplayedViews")
    fun `renders displayed views`() {
        val views         = (0 until 2).mapTo(mutableListOf()) { spyk(view()) }
        val display       = display(*views.toTypedArray())
        val renderManager = renderManager(display)

        views.forEach {
            verifyChildAddedProperly(renderManager, display, it)
        }
    }

    @Test @JsName("rendersNewViews")
    fun `renders new views`() {
        val child = spyk(view())

        val display        = display()
        val surface        = mockk<GraphicsSurface>()
        val graphicsDevice = graphicsDevice(mapOf(child to surface))

        val renderManager = renderManager(display, graphicsDevice = graphicsDevice)

        verify(exactly = 0) { child.render(any()) }

        display.children += child

        verifyChildAddedProperly(renderManager, display, child)

        val bounds    = child.bounds
        val transform = child.transform

        verify(exactly = 1) { surface setProperty "bounds"    value bounds    }
        verify(exactly = 1) { surface setProperty "transform" value transform }
    }

    @Test @JsName("removesTopLevelViews")
    fun `removes top-level views`() {
        val container = spyk<Box>().apply { bounds = Rectangle(size = Size(10.0, 10.0)); children += spyk(view()).apply { children += spyk(view()) } }

        val display   = display(container)
        val scheduler = ManualAnimationScheduler()

        val renderManager = renderManager(display, scheduler = scheduler)

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, container)

        display.children.remove(container)

        scheduler.runJobs()

        verifyChildRemovedProperly(container)
    }

    @Test @JsName("noopRemoveAddTopLevelViews")
    fun `no-op remove, add top-level views`() {
        val container1 = spyk<Box>().apply { bounds = Rectangle(size = Size(10.0, 10.0)); children += spyk(view()).apply { children += spyk(view()) } }
        val container2 = spyk<Box>().apply { bounds = Rectangle(size = Size(10.0, 10.0)); children += spyk(view()).apply { children += spyk(view()) } }
        val display    = display(container1, container2)
        val surface1   = mockk<GraphicsSurface>()
        val surface2   = mockk<GraphicsSurface>()

        val renderManager = renderManager(display = display, graphicsDevice = graphicsDevice(mapOf(container1 to surface1, container2 to surface2)))

        listOf(container1, container2).forEach {
            verifyChildAddedProperly(renderManager, display, it)
        }

        display.children.move(container2, 0)

        verify(exactly = 1) { surface2.index = 0 }

        listOf(container1, container2).forEach {
            verify(exactly = 0) { it.removedFromDisplay_(     ) }
            verify(exactly = 1) { it.render             (any()) }
            verify(exactly = 1) { it.doLayout_          (     ) }
        }
    }

    @Test @JsName("zOrderChangeTopLevelViews")
    fun `z-order change top-level views`() {
        val container1 = spyk<Box>().apply { bounds = Rectangle(size = Size(10.0, 10.0)); children += spyk(view()).apply { children += spyk(view()) } }
        val container2 = spyk<Box>().apply { bounds = Rectangle(size = Size(10.0, 10.0)); children += spyk(view()).apply { children += spyk(view()) } }
        val display    = display(container1, container2)
        val surface1   = mockk<GraphicsSurface>()
        val surface2   = mockk<GraphicsSurface>()

        val renderManager = renderManager(display = display, graphicsDevice = graphicsDevice(mapOf(container1 to surface1, container2 to surface2)))

        listOf(container1, container2).forEach {
            verifyChildAddedProperly(renderManager, display, it)
        }

        container2.zOrder = 3

        verify(exactly = 1) { surface2.zOrder = 3 }

        listOf(container1, container2).forEach {
            verify(exactly = 0) { it.removedFromDisplay_(     ) }
            verify(exactly = 1) { it.render             (any()) }
            verify(exactly = 1) { it.doLayout_          (     ) }
        }
    }

    @Test @JsName("removesNestedViews")
    fun `removes nested views`() {
        val container = spyk<Box>().apply { bounds = Rectangle(size = Size(10.0, 10.0)); children += spyk(view()).apply { children += spyk(view()) } }

        val display   = display(container)
        val scheduler = ManualAnimationScheduler()

        val renderManager = renderManager(display, scheduler = scheduler)

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, container)

        val firstChild = container.children.first()
        container.children -= firstChild

        scheduler.runJobs()

        verifyChildRemovedProperly(firstChild)
    }

    @Test @JsName("rerendersOnBoundsChanged")
    fun `rerenders on bounds changed`() {
        val view          = spyk<View>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val display       = display(view)
        val renderManager = renderManager(display)

        verifyChildAddedProperly(renderManager, display, view)

        view.size *= 2.0

        verify(exactly = 2) { view.render(any()) }
    }

    @Test @JsName("rerendersOnBecomingVisible")
    fun `rerenders on becoming visible`() {
        val view    = spyk<View>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val display = display(view)

        view.visible = false

        val renderManager = renderManager(display)

        verifyChildAddedProperly(renderManager, display, view, 0)

        view.visible = true

        verifyChildAddedProperly(renderManager, display, view)
    }

    @Test @JsName("rerendersOnAddedBecomingVisible")
    fun `rerenders on added becoming visible`() {
        val parent  = container()
        val view    = spyk<View>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val display = display(parent)

        view.visible = false

        val renderManager = renderManager(display)

        verify(exactly = 0) { view.addedToDisplay(display, renderManager.first, any()) }
        verify(exactly = 0) { view.render        (any()                              ) }

        parent.children_ += view

        view.visible = true

        verifyChildAddedProperly(renderManager, display, view)
    }

    @Test @JsName("doesNotRerenderOnBoundsZeroed")
    fun `does not rerender on bounds zeroed`() {
        val view          = spyk<View>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val display       = display(view)
        val renderManager = renderManager(display)

        verifyChildAddedProperly(renderManager, display, view)

        view.size *= 0.0

        verify(exactly = 1) { view.render(any()) }
    }

    @Test @JsName("doesNotRerenderOnPositionChanged")
    fun `does not rerender on position changed`() {
        val view          = spyk<View>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val display       = display(view)
        val renderManager = renderManager(display)

        verifyChildAddedProperly(renderManager, display, view)

        view.x *= 2.0

        verify(exactly = 1) { view.render(any()) }
    }

    @Test @JsName("doesNotRerenderOnSizeZeroed")
    fun `does not re-render on size zeroed`() {
        val view          = spyk<View>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val display       = display(view)
        val renderManager = renderManager(display)

        verifyChildAddedProperly(renderManager, display, view)

        view.size = Size.Empty

        verify(exactly = 1) { view.render(any()) }
    }

    @Test @JsName("rendersPreExistingNestedViews")
    fun `renders pre-existing nested views`() {
        val container = container()
        val child     = spyk(view())

        container.children += child

        val display = display(container)

        val renderManager = renderManager(display)

        verifyChildAddedProperly(renderManager, display, child)
    }

    @Test @JsName("rendersReAddedNestedViews")
    fun `renders re-added nested views`() {
        val container = container()
        val child     = spyk(view())

        container.children += child

        val display = display(container)

        val renderManager = renderManager(display)

        container.children -= child

        container.children += child

        verifyChildAddedProperly(renderManager, display, child, 2)
    }

    @Test @JsName("rendersNewNestedViews")
    fun `renders new nested views`() {
        val container = container()
        val child     = spyk(view())

        val display = display(container)

        val renderManager = renderManager(display)

        container.children += view()
        container.children += view()

        verify(exactly = 0) { child.render(any()) }

        container.children += child

        verifyChildAddedProperly(renderManager, display, child)
    }

    @Test @JsName("rendersNewNestedViewInserted")
    fun `renders new nested view inserted`() {
        val container = container()
        val child     = spyk(view())
        val display   = display(container)
        val surface   = mockk<GraphicsSurface>()
        val device    = graphicsDevice(mapOf(child to surface))

        val renderManager = renderManager(display, graphicsDevice = device)

        container.children += view()
        container.children += view()

        verify(exactly = 0) { child.render(any()) }

        container.children.add(1, child)

        verifyChildAddedProperly(renderManager, display, child)
    }

    @Test @JsName("doesNotRenderInvisibleViews")
    fun `does not render invisible views`() = doesNotRender(spyk(view()).apply { visible = false })

    @Test @JsName("doesNotRenderInvisibleChildren")
    fun `does not render invisible children`() = doesNotRenderChild(spyk(view()).apply { visible = false })

    @Test @JsName("doesNotRenderZeroBoundsViews")
    fun `does not render zero bounds views`() = doesNotRender(spyk(view()).apply { bounds = Rectangle.Empty })

    @Test @JsName("renderNowIgnoresUnknownViews")
    fun `renderNow ignores unknown views`() {
        val view = spyk(view())

        renderManager().first.renderNow(view)

        verify(exactly = 0) { view.render(any()) }
    }

    @Test @JsName("renderNowWorks")
    fun `renderNow works`() {
        val view = spyk(view())

        renderManager(display(view)).also { (renderManager, _) ->
            verify(exactly = 1) { view.render(any()) }

            renderManager.renderNow(view)

            verify(exactly = 2) { view.render(any()) }
        }
    }

    @Test @JsName("revalidatesParentWhenNewViews")
    fun `revalidates parent out when new views`() {
        val container = spyk<Box>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val child     = view()

        val display = display(container)

        renderManager(display)

        verify(exactly = 0) { container.revalidate_() }

        container.children += child

        verify(exactly = 1) { container.revalidate_() }
    }

    @Test @JsName("laysOutParentOnSizeChanged")
    fun `lays out parent on size changed`() = verifyLayout { it.size *= 2.0 }

    @Test @JsName("laysOutParentOnPositionChanged")
    fun `lays out parent on position changed`() = verifyLayout { it.x += 2.0 }

    @Test @JsName("laysOutParentOnVisibilityChanged")
    fun `lays out parent on visibility changed`() = verifyLayout { it.visible = false }

    @Test @JsName("reflectsVisibilityChange")
    fun `reflects visibility change`() {
        val container = spyk<Box>("xyz").apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val child     = spyk<View>().apply { bounds = Rectangle(size = Size(10.0, 10.0)) }

        container.children += child

        val parentSurface  = mockk<GraphicsSurface>  ()
        val childSurface   = mockk<GraphicsSurface>  ()
        val graphicsDevice = mockk<GraphicsDevice<*>>().apply {
            every { get(container) } returns parentSurface
            every { get(child    ) } returns childSurface
        }

        renderManager(display(container), graphicsDevice = graphicsDevice)

        child.visible = false

        verify(exactly = 1) { childSurface.visible = false }
        verify(exactly = 0) { childSurface.visible = true  }

        child.visible = true

        verify(exactly = 1) { childSurface.visible = true }
    }

    @Test @JsName("reflectsTransformChange")
    fun `reflects transform change`() {
        val child = spyk<View>().apply { bounds = Rectangle(size = Size(10.0, 10.0)) }

        val childSurface   = mockk<GraphicsSurface>  ()
        val graphicsDevice = graphicsDevice(mapOf(child to childSurface))


        renderManager(display(child), graphicsDevice = graphicsDevice)

        child.transform = Identity.rotate(45 * degrees)

        verify(exactly = 1) { childSurface.transform = Identity.rotate(45 * degrees) }

        child.transform = Identity.rotate(57 * degrees)

        verify(exactly = 1) { childSurface.transform = Identity.rotate(57 * degrees) }
    }


    @Test @JsName("installsThemeForDisplayedViews")
    fun `installs theme for displayed views`() {
        val container = spyk<Box>().apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val child     = view()

        container.children += child

        val display      = display(container)
        val themeManager = mockk<InternalThemeManager>()

        renderManager(display, themeManager = themeManager)

        verify(exactly = 1) { themeManager.update(container) }
        verify(exactly = 1) { themeManager.update(child    ) }
    }

    @Test @JsName("removesWhenReAddedTopLevel")
    fun `renders removed when re-added (top-level)`() {
        val child     = spyk(view())
        val display   = display(child)
        val scheduler = ManualAnimationScheduler()

        val renderManager = renderManager(display, scheduler = scheduler)

        scheduler.runJobs()

        display.children -= child

        // Add happens before next render

        display.children += child

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, child, 2)
    }

    @Test @JsName("noOpRemovedReAddedSameParent")
    fun `no-op removed re-added same parent`() {
        val child     = spyk(view())
        val container = container()

        container.children += child

        val display   = display(container)
        val scheduler = ManualAnimationScheduler()

        val renderManager = renderManager(display, scheduler = scheduler)

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, child)

        container.children -= child

        // Add happens before next render

        container.children += child

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, child)
    }

    @Test @JsName("rendersRemovedReAddedDifferentParent")
    fun `renders removed re-added different parent`() {
        val child      = spyk(view())
        val container1 = container()
        val container2 = container()

        container1.children += child

        val display   = display()
        val scheduler = ManualAnimationScheduler()

        display.children += listOf<View>(container1, container2)

        val renderManager = renderManager(display, scheduler = scheduler)

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, child)

        // Add happens before next render

        container1.children -= child
        container2.children += child

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, child, 2)
    }

    @Test @JsName("rendersRemovedReAddedNestedDifferentParent")
    fun `renders removed re-added nested different parent`() {
        val child      = spyk(view())
        val container1 = container()
        val container2 = container()

        container1.children += child

        val display   = display(container1)
        val scheduler = ManualAnimationScheduler()

        val renderManager = renderManager(display, scheduler = scheduler)

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, child)

        container1.children -= child
        container2.children += child

        // Add happens before next render

        display.children -= container1 as View
        display.children += container2 as View

        scheduler.runJobs()

        verifyChildAddedProperly(renderManager, display, child, 2)
    }

    private fun verifyLayout(block: (View) -> Unit) {
        val container = spyk<Box>("xyz").apply { bounds = Rectangle(size = Size(100.0, 100.0)) }
        val child     = view()

        container.layout    = mockk()
        container.children += child

        renderManager(display(container))

        verify(exactly = 1) { container.doLayout_() }

        block(child)

        verify(exactly = 2) { container.doLayout_() }
    }

    private fun verifyChildAddedProperly(renderManager: Pair<RenderManager, AccessibilityManager>, display: Display, view: View, times: Int = 1) {
        verify(exactly = times) { view.addedToDisplay(display, renderManager.first, renderManager.second) }
        verify(exactly = times) { view.render        (any()                                             ) }
    }

    private fun verifyChildRemovedProperly(view: View) {
        verify(exactly = 1) { view.removedFromDisplay_() }

        view.children_.forEach { verifyChildRemovedProperly(it) }
    }

    private fun view(): View = object: View() {}.apply { bounds = Rectangle(size = Size(10.0, 10.0)) }
    private fun container(): Box = Box().apply { bounds = Rectangle(size = Size(10.0, 10.0)) }

    private fun doesNotRender(view: View) {
        renderManager(display(view))

        verify(exactly = 0) { view.render(any()) }
    }

    private fun doesNotRenderChild(view: View) {
        val container = spyk<Box>("xyz").apply { bounds = Rectangle(size = Size(100.0, 100.0)) }

        container.children += view

        renderManager(display(container))

        verify(exactly = 0) { view.render(any()) }
    }

    private fun renderManager(
            display             : InternalDisplay      = mockk(),
//            timer               : Timer                = timer(),
            themeManager        : InternalThemeManager = mockk(),
            scheduler           : AnimationScheduler   = instantScheduler,
            accessibilityManager: AccessibilityManager = mockk(),
            graphicsDevice      : GraphicsDevice<*>    = defaultGraphicsDevice): Pair<RenderManagerImpl, AccessibilityManager> = RenderManagerImpl(display, scheduler, themeManager, accessibilityManager, graphicsDevice) to accessibilityManager

    private val defaultGraphicsDevice by lazy {
        graphicsDevice()
    }

    private fun timer(): Timer = mockk<Timer>().apply {
        every { now } returns 0 * milliseconds
    }

    private fun graphicsDevice(mapping: Map<View, GraphicsSurface> = mapOf()): GraphicsDevice<*> {
        val result         = mockk<GraphicsDevice<*>>()
        val defaultSurface = mockk<GraphicsSurface>()
        val canvas         = mockk<Canvas>()

        every { defaultSurface.render(captureLambda()) } answers {
            lambda<(Canvas) -> Unit>().captured(canvas)
        }

        mapping.forEach {
            every { it.value.render(captureLambda()) } answers {
                lambda<(Canvas) -> Unit>().captured(canvas)
            }
        }

        every { result[any()] } answers { mapping[firstArg()] ?: defaultSurface }

        return result
    }

    private fun display(vararg children: View): InternalDisplay = mockk<InternalDisplay>().apply {
        val displayChildren = ObservableList<View>()

        displayChildren.addAll(children)

        val view = slot<View>()

        every { this@apply.children   } returns displayChildren
        every { this@apply.iterator() } answers { displayChildren.iterator() }

        // FIXME: compiler fails to build w/o hint
        every { sizeChanged as Pool<PropertyObserver<Display, Size>> } returns mockk()
        every { this@apply.ancestorOf(capture(view)) } answers {
            var result = false

            if (this@apply.children.isNotEmpty()) {
                var parent: View? = view.captured

                do {
                    if (parent in this@apply.children) {
                        result = true
                        break
                    }

                    parent = parent?.parent
                } while (parent != null)
            }

            result
        }
    }

    private val instantScheduler by lazy { mockk<AnimationScheduler>().apply {
        every { this@apply.onNextFrame(captureLambda()) } answers {
            lambda<(Measure<Time>) -> Unit>().captured(0 * milliseconds)

            val task = mockk<Task>()

            every { task.completed } returns true

            task
        }
    }}

//    private val instantStrand by lazy { mockk<Strand>().apply {
//        val iterable = slot<Iterable<() -> Unit>>()
//
//        every { this@apply.invoke(capture(iterable)) } answers {
//            iterable.captured.forEach {
//                it()
//            }
//
//            val task = mockk<Task>()
//
//            every { task.completed } returns true
//
//            task
//        }
//
//        val sequence = slot<Sequence<() -> Unit>>()
//
//        every { this@apply.invoke(capture(sequence)) } answers { this@apply(sequence.captured.asIterable()) }
//    } }

    private class SimpleTask(override var completed: Boolean = false) : Task {
        override fun cancel() {
            completed = true
        }
    }

    private class ManualAnimationScheduler: AnimationScheduler {
        val tasks = mutableListOf<Pair<SimpleTask, (Measure<Time>) -> Unit>>()

        fun runJobs() {
            tasks.forEach {
                it.first.completed = true
                it.second(0 * milliseconds)
            }
        }

        override fun onNextFrame(job: (Measure<Time>) -> Unit): Task {
            val task = SimpleTask()

            tasks += task to job

            return task
        }
    }

//    private class ManualStrand: Strand {
//        val jobs = mutableListOf<Pair<SimpleTask, Iterable<() -> Unit>>>()
//
//        fun runJobs() {
//            jobs.forEach {
//                it.first.completed = true
//                it.second.forEach { it() }
//            }
//        }
//
//        override operator fun invoke(jobs: Sequence<() -> Unit>): Task = invoke(jobs.asIterable())
//
//        override operator fun invoke(jobs: Iterable<() -> Unit>): Task {
//            val task = SimpleTask()
//
//            this.jobs += task to jobs
//
//            return task
//        }
//    }
}
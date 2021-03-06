package io.nacular.doodle.drawing.impl

import io.nacular.doodle.Node
import io.nacular.doodle.NodeList
import io.nacular.doodle.SVGCircleElement
import io.nacular.doodle.SVGElement
import io.nacular.doodle.SVGEllipseElement
import io.nacular.doodle.SVGPolygonElement
import io.nacular.doodle.SVGRectElement
import io.nacular.doodle.dom.HtmlFactory
import io.nacular.doodle.dom.SvgFactory
import io.nacular.doodle.dom.setBounds
import io.nacular.doodle.dom.setCircle
import io.nacular.doodle.dom.setEllipse
import io.nacular.doodle.dom.setFill
import io.nacular.doodle.dom.setPoints
import io.nacular.doodle.dom.setStroke
import io.nacular.doodle.dom.setStrokeWidth
import io.nacular.doodle.drawing.Brush
import io.nacular.doodle.drawing.Color.Companion.Black
import io.nacular.doodle.drawing.Color.Companion.Green
import io.nacular.doodle.drawing.Color.Companion.Pink
import io.nacular.doodle.drawing.Color.Companion.Red
import io.nacular.doodle.drawing.ColorBrush
import io.nacular.doodle.drawing.Pen
import io.nacular.doodle.drawing.TextMetrics
import io.nacular.doodle.geometry.Circle
import io.nacular.doodle.geometry.ConvexPolygon
import io.nacular.doodle.geometry.Ellipse
import io.nacular.doodle.geometry.Point
import io.nacular.doodle.geometry.Point.Companion.Origin
import io.nacular.doodle.geometry.Rectangle
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.expect

/**
 * Created by Nicholas Eddy on 8/11/19.
 */

// FIXME: Remove once Mockk updates js library to support this function
expect inline fun mockkStatic(vararg classes: String)

@Suppress("FunctionName")
class VectorRendererSvglTests {
    init {
        mockkStatic("io.nacular.doodle.dom.SvgElementKt")
    }

    @Test @JsName("invisibleToolsNoOp") fun `invisible tools no-op`() {
        val rect   = Rectangle(100, 100)
        val poly   = ConvexPolygon(Origin, Origin + Point(100, 100), Origin + Point(-100, 100))
        val circle = Circle(100.0)

        listOf<VectorRendererSvg.(Pen, Brush) -> Unit>(
            { pen, _     -> line(Origin, Origin + Point(100, 0), pen) },

            { _,   brush -> rect(rect,            brush) },
            { _,   brush -> rect(rect, 10.0,      brush) },
            { pen, brush -> rect(rect, pen,       brush) },
            { pen, brush -> rect(rect, 10.0, pen, brush) },

            { _,   brush -> circle(circle,      brush) },
            { pen, brush -> circle(circle, pen, brush) },

            { _,   brush -> poly(poly,      brush) },
            { pen, brush -> poly(poly, pen, brush) },

            { _,   brush -> ellipse(circle,      brush) },
            { pen, brush -> ellipse(circle, pen, brush) }
        ).forEach {
            nothingRendered(it)
        }
    }

    @Test @JsName("emptyShapesNoOp") fun `empty shapes no-op`() {
        val pen    = Pen()
        val brush  = ColorBrush(Red)
        val rect   = Rectangle.Empty
        val poly   = ConvexPolygon(Origin, Origin, Origin)
        val circle = Circle.Empty

        listOf<VectorRendererSvg.() -> Unit>(
            { rect(rect,            brush) },
            { rect(rect, 10.0,      brush) },
            { rect(rect,       pen, brush) },
            { rect(rect, 10.0, pen, brush) },

            { circle(circle,      brush) },
            { circle(circle, pen, brush) },

            { poly(poly,      brush) },
            { poly(poly, pen, brush) },

            { ellipse(circle,      brush) },
            { ellipse(circle, pen, brush) }//,

//            { text   ("text", null, Point.Origin,             brush) },
//            { wrapped("text", null, Point.Origin, 0.0, 100.0, brush) }
        ).forEach {
            nothingRendered(it)
        }
    }

    @Test @JsName("rendersSimpleRect") fun `renders simple rect`() {
        validateRender { context, svgFactory ->
            val brush = ColorBrush(Red)
            val rect  = Rectangle(100, 100)

            val region = context.renderRegion
            val svg    = mockk<SVGElement>       ().apply { every { parentNode } returns null; every { firstChild } returns null }
            val poly   = mockk<SVGPolygonElement>().apply { every { parentNode } returns null }

            every { context.renderPosition                   } returns null
            every { svgFactory<SVGElement>       ("svg"    ) } returns svg
            every { svgFactory<SVGPolygonElement>("polygon") } returns poly

            rect(rect, brush)

            verifyRect(poly, rect, brush = brush)

            verify (exactly = 1) { svg.appendChild   (poly) }
            verify (exactly = 1) { region.appendChild(svg ) }
        }
    }

    @Test @JsName("rendersSimpleRoundedRect") fun `renders simple rounded-rect`() {
        val brush  = ColorBrush(Red)
        val rect   = Rectangle(100, 100)
        val radius = 12.0

        validateRender { context, svgFactory ->
            val region = context.renderRegion
            val svg    = mockk<SVGElement>    ().apply { every { parentNode } returns null; every { firstChild } returns null }
            val r      = mockk<SVGRectElement>().apply { every { parentNode } returns null }

            every { context.renderPosition             } returns null
            every { svgFactory<SVGElement>    ("svg" ) } returns svg
            every { svgFactory<SVGRectElement>("rect") } returns r

            rect(rect, radius, brush)

            verify (exactly = 1) { r.setFill  (brush.color           ) }
            verify (exactly = 0) { r.setStroke(isNull(inverse = true)) }
            verify (exactly = 1) { r.setBounds(rect                  ) }

            verify (exactly = 1) { svg.appendChild   (r  ) }
            verify (exactly = 1) { region.appendChild(svg) }
        }
    }

    @Test @JsName("rendersSimpleCircle") fun `renders simple circle`() {
        val brush  = ColorBrush(Red)
        val circle = Circle(center = Point(10, 10), radius = 100.0)

        validateRender { context, svgFactory ->
            val region = context.renderRegion
            val svg    = mockk<SVGElement>      ().apply { every { parentNode } returns null; every { firstChild } returns null }
            val c      = mockk<SVGCircleElement>().apply { every { parentNode } returns null }

            every { context.renderPosition                 } returns null
            every { svgFactory<SVGElement>      ("svg"   ) } returns svg
            every { svgFactory<SVGCircleElement>("circle") } returns c

            circle(circle, brush)

            verify (exactly = 1) { c.setFill  (brush.color           ) }
            verify (exactly = 1) { c.setCircle(circle                ) }
            verify (exactly = 0) { c.setStroke(isNull(inverse = true)) }

            verify (exactly = 1) { svg.appendChild   (c  ) }
            verify (exactly = 1) { region.appendChild(svg) }
        }
    }

    @Test @JsName("rendersSimpleEllipse") fun `renders simple ellipse`() {
        val brush   = ColorBrush(Red)
        val ellipse = Ellipse(center = Point(10, 10), xRadius = 100.0, yRadius = 45.0)

        validateRender { context, svgFactory ->
            val region = context.renderRegion
            val svg    = mockk<SVGElement>       ().apply { every { parentNode } returns null; every { firstChild } returns null }
            val e      = mockk<SVGEllipseElement>().apply { every { parentNode } returns null }

            every { context.renderPosition                   } returns null
            every { svgFactory<SVGElement>       ("svg"    ) } returns svg
            every { svgFactory<SVGEllipseElement>("ellipse") } returns e

            ellipse(ellipse, brush)

            verify (exactly = 1) { e.setFill   (brush.color           ) }
            verify (exactly = 0) { e.setStroke (isNull(inverse = true)) }
            verify (exactly = 1) { e.setEllipse(ellipse               ) }

            verify (exactly = 1) { svg.appendChild   (e  ) }
            verify (exactly = 1) { region.appendChild(svg) }
        }
    }

    @Test @JsName("reusesSvgElement") fun `reuses svg element`() {
        val svg        = slot<SVGElement>()
        val context    = mockk<CanvasContext>()
        val element    = slot<Node>()
        val svgFactory = svgFactory()
        val renderer   = renderer(context, svgFactory)

        val region = context.renderRegion

        every { context.renderPosition } returns null

        every { context.renderPosition = capture(element) } answers { every { context.renderPosition } returns element.captured }

        every { region.appendChild(capture(svg)) } answers { every { svg.captured.parentNode } returns region; svg.captured }

        renderer.rect(Rectangle(200, 100), ColorBrush(Red))
        renderer.rect(Rectangle(100, 200), ColorBrush(Red))

        verify (exactly = 2) { svg.captured.appendChild(any()        ) }
        verify (exactly = 1) { svgFactory<SVGElement>  ("svg"        ) }
        verify (exactly = 2) { svgFactory<SVGElement>  ("polygon"    ) }
        verify (exactly = 1) { region.appendChild      (svg.captured ) }
    }

    @Test @JsName("reusesAllElments") fun `reuses all elements`() {
        val svgElements = mutableListOf<SVGElement>()
        val context     = mockk<CanvasContext>()
        val element     = slot<Node>()
        val svgFactory  = svgFactory(svgElements)
        val renderer    = renderer(context, svgFactory)
        val region      = context.renderRegion

        setupNode(region, "")

        every { context.renderPosition } returns null

        every { context.renderPosition = capture(element) } answers { every { context.renderPosition } returns element.captured }

        renderer.rect(Rectangle(100, 200), ColorBrush(Red))
        drawNonSvg(context)

        context.renderPosition = context.renderRegion.firstChild
        renderer.clear()

        renderer.rect(Rectangle(200, 100), ColorBrush(Green))

        expect(2) { svgElements.size }

        val svg = svgElements.first { it.nodeName == "svg" }

        verifyRect(svg.firstChild as SVGPolygonElement, Rectangle(100, 200), brush = ColorBrush(Red  ))
        verifyRect(svg.firstChild as SVGPolygonElement, Rectangle(200, 100), brush = ColorBrush(Green))

        verify (exactly = 1) { svg.appendChild(any()           ) }
        verify (exactly = 1) { svgFactory<SVGElement>("svg"    ) }
        verify (exactly = 1) { svgFactory<SVGElement>("polygon") }
        verify (exactly = 1) { region.appendChild    (svg      ) }
    }

    @Test @JsName("picksCorrectSvg") fun `picks correct svg`() {
        val svgElements = mutableListOf<SVGElement>()
        val context     = mockk<CanvasContext>()
        val element     = slot<Node>()
        val svgFactory  = svgFactory(svgElements)
        val renderer    = renderer(context, svgFactory)
        val region      = context.renderRegion

        setupNode(region, "")

        every { context.renderPosition } returns null

        every { context.renderPosition = capture(element) } answers { every { context.renderPosition } returns element.captured }

        renderer.rect(Rectangle(100, 200), ColorBrush(Red))

        drawNonSvg(context)

        renderer.rect(Rectangle(200, 100), ColorBrush(Green))

        val svgs = svgElements.filter { it.nodeName == "svg" }

        expect(2) { svgs.size }

        verifyRect(svgs[0].firstChild as SVGPolygonElement, Rectangle(100, 200), brush = ColorBrush(Red  ))
        verifyRect(svgs[1].firstChild as SVGPolygonElement, Rectangle(200, 100), brush = ColorBrush(Green))

        verify (exactly = 1) { svgs[0].appendChild   (any()    ) }
        verify (exactly = 1) { svgs[1].appendChild   (any()    ) }
        verify (exactly = 2) { svgFactory<SVGElement>("svg"    ) }
        verify (exactly = 2) { svgFactory<SVGElement>("polygon") }
        verify (exactly = 1) { region.appendChild    (svgs[0]  ) }
        verify (exactly = 1) { region.appendChild    (svgs[1]  ) }
    }

    @Test @JsName("picksCorrectSvgRepeated") fun `picks correct svg repeated`() {
        val svgElements = mutableListOf<SVGElement>()
        val context     = mockk<CanvasContext>()
        val element     = slot<Node>()
        val svgFactory  = svgFactory(svgElements)
        val renderer    = renderer(context, svgFactory)
        val region      = context.renderRegion

        setupNode(region, "")

        every { context.renderPosition } returns null

        every { context.renderPosition = capture(element) } answers { every { context.renderPosition } returns element.captured }

        renderer.rect(Rectangle(100, 100), ColorBrush(Red))
        drawNonSvg(context)
        renderer.rect(Rectangle(200, 200), ColorBrush(Green))
        context.renderPosition = context.renderRegion.firstChild
        renderer.clear()
        renderer.rect(Rectangle(300, 300), ColorBrush(Pink))
        context.renderPosition = context.renderRegion.childNodes.item(2)
        renderer.rect(Rectangle(400, 400), ColorBrush(Black))

        val svgs = svgElements.filter { it.nodeName == "svg" }

        expect(2) { svgs.size }

        verifyRect(svgs[0].firstChild as SVGPolygonElement, Rectangle(100, 100), brush = ColorBrush(Red  ))
        verifyRect(svgs[1].firstChild as SVGPolygonElement, Rectangle(200, 200), brush = ColorBrush(Green))
        verifyRect(svgs[0].firstChild as SVGPolygonElement, Rectangle(300, 300), brush = ColorBrush(Pink ))
        verifyRect(svgs[1].firstChild as SVGPolygonElement, Rectangle(400, 400), brush = ColorBrush(Black))

        verify (exactly = 1) { svgs[0].appendChild   (any()    ) }
        verify (exactly = 1) { svgs[1].appendChild   (any()    ) }
        verify (exactly = 2) { svgFactory<SVGElement>("svg"    ) }
        verify (exactly = 2) { svgFactory<SVGElement>("polygon") }
        verify (exactly = 1) { region.appendChild    (svgs[0]  ) }
        verify (exactly = 1) { region.appendChild    (svgs[1]  ) }
    }

    private fun drawNonSvg(context: CanvasContext) {
        context.renderRegion.appendChild(mockk())

        every { context.renderPosition } returns null
    }

    private fun verifyRect(poly: SVGPolygonElement, rect: Rectangle, pen: Pen? = null, brush: ColorBrush) {
        verify (exactly = 1) { poly.setFill  (brush.color                ) }

        when (pen) {
            null -> verify (exactly = 0) { poly.setStroke(isNull(inverse = true)     ) }
            else -> verify (exactly = 1) { poly.setStroke(pen.color); poly.setStrokeWidth(pen.thickness) }
        }

        verify (exactly = 1) { poly.setPoints(*rect.points.toTypedArray()) }
    }

    private fun setupNode(node: Node, name: String) {
        node.apply {
            val child    = slot<Node>()
            val children = mutableListOf<Node>()

            every { parentNode } returns null
            every { nodeName   } returns name

            every { appendChild(capture(child)) } answers {
                children.lastOrNull()?.apply {
                    every { nextSibling } returns child.captured
                }

                children += child.captured

                child.captured
            }

            every { firstChild } answers {
                childNodes.item(0)
            }

            every { childNodes } returns object: NodeList() {
                override val length get() = children.size

                override fun item(index: Int) = children.getOrNull(index)
            }
        }
    }

    private inline fun <reified T: SVGElement> svg(tag: String): T = mockk<T>().apply {
        setupNode(this, tag)
    }

    private fun svgFactory(products: MutableList<SVGElement> = mutableListOf()) = mockk<SvgFactory>().apply {
        val slot = slot<String>()

        every { invoke<SVGElement>(capture(slot)) } answers {
            when (val tag = slot.captured) {
                "polygon" -> svg<SVGPolygonElement>(tag)
                else      -> svg<SVGElement>       (tag)
            }.also {
                products += it
            }
        }
    }

//    @Test fun `renders simple text`() {
//        val brush = ColorBrush(red)
//        val text  = "some text"
//        val font  = mockk<Font>()
//        val at    = Point(34, 89)
//
//        validateRender { renderParent, _, textFactory, _ ->
//            val t = mockk<HTMLElement>()
//            every { textFactory.create(text, font, null) } returns t
//
//            text(text, font, at, brush)
//
//            val style = t.style
//
//            verify (exactly = 1) { style.setOpacity(brush.color.opacity) }
//            verify (exactly = 1) { style.setColor  (brush.color        ) }
//            verify (exactly = 1) { style.translate (at                 ) }
//
//            verify (exactly = 1) { renderParent.appendChild(t) }
//        }
//    }
//
//    @Test fun `renders simple styled text`() {
//        val text = StyledText("some text")
//        val at   = Point(34, 89)
//
//        validateRender { renderParent, _, textFactory, _ ->
//            val t = mockk<HTMLElement>()
//            every { textFactory.create(text, null) } returns t
//
//            text(text, at)
//
//            val style = t.style
//
//            verify (exactly = 1) { style.translate(at) }
//
//            verify (exactly = 1) { renderParent.appendChild(t) }
//        }
//    }
//
//    @Test fun `renders simple wrapped text`() {
//        val brush = ColorBrush(red)
//        val text  = "some text"
//        val font  = mockk<Font>()
//        val at    = Point(150, 89)
//
//        validateRender { renderParent, _, textFactory, _ ->
//            val t = mockk<HTMLElement>()
//            every { textFactory.wrapped(text, font, 100.0, 50.0, null) } returns t
//
//            wrapped(text, font, at, 100.0, 200.0, brush)
//
//            val style = t.style
//
//            verify (exactly = 1) { style.setOpacity(brush.color.opacity) }
//            verify (exactly = 1) { style.setColor  (brush.color        ) }
//            verify (exactly = 1) { style.translate (at                 ) }
//
//            verify (exactly = 1) { renderParent.appendChild(t) }
//        }
//    }
//
//    @Test fun `renders simple wrapped styled text`() {
//        val text  = StyledText("some text")
//        val at    = Point(150, 89)
//
//        validateRender { renderParent, _, textFactory, _ ->
//            val t = mockk<HTMLElement>()
//            every { textFactory.wrapped(text, 100.0, 50.0, null) } returns t
//
//            wrapped(text, at, 100.0, 200.0)
//
//            val style = t.style
//
//            verify (exactly = 1) { style.translate(at) }
//
//            verify (exactly = 1) { renderParent.appendChild(t) }
//        }
//    }

//    @Test fun `renders image`() {
//        val image = mockk<ImageImpl>().apply {
//            every { size } returns Size(130.0, 46.0)
//        }
//        val at    = Point(150, 89)
//
//        validateRender { renderParent, _, _, _ ->
//            val clone = mockk<HTMLImageElement>()
//            val img   = mockk<HTMLImageElement>().apply {
//                every { cloneNode(false) } returns clone
//            }
//
//            every { image.image } returns img
//
//            image(image, Rectangle(position = at, size = image.size))
//
//            val style = clone.style
//            val size  = image.size
//
//            verify (exactly = 1) { style.setOpacity(1f  ) }
//            verify (exactly = 1) { style.setSize   (size) }
//            verify (exactly = 1) { style.translate (at  ) }
//
//            verify (exactly = 1) { renderParent.appendChild(clone) }
//        }
//    }

//    @Test fun `transforms work`() {
//        listOf<CanvasImpl.() -> AffineTransform>(
//            // TODO: Verify other forms for these APIs
//            { scale           (      10.0, 4.5 )         {}; Identity.scale    (10.0, 4.5   ) },
//            { rotate          (23 * degrees    )         {}; Identity.rotate   (23 * degrees) },
//            { translate       (Point(10.0, 4.5))         {}; Identity.translate(10.0,  4.5  ) },
//            { transform       (Identity.skew(46.0, 0.0)) {}; Identity.skew     (46.0,  0.0  ) },
//            { flipVertically                             {}; Identity.scale    ( 1.0, -1.0  ) },
//            { flipHorizontally                           {}; Identity.scale    (-1.0,  1.0  ) }
//        ).forEach {
//            validateTransform(it)
//        }
//    }

//    private fun validateTransform(block: CanvasImpl.() -> AffineTransform) {
//        val htmlFactory  = mockk<HtmlFactory>()
//        val renderParent = spyk<HTMLElement> (              )
//
//        renderer(renderParent, htmlFactory).apply {
//            val frame = mockk<HTMLElement>().apply { every { parentNode } returns null andThen renderParent }
//
//            every { htmlFactory.createOrUse("B", any()) } returns frame
//
//            val transform = block(this)
//            val style     = frame.style
//
//            verify { style.setTransform(transform) }
//            verify (exactly = 1) { renderParent.appendChild(frame) }
//        }
//    }

    private fun validateRender(block: VectorRendererSvg.(context: CanvasContext, svgFactory: SvgFactory) -> Unit) {
        val context = mockk<CanvasContext>()
        val factory = mockk<SvgFactory>   ()

        renderer(context, factory).apply {
            block(this, context, factory)
        }
    }

    private fun nothingRendered(block: VectorRendererSvg.(Pen, Brush) -> Unit) {
        val context = mockk<CanvasContext>()
        val factory = mockk<SvgFactory>   ()

        renderer(context, factory).apply {
            block(this, mockk<Pen>().apply { every { visible } returns false }, mockk<Brush>().apply { every { visible } returns false })
        }

        val region = context.renderRegion

        verify(exactly = 0) { region.appendChild(any()) }
    }

    private fun nothingRendered(block: VectorRendererSvg.() -> Unit) {
        val context = mockk<CanvasContext>()
        val factory = mockk<SvgFactory>   ()

        renderer(context, factory).apply {
            block(this)
        }

        val region = context.renderRegion

        verify(exactly = 0) { region.appendChild(any()) }
    }

    private fun renderer(
            context     : CanvasContext = mockk(),
            svgFactory  : SvgFactory = mockk(),
            htmleFactory: HtmlFactory = mockk(),
            textMetrics : TextMetrics = mockk()) = VectorRendererSvg(context, svgFactory, htmleFactory, textMetrics)
}
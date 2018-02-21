package com.nectar.doodle.drawing

import com.nectar.doodle.JsName
import com.nectar.doodle.drawing.Color.Companion.black
import com.nectar.doodle.drawing.Color.Companion.blue
import com.nectar.doodle.drawing.Color.Companion.cyan
import com.nectar.doodle.drawing.Color.Companion.green
import com.nectar.doodle.drawing.Color.Companion.red
import com.nectar.doodle.drawing.Color.Companion.transparent
import com.nectar.doodle.drawing.Color.Companion.white
import com.nectar.doodle.drawing.Color.Companion.yellow
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect

/**
 * Created by Nicholas Eddy on 2/19/18.
 */
class ColorTests {
    @Test @JsName("allTransparentEqual")
    fun `all transparent equal`() = expect(true) { transparent == Color(0xaaaaaa, 0f) }

    @Test @JsName("redNotEqualBlue")
    fun `red != blue`() = expect(false) { red == blue }

    @Test @JsName("invalidComponentsFails")
    fun `invalid components fails`() {
        assertFailsWith(IllegalArgumentException::class) { Color(300, 300, 300) }
    }

    @Test @JsName("invalidHexFails")
    fun `invalid hex fails`() {
        assertFailsWith(IllegalArgumentException::class) { Color(0xfffffff) }
    }

    @Test @JsName("invalidOpacityFails")
    fun `invalid opacity fails`() {
        assertFailsWith(IllegalArgumentException::class) { Color(0xff, 3f) }
    }

    @Test @JsName("transparentInvisible")
    fun `transparent invisible`() = listOf(Color(0xaaaaaa, 0f), transparent).forEach { expect(false) { it.visible } }

    @Test @JsName("inversion")
    fun `inversion works`() = listOf(white to black, red to cyan, yellow to blue, green to Color(0xff00ff)).forEach {
        expect(it.first ) { it.second.inverted }
        expect(it.second) { it.first.inverted  }
    }

    @Test @JsName("hexCorrect")
    fun `hexString correct`() = listOf(red to "ff0000", green to "00ff00", blue to "0000ff").forEach {
        expect(it.second) { it.first.hexString }
    }
}
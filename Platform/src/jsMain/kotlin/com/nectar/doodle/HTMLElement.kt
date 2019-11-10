package com.nectar.doodle

import org.w3c.dom.CharacterData
import org.w3c.dom.DOMRect
import org.w3c.dom.Document
import org.w3c.dom.DragEvent
import org.w3c.dom.Element
import org.w3c.dom.ElementCreationOptions
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLHeadElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.Text
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.css.ElementCSSInlineStyle
import org.w3c.dom.css.StyleSheet
import org.w3c.dom.css.StyleSheetList
import org.w3c.dom.events.Event

/**
 * Created by Nicholas Eddy on 8/9/19.
 */
actual typealias CSSStyleSheet = CSSStyleSheet

actual val com.nectar.doodle.CSSStyleSheet.numStyles: Int get() = this.cssRules.length

actual typealias CSSStyleDeclaration = CSSStyleDeclaration

actual var CSSStyleDeclaration.clipPath: String get() = this.asDynamic()["clip-path"]
    set(new) { this.asDynamic()["clip-path"] = new }

actual var CSSStyleDeclaration.willChange: String get() = this.asDynamic()["will-change"]
    set(new) { this.asDynamic()["will-change"] = new }

actual typealias ElementCSSInlineStyle = ElementCSSInlineStyle

actual typealias DOMRect   = DOMRect
actual typealias Element   = Element
actual typealias Event     = Event
actual typealias DragEvent = DragEvent

actual typealias HTMLElement = HTMLElement

actual var HTMLElement.onresize: ((Event) -> Unit)? get() = onresize
    set(new) { onresize = new }
actual var HTMLElement.ondragstart: ((DragEvent) -> Boolean)? get() = ondrag
    set(value) { ondragstart = value }

actual typealias ElementCreationOptions = ElementCreationOptions

actual typealias Document = Document

actual typealias Text              = Text
actual typealias CharacterData     = CharacterData
actual typealias HTMLHeadElement   = HTMLHeadElement
actual typealias HTMLImageElement  = HTMLImageElement
actual typealias HTMLInputElement  = HTMLInputElement
actual typealias HTMLButtonElement = HTMLButtonElement

actual typealias StyleSheet = StyleSheet

actual inline operator fun StyleSheetList.get(index: Int): StyleSheet? = item(index)

actual typealias StyleSheetList = StyleSheetList
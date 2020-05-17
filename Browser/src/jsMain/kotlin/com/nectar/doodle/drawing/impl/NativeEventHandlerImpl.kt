package com.nectar.doodle.drawing.impl

import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget


internal class NativeEventHandlerImpl(private val element: HTMLElement, private val listener: NativeEventListener): NativeEventHandler {

    override fun startConsumingMouseMoveEvents(onlySelf: Boolean) { element.onmousemove = { muteEvent(it, onlySelf) } }
    override fun stopConsumingMouseMoveEvents (                 ) { element.onmousemove = null                        }

    override fun startConsumingMousePressEvents() {
        element.onmouseup   = { muteEvent(it) }
        element.onmousedown = { muteEvent(it) }
        element.ondblclick  = { muteEvent(it) }
    }
    override fun stopConsumingMousePressEvents() {
        element.onmouseup   = null
        element.onmousedown = null
        element.ondblclick  = null
    }

    override fun startConsumingSelectionEvents() { element.onselect = { muteEvent(it) } }
    override fun stopConsumingSelectionEvents () { element.onselect = null              }

    override fun registerFocusListener  () {
        element.onblur  = { onBlur (it.target) }
        element.onfocus = { onFocus(it.target) }
    }

    override fun unregisterFocusListener() {
        element.onblur  = null
        element.onfocus = null
    }

    private fun focusIn(@Suppress("UNUSED_PARAMETER") event: Event? = null) {
        onFocusIn(event?.target)
    }

    private fun focusOut(@Suppress("UNUSED_PARAMETER") event: Event? = null) {
        onFocusOut(event?.target)
    }

    override fun registerFocusInListener() {
        element.addEventListener("focusin",  ::focusIn )
        element.addEventListener("focusout", ::focusOut)
    }

    override fun unregisterFocusInListener() {
        element.removeEventListener("focusin",  ::focusIn )
        element.removeEventListener("focusout", ::focusOut)
    }

    override fun registerKeyListener  () {
        element.onkeyup    = { onKeyUp   (it.target) }
        element.onkeydown  = { onKeyDown (it.target) }
        element.onkeypress = { onKeyPress(it.target) }
    }

    override fun unregisterKeyListener() {
        element.onkeyup    = null
        element.onkeydown  = null
        element.onkeypress = null
    }

    override fun registerClickListener   () { element.onclick  = { onClick(it.target) } }
    override fun unregisterClickListener () { element.onclick  = null          }

    override fun registerScrollListener  () { element.onscroll = { onScroll(it.target) } }
    override fun unregisterScrollListener() { element.onscroll = null           }

    override fun registerChangeListener  () { element.onchange = { onChange(it.target) } }
    override fun unregisterChangeListener() { element.onchange = null          }

    override fun registerInputListener   () { element.oninput  = { onInput(it.target) } }
    override fun unregisterInputListener () { element.oninput  = null          }

    private fun muteEvent(event: Event, onlySelf: Boolean = false): Boolean {
        if (onlySelf && event.target != element) {
            return true
        }

        event.stopPropagation()

        return false
    }

    private fun onBlur    (target: EventTarget?) = true.also { listener.onFocusLost  (target) }
    private fun onFocus   (target: EventTarget?) = true.also { listener.onFocusGained(target) }
    private fun onKeyUp   (target: EventTarget?) = true.also { listener.onKeyUp      (target) }
    private fun onKeyDown (target: EventTarget?) = true.also { listener.onKeyDown    (target) }
    private fun onKeyPress(target: EventTarget?) = true.also { listener.onKeyPress   (target) }
    private fun onClick   (target: EventTarget?) = true.also { listener.onClick      (target) }
    private fun onScroll  (target: EventTarget?) = true.also { listener.onScroll     (target) }
    private fun onChange  (target: EventTarget?) = true.also { listener.onChange     (target) }
    private fun onInput   (target: EventTarget?) = true.also { listener.onInput      (target) }

    private fun onFocusIn (target: EventTarget?) = true.also { listener.onFocusGained(target) }
    private fun onFocusOut(target: EventTarget?) = true.also { listener.onFocusLost  (target) }

}

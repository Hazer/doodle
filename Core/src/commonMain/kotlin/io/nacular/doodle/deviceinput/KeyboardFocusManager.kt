package io.nacular.doodle.deviceinput

import io.nacular.doodle.core.View
import io.nacular.doodle.event.KeyEvent
import io.nacular.doodle.event.KeyState
import io.nacular.doodle.focus.FocusManager
import io.nacular.doodle.focus.FocusTraversalPolicy.TraversalType
import io.nacular.doodle.focus.FocusTraversalPolicy.TraversalType.Backward
import io.nacular.doodle.focus.FocusTraversalPolicy.TraversalType.Downward
import io.nacular.doodle.focus.FocusTraversalPolicy.TraversalType.Forward
import io.nacular.doodle.focus.FocusTraversalPolicy.TraversalType.Upward
import io.nacular.doodle.system.KeyInputService
import io.nacular.doodle.system.KeyInputService.Listener
import io.nacular.doodle.utils.contains

/**
 * Created by Nicholas Eddy on 3/10/18.
 */

interface Listener {
    operator fun invoke(keyEvent: KeyEvent)
}

interface Preprocessor {
    operator fun invoke(keyEvent: KeyEvent)
}

interface Postprocessor {
    operator fun invoke(keyEvent: KeyEvent)
}

interface KeyboardFocusManager {
    fun shutdown()
}

class KeyboardFocusManagerImpl(
        private val keyInputService     : KeyInputService,
        private val focusManager        : FocusManager,
        private val defaultTraversalKeys: Map<TraversalType, Set<KeyState>>): KeyboardFocusManager, Listener {

    private var preprocessors  = mutableListOf<Preprocessor >()
    private var postprocessors = mutableListOf<Postprocessor>()

    init {
        keyInputService += this
    }

    override fun shutdown() {
        keyInputService -= this
    }

    override operator fun invoke(keyState: KeyState): Boolean {
        focusManager.focusOwner?.let { focusOwner ->
            val keyEvent = KeyEvent(focusOwner, keyState)

            preprocessKeyEvent(keyEvent)

            if (!keyEvent.consumed) {
                handleKeyEvent(focusOwner, keyEvent)
            }

            if (!keyEvent.consumed) {
                postprocessKeyEvent(keyEvent)
            }

            return !keyEvent.consumed
        }

        return false
    }

    operator fun plusAssign (preprocessor: Preprocessor) { preprocessors.add   (preprocessor) }
    operator fun minusAssign(preprocessor: Preprocessor) { preprocessors.remove(preprocessor) }

    operator fun plusAssign (postprocessor: Postprocessor) { postprocessors.add   (postprocessor) }
    operator fun minusAssign(postprocessor: Postprocessor) { postprocessors.remove(postprocessor) }

    private fun handleKeyEvent(view: View, keyEvent: KeyEvent) {
        val keyState = keyEvent.run { KeyState(code, key, modifiers, type) }

        val upwardKeyEvents   = view[Upward  ] ?: defaultTraversalKeys[Upward  ]
        val forwardKeyEvents  = view[Forward ] ?: defaultTraversalKeys[Forward ]
        val backwardKeyEvents = view[Backward] ?: defaultTraversalKeys[Backward]
        val downwardKeyEvents = if (view.isFocusCycleRoot_) view[Downward] else null

        when (keyState) {
            in forwardKeyEvents  -> focusManager.moveFocusForward (view)
            in backwardKeyEvents -> focusManager.moveFocusBackward(view)
            in upwardKeyEvents   -> focusManager.moveFocusUpward  (view)
            in downwardKeyEvents -> focusManager.moveFocusDownward(view)
            else                 -> {
//            var g: View? = view

//            while (g != null) {
//                if (g.monitorsKeyboard) {
                view.handleKeyEvent_(keyEvent)
//                    break
//                } else {
//                    g = g.parent
//                }
//            }
            }
        }

        keyEvent.consume()
    }

    private fun preprocessKeyEvent(event: KeyEvent) {
        preprocessors.forEach {
            it(event)
            if (event.consumed) {
                return
            }
        }
    }

    private fun postprocessKeyEvent(event: KeyEvent) {
        postprocessors.forEach {
            it(event)
            if (event.consumed) {
                return
            }
        }
    }
}

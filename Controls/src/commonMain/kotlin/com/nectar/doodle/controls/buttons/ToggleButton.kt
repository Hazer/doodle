package com.nectar.doodle.controls.buttons

import com.nectar.doodle.core.Icon
import com.nectar.doodle.utils.PropertyObservers
import com.nectar.doodle.utils.PropertyObserversImpl


@Suppress("PrivatePropertyName")
open class ToggleButton(text: String = "", icon: Icon<Button>? = null): PushButton(text, icon, ToggleButtonModel()) {
    private class ToggleButtonModel: ButtonModelImpl() {
        override var selected
            get(   ) = super.selected
            set(new) {
                if (tempSelected != new) {
                    tempSelected = new

                    buttonGroup?.let {
                        callCount++
                        it.setSelected(this, new)
                        callCount--

                        if (callCount > 0) {
                            return
                        }
                    }

                    super.selected = tempSelected
                }
            }

        private var callCount    = 0
        private var tempSelected = super.selected

        override var pressed
            get(   ) = super.pressed
            set(new) {
                if (super.pressed != new) {
                    super.pressed = new

                    if (!pressed && armed) {
                        selected = !selected
                    }
                }
            }
    }

    val selectedChanged: PropertyObservers<ToggleButton, Boolean> by lazy { PropertyObserversImpl<ToggleButton, Boolean>(this) }

    private val selectedChanged_ = { _: ButtonModel, old: Boolean, new: Boolean -> (selectedChanged as PropertyObserversImpl)(old, new) }

    override fun addedToDisplay() {
        super.addedToDisplay()

        model.selectedChanged += selectedChanged_
    }

    override fun removedFromDisplay() {
        super.removedFromDisplay()

        model.selectedChanged -= selectedChanged_
    }

    // FIXME: It is possible to assign a non-ToggleButtonModel to these types.  Is that desirable?
    override var model get() = super.model
        set(new) {
            super.model.selectedChanged -= selectedChanged_

            super.model = new

            if (displayed) {
                super.model.selectedChanged += selectedChanged_
            }
        }
}

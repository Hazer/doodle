package io.nacular.doodle.controls.spinner

class LongModel(private val progression: LongProgression): CommonSpinnerModel<Long>() {

    override val hasNext     get() = value + progression.step <= progression.last
    override val hasPrevious get() = value - progression.step >= progression.first

    override fun next    () { if (hasNext    ) { value += progression.step } }
    override fun previous() { if (hasPrevious) { value -= progression.step } }

    override var value = progression.first
        private set(new) {
            if (new == field) { return }

            field = new

            changed_()
        }
}
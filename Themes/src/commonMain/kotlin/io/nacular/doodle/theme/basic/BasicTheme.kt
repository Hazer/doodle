package io.nacular.doodle.theme.basic

import io.nacular.doodle.controls.MutableListModel
import io.nacular.doodle.controls.ProgressBar
import io.nacular.doodle.controls.ProgressIndicator
import io.nacular.doodle.controls.range.Slider
import io.nacular.doodle.controls.buttons.Button
import io.nacular.doodle.controls.buttons.CheckBox
import io.nacular.doodle.controls.buttons.RadioButton
import io.nacular.doodle.controls.buttons.Switch
import io.nacular.doodle.controls.list.List
import io.nacular.doodle.controls.list.MutableList
import io.nacular.doodle.controls.panels.SplitPanel
import io.nacular.doodle.controls.panels.TabbedPanel
import io.nacular.doodle.controls.spinner.Spinner
import io.nacular.doodle.controls.table.MutableTable
import io.nacular.doodle.controls.table.Table
import io.nacular.doodle.controls.text.Label
import io.nacular.doodle.controls.theme.LabelBehavior
import io.nacular.doodle.controls.tree.MutableTree
import io.nacular.doodle.controls.tree.Tree
import io.nacular.doodle.controls.tree.TreeModel
import io.nacular.doodle.core.Behavior
import io.nacular.doodle.core.Display
import io.nacular.doodle.core.View
import io.nacular.doodle.drawing.Brush
import io.nacular.doodle.drawing.Color
import io.nacular.doodle.drawing.Color.Companion.Black
import io.nacular.doodle.drawing.Color.Companion.Blue
import io.nacular.doodle.drawing.Color.Companion.Gray
import io.nacular.doodle.drawing.Color.Companion.White
import io.nacular.doodle.drawing.ColorBrush
import io.nacular.doodle.drawing.grayScale
import io.nacular.doodle.drawing.lighter
import io.nacular.doodle.theme.Modules
import io.nacular.doodle.theme.Modules.Companion.ThemeModule
import io.nacular.doodle.theme.Modules.Companion.bindBehavior
import io.nacular.doodle.theme.adhoc.DynamicTheme
import io.nacular.doodle.theme.basic.list.BasicListBehavior
import io.nacular.doodle.theme.basic.list.BasicMutableListBehavior
import io.nacular.doodle.theme.basic.tabbedpanel.BasicTabProducer
import io.nacular.doodle.theme.basic.table.BasicMutableTableBehavior
import io.nacular.doodle.theme.basic.table.BasicTableBehavior
import io.nacular.doodle.theme.basic.tree.BasicMutableTreeBehavior
import io.nacular.doodle.theme.basic.tree.BasicTreeBehavior
import org.kodein.di.Kodein
import org.kodein.di.Kodein.Module
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.instanceOrNull
import org.kodein.di.erased.provider
import org.kodein.di.erased.singleton
import org.kodein.di.erasedSet

/**
 * Created by Nicholas Eddy on 2/12/18.
 */
private typealias ListModel<T>        = io.nacular.doodle.controls.ListModel<T>
private typealias SpinnerModel<T>     = io.nacular.doodle.controls.spinner.Model<T>
private typealias MutableTreeModel<T> = io.nacular.doodle.controls.tree.MutableTreeModel<T>
private typealias BTheme              = BasicTheme

@Suppress("UNCHECKED_CAST")
open class BasicTheme(private val configProvider: ConfigProvider, behaviors: Iterable<Modules.BehaviorResolver>): DynamicTheme(behaviors.filter { it.theme == BTheme::class }) {
    override fun install(display: Display, all: Sequence<View>) {
        configProvider.config = config

        super.install(display, all)
    }

    open val config = object: BasicThemeConfig {}

    override fun toString() = this::class.simpleName ?: ""

    interface BasicThemeConfig {
        val borderColor            get() = Color(0x888888u)
        val oddRowColor            get() = foregroundColor.inverted
        val evenRowColor           get() = lightBackgroundColor
        val selectionColor         get() = Color(0x0063e1u)
        val foregroundColor        get() = Black
        val backgroundColor        get() = Color(0xccccccu)
        val darkBackgroundColor    get() = Color(0xaaaaaau)
        val lightBackgroundColor   get() = Color(0xf3f4f5u)
        val defaultBackgroundColor get() = backgroundColor
    }

    interface ConfigProvider {
        var config: BasicThemeConfig
    }

    private class ConfigProviderImpl: ConfigProvider {
        override var config = object: BasicThemeConfig {}
    }

    companion object {
        internal fun BasicModule(name: String, init: Kodein.Builder.() -> Unit) = Module(name = name) {
            importOnce(Config, allowOverride = true)

            init()
        }

        val BasicTheme = BasicModule(name = "BasicTheme") {
            importOnce(ThemeModule, allowOverride = true)

            bind<BasicTheme>() with singleton { BasicTheme(instance(), Instance(erasedSet())) }
        }

        private val Config = Module(name = "BasicThemeConfig") {
            bind<ConfigProvider>  () with singleton { ConfigProviderImpl()              }
            bind<BasicThemeConfig>() with provider  { instance<ConfigProvider>().config }
        }

        fun basicListBehavior(
                rowHeight            : Double? = null,
                evenRowColor         : Color?  = null,
                oddRowColor          : Color?  = null,
                selectionColor       : Color?  = null,
                selectionBlurredColor: Color?  = null) = BasicModule(name = "BasicListBehavior") {
            bindBehavior<List<Any, ListModel<Any>>>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run {
                    BasicListBehavior(
                            focusManager          = instanceOrNull(),
                            textMetrics           = instance(),
                            evenRowColor          = evenRowColor          ?: this.evenRowColor,
                            oddRowColor           = oddRowColor           ?: this.oddRowColor,
                            selectionColor        = selectionColor        ?: this.selectionColor,
                            selectionBlurredColor = selectionBlurredColor ?: this.selectionColor.grayScale().lighter(),
                            rowHeight             = rowHeight             ?: 20.0
                    )
                }
            }
        }

        fun basicTreeBehavior(
                rowHeight            : Double?              = null,
                evenRowColor         : Color?               = null,
                oddRowColor          : Color?               = null,
                selectionColor       : Color?               = null,
                selectionBlurredColor: Color?               = null,
                iconFactory          : (() -> TreeRowIcon)? = null) = BasicModule(name = "BasicTreeBehavior") {
            bindBehavior<Tree<Any,TreeModel<Any>>>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run {
                    BasicTreeBehavior(
                            focusManager          = instanceOrNull(),
                            textMetrics           = instance(),
                            rowHeight             = rowHeight             ?: 20.0,
                            evenRowColor          = evenRowColor          ?: this.evenRowColor,
                            oddRowColor           = oddRowColor           ?: this.oddRowColor,
                            selectionColor        = selectionColor        ?: this.selectionColor,
                            selectionBlurredColor = selectionBlurredColor ?: this.selectionColor.grayScale().lighter(),
                            iconFactory           = iconFactory           ?: { SimpleTreeRowIcon(foregroundColor, foregroundColor.inverted) }
                    )
                }
            }
        }

        val BasicLabelBehavior = BasicModule(name = "BasicLabelBehavior") {
            bindBehavior<Label>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { LabelBehavior(foregroundColor) }
            }
        }

        val BasicTableBehavior = BasicModule(name = "BasicTableBehavior") {
            bindBehavior<Table<Any, ListModel<Any>>>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicTableBehavior(instanceOrNull(), 20.0, backgroundColor, evenRowColor, oddRowColor, selectionColor, selectionColor.grayScale().lighter()) }
            }
        }

        fun basicButtonBehavior(
                backgroundColor    : Color?  = null,
                darkBackgroundColor: Color?  = null,
                foregroundColor    : Color?  = null,
                borderColor        : Color?  = null,
                borderWidth        : Double? = null,
                cornerRadius       : Double? = null,
                insets             : Double? = null) = BasicModule(name = "BasicButtonBehavior") {
            bindBehavior<Button>(BTheme::class) {
                val defaultCornerRadius = when (it.parent) {
                    is Spinner<*,*> -> 0.0
                    else            -> 4.0
                }

                it.behavior = instance<BasicThemeConfig>().run {
                    BasicButtonBehavior(
                            instance(),
                            backgroundColor     = backgroundColor     ?: this.backgroundColor,
                            darkBackgroundColor = darkBackgroundColor ?: this.darkBackgroundColor,
                            foregroundColor     = foregroundColor     ?: this.foregroundColor,
                            borderColor         = borderColor         ?: this.borderColor,
                            borderWidth         = borderWidth         ?: 0.0,
                            cornerRadius        = cornerRadius        ?: defaultCornerRadius,
                            insets              = insets              ?: 8.0)
                }
            }
        }

        fun basicSliderBehavior(barColor: Color? = null, knobColor: Color? = null, grooveThicknessRatio: Float? = null) = BasicModule(name = "BasicSliderBehavior") {
            bindBehavior<Slider>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run {
                    BasicSliderBehavior(
                            barColor             ?: defaultBackgroundColor,
                            knobColor            ?: darkBackgroundColor,
                            grooveThicknessRatio ?: 0.6f,
                            instanceOrNull()
                    )
                }
            }
        }

        val BasicSpinnerBehavior = BasicModule(name = "BasicSpinnerBehavior") {
            bindBehavior<Spinner<Any, SpinnerModel<Any>>>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicSpinnerBehavior(instance()) }
            }
        }

        val BasicCheckBoxBehavior = BasicModule(name = "BasicCheckBoxBehavior") {
            bindBehavior<CheckBox>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicCheckBoxBehavior(instance()) as Behavior<Button> }
            }
        }

        val BasicSplitPanelBehavior = BasicModule(name = "BasicSplitPanelBehavior") {
            bindBehavior<SplitPanel>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicSplitPanelBehavior() }
            }
        }

        val BasicRadioButtonBehavior = BasicModule(name = "BasicRadioButtonBehavior") {
            bindBehavior<RadioButton>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicRadioBehavior(instance()) as Behavior<Button> }
            }
        }

        fun basicSwitchBehavior(
                onBackground      : Color? = null,
                onForeground      : Color? = null,
                offBackground     : Color? = null,
                offForeground     : Color? = null,
                disabledBackground: Color? = null,
                disabledForeground: Color? = null) = BasicModule(name = "BasicSwitchBehavior") {
            bindBehavior<Switch>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run {
                    BasicSwitchBehavior(
                            onBackground       ?: Blue,
                            onForeground       ?: White,
                            offBackground      ?: backgroundColor,
                            offForeground      ?: onForeground  ?: White,
                            disabledBackground ?: offForeground ?: backgroundColor,
                            disabledForeground ?: Gray) as Behavior<Button>
                }
            }
        }

        val BasicMutableListBehavior = BasicModule(name = "BasicMutableListBehavior") {
            bindBehavior<MutableList<Any, MutableListModel<Any>>>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicMutableListBehavior(instanceOrNull(), instance(), evenRowColor, oddRowColor, selectionColor, selectionColor.grayScale().lighter()) }
            }
        }

        fun basicProgressBarBehavior(
                backgroundBrush: Brush? = null,
                fillBrush      : Brush? = null,
                outlineColor   : Color? = null,
                cornerRadius   : Double = 2.0): Module = BasicModule(name = "BasicProgressBarBehavior") {
            bindBehavior<ProgressBar>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run {
                    BasicProgressBarBehavior(
                            backgroundBrush ?: ColorBrush(defaultBackgroundColor),
                            fillBrush       ?: ColorBrush(darkBackgroundColor   ),
                            outlineColor,
                            cornerRadius) as Behavior<ProgressIndicator> }
            }
        }

        val BasicMutableTreeBehavior = BasicModule(name = "BasicMutableTreeBehavior") {
            bindBehavior<MutableTree<Any, MutableTreeModel<Any>>>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicMutableTreeBehavior(instance(), evenRowColor, oddRowColor, selectionColor, selectionColor.grayScale().lighter(), focusManager = instanceOrNull()) }
            }
        }

        val BasicMutableTableBehavior = BasicModule(name = "BasicMutableTableBehavior") {
            bindBehavior<MutableTable<Any, MutableListModel<Any>>>(BTheme::class) {
                it.behavior = instance<BasicThemeConfig>().run { BasicMutableTableBehavior(instanceOrNull(), 20.0, backgroundColor, evenRowColor, oddRowColor, selectionColor, selectionColor.grayScale().lighter()) }
            }
        }

        val BasicTabbedPanelBehavior = BasicModule(name = "BasicTabbedPanelBehavior") {
            bind<io.nacular.doodle.theme.basic.tabbedpanel.BasicTabbedPanelBehavior<Any>>() with singleton {
                instance<BasicThemeConfig>().run {
                    io.nacular.doodle.theme.basic.tabbedpanel.BasicTabbedPanelBehavior<Any>(
                            BasicTabProducer(instance(), { _,_,index ->
                                "tab: $index"
                            }, tabColor = backgroundColor),
                            backgroundColor)
                }
            }

            bindBehavior<TabbedPanel<Any>>(BTheme::class) {
                it.behavior = instance<io.nacular.doodle.theme.basic.tabbedpanel.BasicTabbedPanelBehavior<Any>>()
            }
        }

        val BasicThemeBehaviors = Module(name = "BasicThemeBehaviors") {
            importAll(listOf(
                    basicListBehavior(),
                    basicTreeBehavior(),
                    BasicLabelBehavior,
                    BasicTableBehavior,
                    basicButtonBehavior(),
                    basicSliderBehavior(),
                    BasicSpinnerBehavior,
                    BasicCheckBoxBehavior,
                    BasicSplitPanelBehavior,
                    BasicRadioButtonBehavior,
                    BasicMutableListBehavior,
                    basicProgressBarBehavior(),
                    BasicMutableTreeBehavior,
                    BasicMutableTableBehavior,
                    basicSwitchBehavior()),
                    allowOverride = true)
        }
    }
}

class DarkBasicTheme(configProvider: ConfigProvider, behaviors: Iterable<Modules.BehaviorResolver>): BasicTheme(configProvider, behaviors) {
    class DarkBasicThemeConfig: BasicThemeConfig {
        override val borderColor            = super.borderColor.inverted
        override val foregroundColor        = super.foregroundColor.inverted
        override val backgroundColor        = super.backgroundColor.inverted
        override val darkBackgroundColor    = super.darkBackgroundColor.inverted
        override val lightBackgroundColor   = Color(0x282928u)
        override val defaultBackgroundColor = super.defaultBackgroundColor.inverted
    }

    override val config = DarkBasicThemeConfig()

    companion object {
        val DarkBasicTheme = BasicModule(name = "DarkBasicTheme") {
            bind<DarkBasicTheme>() with singleton { DarkBasicTheme(instance(), Instance(erasedSet())) }
        }
    }
}
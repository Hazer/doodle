package com.nectar.doodle.application

import com.nectar.doodle.HTMLElement
import com.nectar.doodle.accessibility.AccessibilityManagerImpl
import com.nectar.doodle.core.Display
import com.nectar.doodle.core.InternalDisplay
import com.nectar.doodle.core.impl.DisplayImpl
import com.nectar.doodle.datatransport.dragdrop.DragManager
import com.nectar.doodle.deviceinput.KeyboardFocusManager
import com.nectar.doodle.deviceinput.PointerInputManager
import com.nectar.doodle.dom.ElementRuler
import com.nectar.doodle.dom.HtmlFactory
import com.nectar.doodle.dom.SvgFactory
import com.nectar.doodle.dom.SvgFactoryImpl
import com.nectar.doodle.dom.SystemStyler
import com.nectar.doodle.dom.SystemStylerImpl
import com.nectar.doodle.dom.impl.ElementRulerImpl
import com.nectar.doodle.dom.impl.HtmlFactoryImpl
import com.nectar.doodle.drawing.CanvasFactory
import com.nectar.doodle.drawing.GraphicsDevice
import com.nectar.doodle.drawing.RenderManager
import com.nectar.doodle.drawing.TextFactory
import com.nectar.doodle.drawing.TextMetrics
import com.nectar.doodle.drawing.impl.CanvasFactoryImpl
import com.nectar.doodle.drawing.impl.GraphicsSurfaceFactory
import com.nectar.doodle.drawing.impl.RealGraphicsDevice
import com.nectar.doodle.drawing.impl.RealGraphicsSurfaceFactory
import com.nectar.doodle.drawing.impl.RenderManagerImpl
import com.nectar.doodle.drawing.impl.TextFactoryImpl
import com.nectar.doodle.drawing.impl.TextMetricsImpl
import com.nectar.doodle.scheduler.AnimationScheduler
import com.nectar.doodle.scheduler.Scheduler
import com.nectar.doodle.scheduler.Strand
import com.nectar.doodle.scheduler.Task
import com.nectar.doodle.scheduler.impl.AnimationSchedulerImpl
import com.nectar.doodle.scheduler.impl.SchedulerImpl
import com.nectar.doodle.scheduler.impl.StrandImpl
import com.nectar.doodle.system.SystemPointerEvent
import com.nectar.doodle.system.impl.PointerInputServiceStrategy
import com.nectar.doodle.system.impl.PointerInputServiceStrategy.EventHandler
import com.nectar.doodle.time.Timer
import com.nectar.doodle.time.impl.PerformanceTimer
import org.kodein.di.Copy
import org.kodein.di.DKodein
import org.kodein.di.Kodein
import org.kodein.di.Kodein.Module
import org.kodein.di.bindings.NoArgSimpleBindingKodein
import org.kodein.di.erased.bind
import org.kodein.di.erased.instance
import org.kodein.di.erased.instanceOrNull
import org.kodein.di.erased.singleton
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.Window
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import kotlin.browser.document
import kotlin.browser.window

/**
 * Created by Nicholas Eddy on 1/22/20.
 */
inline fun <reified T: Application> application(
                 root                : HTMLElement = document.body!!,
                 allowDefaultDarkMode: Boolean     = false,
                 modules             : List<Module> = emptyList(),
        noinline creator             : NoArgSimpleBindingKodein<*>.() -> T): Application = createApplication(Kodein.direct {
    bind<Application>() with singleton(creator = creator)
}, root, allowDefaultDarkMode, modules)

inline fun <reified T: Application> nestedApplication(
                 view                : ApplicationView,
                 root                : HTMLElement  = document.body!!,
                 allowDefaultDarkMode: Boolean      = false,
                 modules             : List<Module> = emptyList(),
        noinline creator             : NoArgSimpleBindingKodein<*>.() -> T): Application = createNestedApplication(view, Kodein.direct {
    bind<Application>() with singleton(creator = creator)
}, root, allowDefaultDarkMode, modules)

fun createApplication(
        injector            : DKodein,
        root                : HTMLElement,
        allowDefaultDarkMode: Boolean,
        modules             : List<Module>): Application = ApplicationHolderImpl(injector, root, allowDefaultDarkMode, modules)

fun createNestedApplication(
        view                : ApplicationView,
        injector            : DKodein,
        root                : HTMLElement,
        allowDefaultDarkMode: Boolean,
        modules             : List<Module>): Application = NestedApplicationHolder(view, injector, root, allowDefaultDarkMode, modules)

private class NestedPointerInputStrategy(private val view: ApplicationView, private val delegate: PointerInputServiceStrategy): PointerInputServiceStrategy by(delegate) {
    override fun startUp(handler: EventHandler) {
        // Provide an adapter to handle mapping pointer location correctly based on ApplicationView's orientation
        delegate.startUp(object: EventHandler {
            override fun handle(event: SystemPointerEvent) {
                handler.handle(SystemPointerEvent(
                        event.type,
                        view.fromAbsolute(pointerLocation),
                        event.buttons,
                        event.clickCount,
                        event.modifiers,
                        event.nativeScrollPanel))
            }
        })
    }
}

private class NestedApplicationHolder(
        view                : ApplicationView,
        previousInjector    : DKodein,
        root                : HTMLElement = document.body!!,
        allowDefaultDarkMode: Boolean = false,
        modules             : List<Module> = emptyList()): ApplicationHolderImpl(previousInjector, root, allowDefaultDarkMode, modules, isNested = true) {

    init {
        injector.instanceOrNull<PointerInputServiceStrategy>()?.let {
            injector = Kodein.direct {
                extend(injector, copy = Copy.All)

                bind<PointerInputServiceStrategy>(overrides = true) with singleton { NestedPointerInputStrategy(view, it) }
            }
        }

        run()
    }
}

private open class ApplicationHolderImpl protected constructor(
                    previousInjector    : DKodein,
        private val root                : HTMLElement  = document.body!!,
                    allowDefaultDarkMode: Boolean      = false,
                    modules             : List<Module> = emptyList(),
        private val isNested            : Boolean      = false): Application {
    protected var injector = Kodein.direct {
        extend(previousInjector, copy = Copy.All)

        bind<Window>                   () with instance  ( window )

        bind<Timer>                    () with singleton { PerformanceTimer          (window.performance                                                    ) }
        bind<Strand>                   () with singleton { StrandImpl                (instance(), instance()                                                ) }
        bind<Display>                  () with singleton { DisplayImpl               (instance(), instance(), root                                          ) }
        bind<Scheduler>                () with singleton { SchedulerImpl             (instance(), instance()                                                ) }
        bind<SvgFactory>               () with singleton { SvgFactoryImpl            (root, document                                                        ) }
        bind<HtmlFactory>              () with singleton { HtmlFactoryImpl           (root, document                                                        ) }
        bind<TextFactory>              () with singleton { TextFactoryImpl           (instance()                                                            ) }
        bind<TextMetrics>              () with singleton { TextMetricsImpl           (instance(), instance(), instance()                                    ) }
        bind<ElementRuler>             () with singleton { ElementRulerImpl          (instance()                                                            ) }
        if (!isNested) {
            bind<SystemStyler>() with singleton { SystemStylerImpl(instance(), document, allowDefaultDarkMode) }
        }
        bind<CanvasFactory>            () with singleton { CanvasFactoryImpl         (instance(), instance(), instance(), instance()                        ) }
        bind<RenderManager>            () with singleton { RenderManagerImpl         (instance(), instance(), instanceOrNull(), instanceOrNull(), instance()) }
        bind<GraphicsDevice<*>>        () with singleton { RealGraphicsDevice        (instance()                                                            ) }
        bind<AnimationScheduler>       () with singleton { AnimationSchedulerImpl    (instance()                                                            ) } // FIXME: Provide fallback in case not supported
        bind<GraphicsSurfaceFactory<*>>() with singleton { RealGraphicsSurfaceFactory(instance(), instance()                                                ) }

        // TODO: Can this be handled better?
        bind<DisplayImpl>              () with singleton { instance<Display>    () as DisplayImpl }
        bind<InternalDisplay>          () with singleton { instance<DisplayImpl>()                }

        modules.forEach {
            import(it, allowOverride = true)
        }
    }

    private var initTask    = null as Task?
    private var isShutdown  = false
    private var application = null as Application?

    private fun onUnload(@Suppress("UNUSED_PARAMETER") event: Event? = null) {
        shutdown()
    }

    private var mutations: MutationObserver? = null

    protected fun run() {
        window.addEventListener("unload", ::onUnload)

        root.parentNode?.let { parent ->
            mutations = MutationObserver { mutations, _ ->
                mutations.flatMap { it.removedNodes.asList() }.firstOrNull { root == it }?.let {
                    shutdown()
                }
            }.apply {
                observe(parent, object: MutationObserverInit {
                    override var childList: Boolean? = true
                })
            }
        }

        // Initialize framework components
        if (!isNested) {
            injector.instance<SystemStyler>()
        }
        injector.instance<RenderManager>()

        injector.instanceOrNull<PointerInputManager>   ()
        injector.instanceOrNull<KeyboardFocusManager>()
        injector.instanceOrNull<DragManager>         ()

        initTask = injector.instance<Scheduler>().now {
            application = injector.instance()
        }
    }

    override fun shutdown() {
        if (isShutdown) {
            return
        }

        window.removeEventListener("unload", ::onUnload)

        mutations?.disconnect()

        initTask?.cancel()

        injector.instance<DisplayImpl> ().shutdown()
        if (!isNested) {
            injector.instance<SystemStyler>().shutdown()
        }
        injector.instanceOrNull<DragManager>             ()?.shutdown()
        injector.instanceOrNull<PointerInputManager>       ()?.shutdown()
        injector.instanceOrNull<KeyboardFocusManager>    ()?.shutdown()
        injector.instanceOrNull<AccessibilityManagerImpl>()?.shutdown()

        application?.shutdown()

        injector = Kodein.direct {}

        isShutdown = true
    }

    companion object {
        operator fun invoke(previousInjector    : DKodein,
                            root                : HTMLElement  = document.body!!,
                            allowDefaultDarkMode: Boolean      = false,
                            modules             : List<Module> = emptyList()): ApplicationHolderImpl {
            return ApplicationHolderImpl(previousInjector, root, allowDefaultDarkMode, modules).apply { run() }
        }
    }
}
package fr.wolfdev.wolflin.lang

import net.minecraftforge.eventbus.EventBusErrorMessage
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.eventbus.api.IEventListener
import net.minecraftforge.fml.*
import net.minecraftforge.fml.Logging.LOADING
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.ModFileScanData
import org.apache.logging.log4j.LogManager
import java.util.function.Consumer
import java.util.function.Supplier

class WolflinModContainer(info: IModInfo, className: String, val modClassLoader: ClassLoader, val modFileScanData: ModFileScanData): ModContainer(info) {
    private val LOGGER = LogManager.getLogger()
    private var eventBus: IEventBus
    private lateinit var modInstance: Any
    private var modClass: Class<*>

    init {
        LOGGER.debug(LOADING, "Creating WolflinModContainer instance for $className with classLoader $modClassLoader & ${javaClass.classLoader}")
        this.triggerMap[ModLoadingStage.CONSTRUCT] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.constructMod(it)}.andThen {this.afterEvent(it)}
        this.triggerMap[ModLoadingStage.CREATE_REGISTRIES] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.fireEvent(it)}.andThen {this.afterEvent(it)}
        this.triggerMap[ModLoadingStage.LOAD_REGISTRIES] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.fireEvent(it)}.andThen {this.afterEvent(it)}
        this.triggerMap[ModLoadingStage.COMMON_SETUP] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.preinitMod(it)}.andThen {this.fireEvent(it)}.andThen {this.afterEvent(it)}
        this.triggerMap[ModLoadingStage.SIDED_SETUP] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.fireEvent(it)}.andThen {this.afterEvent(it)}
        this.triggerMap[ModLoadingStage.ENQUEUE_IMC] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.initMod(it)}.andThen {this.fireEvent(it)}.andThen {this.afterEvent(it)}
        this.triggerMap[ModLoadingStage.PROCESS_IMC] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.fireEvent(it)}.andThen {this.afterEvent(it)}
        this.triggerMap[ModLoadingStage.COMPLETE] = this.dummy().andThen {this.beforeEvent(it)}.andThen {this.completeLoading(it)}.andThen {this.fireEvent(it)}.andThen {this.afterEvent(it)}

        eventBus = IEventBus.create(this::onEventFailed)
        try {
            modClass = Class.forName(className, true, modClassLoader)
            LOGGER.debug(LOADING, "Loaded modclass ${modClass.name} with ${modClass.classLoader}")
        } catch(e: Throwable) {
            LOGGER.error(LOADING, "Failed to load class $className", e)
            throw ModLoadingException(modInfo, ModLoadingStage.CONSTRUCT, "Wut!?!", e)
        }
    }

    private fun completeLoading(lifecycleEvent: LifecycleEventProvider.LifecycleEvent) {
    }

    private fun initMod(lifecycleEvent: LifecycleEventProvider.LifecycleEvent) {
    }

    private fun dummy(): Consumer<LifecycleEventProvider.LifecycleEvent> = Consumer {}
    private fun onEventFailed(eventBus: IEventBus, event: Event, eventListener: Array<IEventListener>, i: Int, throwable: Throwable) {
        LOGGER.error(EventBusErrorMessage(event, i, eventListener, throwable))
    }

    private fun beforeEvent(lifecycleEvent: LifecycleEventProvider.LifecycleEvent) {
        WolflinModLoadingContext.get().activeContainer = this
        ModThreadContext.get().activeContainer = this
    }

    private fun fireEvent(lifecycleEvent: LifecycleEventProvider.LifecycleEvent) {
        val event = lifecycleEvent.getOrBuildEvent(this)
        LOGGER.debug(LOADING, "Firing event to modid ${getModId()} : ${event.javaClass.name}")
        try {
            eventBus.post(event)
            LOGGER.debug(LOADING, "Fired event to modid ${getModId()} : ${event.javaClass.name}")
        } catch(e: Throwable) {
            LOGGER.error(LOADING, "Caught exception during event $event dispatch for modid ${getModId()} ")
            throw ModLoadingException(modInfo, lifecycleEvent.fromStage(), "Error firing event", e)
        }
    }

    private fun afterEvent(lifecycleEvent: LifecycleEventProvider.LifecycleEvent) {
        ModThreadContext.get().activeContainer = null
        WolflinModLoadingContext.get().activeContainer = null
        if(currentState == ModLoadingStage.ERROR) {
            LOGGER.error(LOADING, "An error occurred while dispatching event ${lifecycleEvent.fromStage()} to ${getModId()}")
        }
    }

    private fun preinitMod(lifecycleEvent: LifecycleEventProvider.LifecycleEvent) {
    }

    private fun constructMod(lifecycleEvent: LifecycleEventProvider.LifecycleEvent) {
        try {
            LOGGER.debug(LOADING, "Loading mod instance ${getModId()} of type ${modClass.name}")
            this.modInstance = modClass.kotlin.objectInstance ?: modClass.newInstance()
            LOGGER.debug(LOADING, "Loaded mod instance ${getModId()} of type ${modClass.name}")
        } catch(e: Throwable) {
            LOGGER.error(LOADING, "Failed to create mod instance. ModId ${getModId()} for class ${modClass.name}", e)
            throw ModLoadingException(modInfo, lifecycleEvent.fromStage(), "Failed to load mod", e, modClass)
        }
        LOGGER.debug(LOADING, "Injecting Automatic event subscribers for ${getModId()}")
        WolflinAutomaticEventSubscriber.inject(this, modFileScanData, modClassLoader)
        LOGGER.debug(LOADING, "Completed Automatic event subscribers for ${getModId()}")
    }

    override fun matches(mod: Any?) = mod == modInstance
    override fun getMod() = this.modInstance
    fun getEventBus(): IEventBus = this.eventBus
}

fun LifecycleEventProvider.LifecycleEvent.getOrBuildEvent(modContainer: ModContainer): Event {
    val field = javaClass.getDeclaredField("customEventSupplier")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val customEventSupplier = field.get(this) as Supplier<Event>?
    if(customEventSupplier != null) {
        return customEventSupplier.get()
    }
    return this.fromStage().getModEvent(modContainer)
}


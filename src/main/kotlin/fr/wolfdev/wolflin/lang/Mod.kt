package fr.wolfdev.wolflin.lang

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.IEventBus
import java.util.function.Supplier

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Mod(val value: String) {
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.CLASS)
    annotation class EventBusSubscriber(val value: Array<Dist> = [Dist.CLIENT, Dist.DEDICATED_SERVER], val modid: String, val bus: Bus = Bus.FORGE) {
        enum class Bus(private val eventBusSupplier: Supplier<IEventBus>) {
            FORGE(Supplier {MinecraftForge.EVENT_BUS}),
            MOD(Supplier {WolflinModLoadingContext.get().getModEventBus()});

            fun bus() = eventBusSupplier
        }
    }
}
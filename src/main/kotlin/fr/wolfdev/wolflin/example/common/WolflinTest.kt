package fr.wolfdev.wolflin.example.common

import fr.wolfdev.wolflin.lang.Mod
import fr.wolfdev.wolflin.lang.WolflinModLoadingContext
import net.minecraft.init.Blocks
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent
import org.apache.logging.log4j.LogManager
import java.util.stream.Collectors

@Mod(WolflinTest.MODID)
class WolflinTest {
    companion object {
        const val MODID = "wolflin"
        val LOGGER = LogManager.getLogger()
    }

    init {
        WolflinModLoadingContext.get().getModEventBus().addListener<FMLCommonSetupEvent> {this.setup(it)}
        WolflinModLoadingContext.get().getModEventBus().addListener<InterModEnqueueEvent> {this.enqueueIMC(it)}
        WolflinModLoadingContext.get().getModEventBus().addListener<InterModProcessEvent> {this.processIMC(it)}
        WolflinModLoadingContext.get().getModEventBus().addListener<FMLClientSetupEvent> {this.doClientStuff(it)}
    }

    private fun setup(event: FMLCommonSetupEvent) {
        LOGGER.info("HELLO FROM PREINIT")
        LOGGER.info("DIRT BLOCK >> ${Blocks.DIRT.registryName}")
    }

    private fun doClientStuff(event: FMLClientSetupEvent) {
        // do something that can only be done on the client
        LOGGER.info("Got game settings {}", event.minecraftSupplier.get().gameSettings)
    }

    private fun enqueueIMC(event: InterModEnqueueEvent) {
        // some example code to dispatch IMC to another mod
        InterModComms.sendTo("forge", "helloworld") {
            LOGGER.info("Hello world")
            "Hello world"
        }
    }

    private fun processIMC(event: InterModProcessEvent) {
        // some example code to receive and process InterModComms from other mods
        LOGGER.info("Got IMC", event.imcStream.map {m -> m.getMessageSupplier<Any>().get()}.collect(Collectors.toList<Any>()))
    }
}
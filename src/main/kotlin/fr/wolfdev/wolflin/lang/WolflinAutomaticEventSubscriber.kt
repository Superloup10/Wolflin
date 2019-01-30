package fr.wolfdev.wolflin.lang

import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.Logging.LOADING
import net.minecraftforge.fml.ModContainer
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation
import net.minecraftforge.forgespi.language.ModFileScanData
import org.apache.logging.log4j.LogManager
import org.objectweb.asm.Type
import java.util.*
import kotlin.reflect.full.companionObjectInstance

object WolflinAutomaticEventSubscriber {
    private val LOGGER = LogManager.getLogger()
    private val AUTO_SUBSCRIBER = Type.getType(Mod.EventBusSubscriber::class.java)
    fun inject(mod: ModContainer, scanData: ModFileScanData, loader: ClassLoader) {
        LOGGER.debug(LOADING, "Attempting to inject @EventBusSubscriber classes into the eventbus for ${mod.modId}")
        val ebsTargets = scanData.annotations.filter {AUTO_SUBSCRIBER == it.annotationType}.toList()
        ebsTargets.forEach {
            @Suppress("UNCHECKED_CAST")
            val sideValues = it.annotationData.getOrDefault("value", listOf(ModAnnotation.EnumHolder(null, "CLIENT"), ModAnnotation.EnumHolder(null, "DEDICATED_SERVER"))) as List<ModAnnotation.EnumHolder>
            val sides = sideValues.map {eh -> Dist.valueOf(eh.value)}.toCollection(EnumSet.noneOf(Dist::class.java))
            val modId = it.annotationData.getOrDefault("modid", mod.modId) as String
            val busTargetHolder = it.annotationData.getOrDefault("bus", ModAnnotation.EnumHolder(null, "FORGE")) as ModAnnotation.EnumHolder
            val busTarget = Mod.EventBusSubscriber.Bus.valueOf(busTargetHolder.value)
            if(Objects.equals(mod.modId, modId) && sides.contains(FMLEnvironment.dist)) {
                try {
                    LOGGER.debug(LOADING, "Auto-subscribing ${it.classType.className} to $busTarget")
                    val subscriberClass = Class.forName(it.classType.className, true, loader)
                    val kotlinClass = subscriberClass.kotlin
                    val objectInstance = kotlinClass.objectInstance ?: kotlinClass.companionObjectInstance
                    busTarget.bus().get().register(objectInstance)
                } catch(e: ClassNotFoundException) {
                    LOGGER.fatal(LOADING, "Failed to load mod class ${it.classType.className} for @EventBusSubscriber annotation", e)
                    throw RuntimeException()
                }
            }
        }
    }
}
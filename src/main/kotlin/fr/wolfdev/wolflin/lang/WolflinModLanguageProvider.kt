package fr.wolfdev.wolflin.lang

import net.minecraftforge.fml.Logging.LOADING
import net.minecraftforge.fml.Logging.SCAN
import net.minecraftforge.forgespi.language.ILifecycleEvent
import net.minecraftforge.forgespi.language.IModInfo
import net.minecraftforge.forgespi.language.IModLanguageProvider
import net.minecraftforge.forgespi.language.ModFileScanData
import org.apache.logging.log4j.LogManager
import org.objectweb.asm.Type
import java.lang.reflect.InvocationTargetException
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collectors

class WolflinModLanguageProvider: IModLanguageProvider {
    companion object {
        private val LOGGER = LogManager.getLogger()
        val MOD_ANNOTATION = Type.getType(Mod::class.java)
    }

    override fun name() = "kotlinfml"
    override fun getFileVisitor(): Consumer<ModFileScanData> {
        return Consumer {scanResult ->
            val modTargetMap = scanResult.annotations.stream()
                    .filter {ad -> ad.annotationType == MOD_ANNOTATION}
                    .peek {ad -> LOGGER.debug(SCAN, "Found @Mod class ${ad.classType.className} with id ${ad.annotationData["value"]}")}
                    .map {ad -> WolflinModTarget(ad.classType.className, ad.annotationData["value"] as String)}
                    .collect(Collectors.toMap<WolflinModTarget, String, WolflinModTarget>(Function<WolflinModTarget, String> {it.modid}, Function.identity<WolflinModTarget>()))
            scanResult.addLanguageLoader(modTargetMap)
        }
    }

    override fun <R: ILifecycleEvent<R>?> consumeLifecycleEvent(consumeEvent: Supplier<R>?) {
    }

    private class WolflinModTarget(val className: String, val modid: String): IModLanguageProvider.IModLanguageLoader {
        val LOGGER = WolflinModLanguageProvider.LOGGER
        @Suppress("UNCHECKED_CAST")
        override fun <T: Any?> loadMod(info: IModInfo?, modClassLoader: ClassLoader?, modFileScanResults: ModFileScanData?): T {
            try {
                val wolflinModContainer = Class.forName("fr.wolfdev.wolflin.lang.WolflinModContainer", true, Thread.currentThread().contextClassLoader)
                LOGGER.debug(LOADING, "Loading WolflinModContainer from classloader ${Thread.currentThread().contextClassLoader} - got ${wolflinModContainer.classLoader}")
                val constructor = wolflinModContainer.getConstructor(IModInfo::class.java, String::class.java, ClassLoader::class.java, ModFileScanData::class.java)
                return constructor.newInstance(info, className, modClassLoader, modFileScanResults) as T
            } catch(e: NoSuchMethodException) {
                LOGGER.fatal(LOADING, "Unable to load WolflinModContainer, wut?", e)
                throw RuntimeException(e)
            } catch(e: ClassNotFoundException) {
                LOGGER.fatal(LOADING, "Unable to load WolflinModContainer, wut?", e)
                throw RuntimeException(e)
            } catch(e: InstantiationException) {
                LOGGER.fatal(LOADING, "Unable to load WolflinModContainer, wut?", e)
                throw RuntimeException(e)
            } catch(e: IllegalAccessException) {
                LOGGER.fatal(LOADING, "Unable to load WolflinModContainer, wut?", e)
                throw RuntimeException(e)
            } catch(e: InvocationTargetException) {
                LOGGER.fatal(LOADING, "Unable to load WolflinModContainer, wut?", e)
                throw RuntimeException(e)
            }
        }
    }
}
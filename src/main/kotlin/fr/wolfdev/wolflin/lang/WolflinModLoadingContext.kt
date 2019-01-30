package fr.wolfdev.wolflin.lang

import net.minecraftforge.fml.ExtensionPoint

class WolflinModLoadingContext {
    var activeContainer: WolflinModContainer? = null
    fun getModEventBus() = activeContainer!!.getEventBus()
    fun <T> registerExtensionPoint(point: ExtensionPoint<T>, extension: () -> T) {
        activeContainer!!.registerExtensionPoint(point, extension)
    }

    companion object {
        private val context = ThreadLocal.withInitial {WolflinModLoadingContext()}
        fun get(): WolflinModLoadingContext {
            return context.get()
        }
    }
}
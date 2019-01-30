package fr.wolfdev.wolflin.example.common.block

import fr.wolfdev.wolflin.example.common.WolflinTest
import fr.wolfdev.wolflin.lang.Mod
import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemGroup
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

@Mod.EventBusSubscriber(modid = WolflinTest.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object WolflinBlocks {
    private val BLOCKS = linkedMapOf<Block, ItemBlock>()
    val ITEM_BUILDER = Item.Builder().group(ItemGroup.BUILDING_BLOCKS)
    val BLOCK_TEST = BlockTest(Block.Builder.create(Material.ROCK).hardnessAndResistance(1.5F, 6.0F).sound(SoundType.STONE)).setRegistryName("block_test")

    init {
        registerBlock(BLOCK_TEST)
    }

    @SubscribeEvent
    fun registerBlock(event: RegistryEvent.Register<Block>) {
        BLOCKS.keys.forEach(event.registry::register)
    }

    @SubscribeEvent
    fun registerItem(event: RegistryEvent.Register<Item>) {
        BLOCKS.values.forEach(event.registry::register)
    }

    private fun registerBlock(block: Block, itemBlock: ItemBlock = ItemBlock(block, ITEM_BUILDER)) {
        itemBlock.registryName = block.registryName
        BLOCKS[block] = itemBlock
    }
}

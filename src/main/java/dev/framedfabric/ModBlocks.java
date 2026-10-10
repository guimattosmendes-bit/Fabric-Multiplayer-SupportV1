package dev.framedfabric;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import dev.framedfabric.block.FramedBlockEntity;
import dev.framedfabric.block.FramedCubeBlock;
import dev.framedfabric.block.FramedFenceBlock;
import dev.framedfabric.block.FramedPanelBlock;
import dev.framedfabric.block.FramedSlabBlock;
import dev.framedfabric.block.FramedStairsBlock;
import dev.framedfabric.block.FramedTrapdoorBlock;
import dev.framedfabric.block.FramedWallBlock;

public final class ModBlocks {
	public static final List<Item> BLOCK_ITEMS = new ArrayList<>();

	public static final FramedCubeBlock CUBE = block("framed_cube", FramedCubeBlock::new);
	public static final FramedSlabBlock SLAB = block("framed_slab", FramedSlabBlock::new);
	public static final FramedStairsBlock STAIRS = block("framed_stairs",
			props -> new FramedStairsBlock(Blocks.OAK_PLANKS.defaultBlockState(), props));
	public static final FramedPanelBlock PANEL = block("framed_panel", FramedPanelBlock::new);
	public static final FramedFenceBlock FENCE = block("framed_fence", FramedFenceBlock::new);
	public static final FramedWallBlock WALL = block("framed_wall", FramedWallBlock::new);

	public static final FramedTrapdoorBlock TRAPDOOR = block("framed_trapdoor", FramedTrapdoorBlock::new);

	public static final Item HAMMER = item("framed_hammer", props -> new Item(props.stacksTo(1)));

	public static final BlockEntityType<FramedBlockEntity> FRAMED_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			FramedFabric.id("framed"),
			FabricBlockEntityTypeBuilder.create(FramedBlockEntity::new, CUBE, SLAB, STAIRS, PANEL, FENCE, WALL, TRAPDOOR).build());

	private ModBlocks() {
	}

	private static <T extends Block> T block(String name, Function<BlockBehaviour.Properties, T> factory) {
		Identifier id = FramedFabric.id(name);
		// noOcclusion: a "moldura" nao deve ocultar faces vizinhas, pois o formato real vem do VoxelShape.
		BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)
				.noOcclusion()
				.setId(ResourceKey.create(Registries.BLOCK, id));
		T block = Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(props));

		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
		BlockItem item = Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey)));
		BLOCK_ITEMS.add(item);
		return block;
	}

	private static <T extends Item> T item(String name, Function<Item.Properties, T> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, FramedFabric.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	public static void init() {
		// Forca o carregamento da classe (e portanto o registro).
	}
}

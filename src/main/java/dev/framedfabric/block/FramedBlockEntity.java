package dev.framedfabric.block;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.fabricmc.fabric.api.blockgetter.v2.RenderDataBlockEntity;

import dev.framedfabric.ModBlocks;

/** Guarda qual bloco esta sendo "imitado" (camuflagem). Baseado no FrameBlockEntity do teste do Fabric API. */
public class FramedBlockEntity extends BlockEntity implements RenderDataBlockEntity {
	private static final Codec<Block> BLOCK_CODEC = BuiltInRegistries.BLOCK.byNameCodec();

	@Nullable
	private Block camo = null;

	public FramedBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.FRAMED_BE, pos, state);
	}

	@Override
	public void loadAdditional(ValueInput data) {
		super.loadAdditional(data);
		camo = data.read("camo", BLOCK_CODEC).orElse(null);

		if (camo == Blocks.AIR) {
			camo = null;
		}

		if (this.getLevel() != null && this.getLevel().isClientSide()) {
			// Forca o remesh do chunk no cliente.
			level.sendBlockUpdated(worldPosition, null, null, 0);
		}
	}

	@Override
	public void saveAdditional(ValueOutput data) {
		super.saveAdditional(data);
		// Sempre grava algo; senao o pacote de sincronizacao nunca e aplicado.
		data.store("camo", BLOCK_CODEC, camo != null ? camo : Blocks.AIR);
	}

	@Override
	public void setChanged() {
		super.setChanged();

		if (this.hasLevel() && !this.getLevel().isClientSide()) {
			((ServerLevel) level).getChunkSource().blockChanged(getBlockPos());
		}
	}

	@Nullable
	public Block getCamo() {
		return camo;
	}

	public void setCamo(@Nullable Block block) {
		this.camo = block == Blocks.AIR ? null : block;
		this.setChanged();
	}

	@Nullable
	@Override
	public Block getRenderData() {
		return camo;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveCustomOnly(registries);
	}
}

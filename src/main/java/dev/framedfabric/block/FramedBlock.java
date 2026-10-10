package dev.framedfabric.block;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Marcador + criacao do BlockEntity compartilhada por todos os blocos-moldura. */
public interface FramedBlock extends EntityBlock {
	@Nullable
	@Override
	default BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FramedBlockEntity(pos, state);
	}
}

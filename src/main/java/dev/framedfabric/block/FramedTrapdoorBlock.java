package dev.framedfabric.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

/** Alcapao camuflavel. Abre/fecha normalmente com a mao vazia; com um bloco na mao, aplica a camuflagem. */
public class FramedTrapdoorBlock extends TrapDoorBlock implements FramedBlock {
	public FramedTrapdoorBlock(Properties props) {
		super(BlockSetType.OAK, props);
	}

	@Override
	public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		return FramedHelper.use(stack, level, pos, player);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		FramedHelper.dropCamo(level, pos, player);
		return super.playerWillDestroy(level, pos, state, player);
	}
}

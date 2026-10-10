package dev.framedfabric.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;

import dev.framedfabric.ModBlocks;

/** Logica de interacao compartilhada (aplicar/remover camuflagem). */
public final class FramedHelper {
	private FramedHelper() {
	}

	/**
	 * Qualquer bloco sem BlockEntity serve de camuflagem. Blocos cubicos solidos usam o modelo completo;
	 * os demais (alcapao, vidro, tocha...) usam a textura principal do bloco aplicada na moldura.
	 */
	public static boolean isValidCamo(Block block) {
		return block != Blocks.AIR
				&& !(block instanceof EntityBlock)
				&& !(block instanceof FramedBlock);
	}

	public static InteractionResult use(ItemStack stack, Level level, BlockPos pos, Player player) {
		if (!(level.getBlockEntity(pos) instanceof FramedBlockEntity be)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		Block current = be.getCamo();

		// Martelo: remove a camuflagem e devolve o bloco.
		if (stack.is(ModBlocks.HAMMER)) {
			if (current == null) {
				return InteractionResult.TRY_WITH_EMPTY_HAND;
			}
			if (!level.isClientSide()) {
				if (!player.isCreative()) {
					player.getInventory().placeItemBackInInventory(new ItemStack(current), Prediction.SERVER_ONLY);
				}
				be.setCamo(null);
			}
			return InteractionResult.SUCCESS;
		}

		// Bloco na mao: aplica como camuflagem.
		Block handBlock = Block.byItem(stack.getItem());
		if (!isValidCamo(handBlock) || handBlock == current) {
			// Deixa a interacao normal do bloco acontecer (ex.: abrir/fechar o alcapao).
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}

		if (!level.isClientSide()) {
			stack.consume(1, player);
			if (current != null && !player.isCreative()) {
				player.getInventory().placeItemBackInInventory(new ItemStack(current), Prediction.SERVER_ONLY);
			}
			be.setCamo(handBlock);
		}
		return InteractionResult.SUCCESS;
	}

	/** Solta o bloco de camuflagem quando a moldura e quebrada. */
	public static void dropCamo(Level level, BlockPos pos, Player player) {
		if (level.isClientSide() || player.isCreative()) {
			return;
		}
		if (level.getBlockEntity(pos) instanceof FramedBlockEntity be && be.getCamo() != null) {
			Block.popResource(level, pos, new ItemStack(be.getCamo()));
		}
	}
}

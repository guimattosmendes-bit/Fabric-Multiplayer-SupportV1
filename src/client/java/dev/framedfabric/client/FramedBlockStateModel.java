package dev.framedfabric.client;

import java.util.List;
import java.util.function.Predicate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.blockgetter.v2.FabricBlockGetter;
import net.fabricmc.fabric.api.client.model.loading.v1.CustomUnbakedBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadTransform;

/**
 * Modelo das molduras.
 *
 * <p>Ideia: o formato vem do VoxelShape do proprio estado do bloco (lista de caixas). Para cada caixa,
 * emitimos o modelo do bloco de camuflagem (um cubo completo) e, por meio de um {@link QuadTransform},
 * "recortamos" cada face do cubo para a face correspondente da caixa, reinterpolando os UVs. Assim a
 * textura fica alinhada ao mundo, como num bloco vanilla (slab mostra a metade de baixo da textura etc.).
 * Sem camuflagem, o mesmo processo e aplicado ao modelo-cubo da propria moldura.
 *
 * <p>Estrutura baseada no FrameBlockStateModel do testmod do Fabric API (Apache-2.0).
 */
public class FramedBlockStateModel implements BlockStateModel {
	private final BlockStateModel frameModel;

	public FramedBlockStateModel(BlockStateModel frameModel) {
		this.frameModel = frameModel;
	}

	@Nullable
	private static BlockState camoState(BlockAndTintGetter level, BlockPos pos) {
		if (((FabricBlockGetter) level).getBlockEntityRenderData(pos) instanceof Block block) {
			return block.defaultBlockState();
		}
		return null;
	}

	private static BlockStateModel modelOf(BlockState state) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		BlockState camo = camoState(level, pos);
		// Camuflagem "plana": bloco nao cubico (alcapao, vidro...). Usa so a textura principal dele.
		boolean flat = camo != null && !camo.isSolidRender();
		BlockStateModel source = (camo != null && !flat) ? modelOf(camo) : frameModel;
		BlockState sourceState = (camo != null && !flat) ? camo : state;
		Material.Baked override = flat ? modelOf(camo).particleMaterial(level, pos, camo) : null;

		for (AABB box : state.getShape(level, pos).toAabbs()) {
			emitter.pushTransform(new BoxTransform(box, cullTest, override));
			// Emite sempre as 6 faces do cubo; o BoxTransform decide o que descartar.
			source.emitQuads(emitter, level, pos, sourceState, random, face -> false);
			emitter.popTransform();
		}
	}

	/** Reposiciona uma face de cubo unitario sobre a face equivalente de uma caixa. */
	private record BoxTransform(AABB box, Predicate<@Nullable Direction> cullTest, @Nullable Material.Baked override) implements QuadTransform {
		private static final float EPS = 1e-4f;

		@Override
		public boolean transform(MutableQuadView quad) {
			Direction face = quad.nominalFace();
			if (face == null) {
				return false;
			}

			final float[] lo = {(float) box.minX, (float) box.minY, (float) box.minZ};
			final float[] hi = {(float) box.maxX, (float) box.maxY, (float) box.maxZ};

			final int n = switch (face.getAxis()) {
				case X -> 0;
				case Y -> 1;
				default -> 2;
			};
			final int a = (n + 1) % 3;
			final int b = (n + 2) % 3;
			final boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;

			// Le o quad original: UV de cada "canto" (indexado por [ca][cb]) e o canto de cada vertice.
			final float[][][] cornerUv = new float[2][2][2];
			final int[] ca = new int[4];
			final int[] cb = new int[4];
			for (int v = 0; v < 4; v++) {
				final float[] p = {quad.x(v), quad.y(v), quad.z(v)};
				ca[v] = p[a] > 0.5f ? 1 : 0;
				cb[v] = p[b] > 0.5f ? 1 : 0;
				cornerUv[ca[v]][cb[v]][0] = quad.u(v);
				cornerUv[ca[v]][cb[v]][1] = quad.v(v);
			}

			// Escreve a nova geometria.
			final float planeCoord = positive ? hi[n] : lo[n];
			for (int v = 0; v < 4; v++) {
				final float s = ca[v] == 1 ? hi[a] : lo[a];
				final float t = cb[v] == 1 ? hi[b] : lo[b];

				final float[] pos = new float[3];
				pos[n] = planeCoord;
				pos[a] = s;
				pos[b] = t;
				quad.pos(v, pos[0], pos[1], pos[2]);

				// Interpolacao bilinear dos UVs originais na posicao (s, t) dentro da face.
				final float u = bilerp(cornerUv, 0, s, t);
				final float vv = bilerp(cornerUv, 1, s, t);
				quad.uv(v, u, vv);
			}

			// Culling: so faz sentido se a face da caixa encosta na borda do bloco.
			final boolean onBoundary = positive ? planeCoord >= 1f - EPS : planeCoord <= EPS;
			if (onBoundary) {
				if (cullTest.test(face)) {
					return false;
				}
				quad.cullFace(face);
			} else {
				quad.cullFace(null);
			}

			if (override != null) {
				// UVs alinhados ao mundo a partir da posicao; camada translucida para respeitar transparencia.
				quad.materialBake(override, MutableQuadView.BAKE_LOCK_UV);
				quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
			}

			return true;
		}

		private static float bilerp(float[][][] c, int k, float s, float t) {
			final float lowT = c[0][0][k] * (1 - s) + c[1][0][k] * s;
			final float highT = c[0][1][k] * (1 - s) + c[1][1][k] * s;
			return lowT * (1 - t) + highT * t;
		}
	}

	private record GeometryKey(@Nullable Object camoKey, BlockState state) {
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		BlockState camo = camoState(level, pos);
		if (camo == null) {
			return new GeometryKey(null, state);
		}

		if (!camo.isSolidRender()) {
			return new GeometryKey(camo, state);
		}

		Object sub = modelOf(camo).createGeometryKey(level, pos, camo, random);
		return sub == null ? null : new GeometryKey(sub, state);
	}

	@Override
	public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		BlockState camo = camoState(level, pos);
		return camo != null ? modelOf(camo).particleMaterial(level, pos, camo) : frameModel.particleMaterial(level, pos, state);
	}

	@Override
	@BakedQuad.MaterialFlags
	public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		@BakedQuad.MaterialFlags int flags = frameModel.materialFlags(level, pos, state, random);
		BlockState camo = camoState(level, pos);
		if (camo != null) {
			flags |= modelOf(camo).materialFlags(level, pos, camo, random);
			if (!camo.isSolidRender()) {
				flags |= BakedQuad.FLAG_TRANSLUCENT;
			}
		}
		return flags;
	}

	@Override
	public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
		// A Renderer API torna isto obsoleto: nao adicionamos partes.
	}

	@Override
	public Material.Baked particleMaterial() {
		return frameModel.particleMaterial();
	}

	@Override
	@BakedQuad.MaterialFlags
	public int materialFlags() {
		// Pode renderizar qualquer submodelo (possivelmente animado/translucido).
		return BakedQuad.FLAG_ANIMATED | BakedQuad.FLAG_TRANSLUCENT;
	}

	public record Unbaked(BlockStateModel.Unbaked frameModel) implements CustomUnbakedBlockStateModel {
		public static final MapCodec<Unbaked> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				BlockStateModel.Unbaked.CODEC.fieldOf("frame_model").forGetter(Unbaked::frameModel)
		).apply(instance, Unbaked::new));

		@Override
		public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
			return CODEC;
		}

		@Override
		public void resolveDependencies(Resolver resolver) {
			frameModel.resolveDependencies(resolver);
		}

		@Override
		public BlockStateModel bake(ModelBaker baker) {
			return new FramedBlockStateModel(frameModel.bake(baker));
		}
	}
}

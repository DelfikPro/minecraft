package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.World;
import optifine.BlockPosM;
import optifine.Config;
import shadersmod.client.SVertexBuilder;

import java.nio.FloatBuffer;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class RenderChunk {

	public static int renderChunksUpdated;
	private static EnumWorldBlockLayer[] ENUM_WORLD_BLOCK_LAYERS = EnumWorldBlockLayer.values();
	private final RenderGlobal renderGlobal;
	private final ReentrantLock lockCompileTask = new ReentrantLock();
	private final ReentrantLock lockCompiledChunk = new ReentrantLock();
	private final Set field_181056_j = Sets.newHashSet();
	private final int index;
	private final FloatBuffer modelviewMatrix = GLAllocation.createDirectFloatBuffer(16);
	private final VertexBuffer[] vertexBuffers = new VertexBuffer[EnumWorldBlockLayer.values().length];
	public CompiledChunk compiledChunk = CompiledChunk.DUMMY;
	public AxisAlignedBB boundingBox;
	private World world;
	private BlockPos position;
	private ChunkCompileTaskGenerator compileTask = null;
	private int frameIndex = -1;
	private boolean needsUpdate = true;
	private EnumMap field_181702_p;
	private BlockPos[] positionOffsets16 = new BlockPos[EnumFacing.VALUES.length];
	private EnumWorldBlockLayer[] blockLayersSingle = new EnumWorldBlockLayer[1];
	private boolean isMipmaps = Config.isMipmaps();
	private boolean playerUpdate = false;

	public RenderChunk(World worldIn, RenderGlobal renderGlobalIn, BlockPos blockPosIn, int indexIn) {
		this.world = worldIn;
		this.renderGlobal = renderGlobalIn;
		this.index = indexIn;

		if (!blockPosIn.equals(this.getPosition())) {
			this.setPosition(blockPosIn);
		}

		if (OpenGlHelper.useVbo()) {
			for (int i = 0; i < EnumWorldBlockLayer.values().length; ++i) {
				this.vertexBuffers[i] = new VertexBuffer(DefaultVertexFormats.BLOCK);
			}
		}
	}

	public boolean setFrameIndex(int frameIndexIn) {
		if (this.frameIndex == frameIndexIn) {
			return false;
		}
		this.frameIndex = frameIndexIn;
		return true;
	}

	public VertexBuffer getVertexBufferByLayer(int layer) {
		return this.vertexBuffers[layer];
	}

	public void resortTransparency(float x, float y, float z, ChunkCompileTaskGenerator generator) {
		CompiledChunk compiledchunk = generator.getCompiledChunk();

		if (compiledchunk.getState() != null && !compiledchunk.isLayerEmpty(EnumWorldBlockLayer.TRANSLUCENT)) {
			WorldRenderer worldrenderer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(EnumWorldBlockLayer.TRANSLUCENT);
			this.preRenderBlocks(worldrenderer, this.position);
			worldrenderer.setVertexState(compiledchunk.getState());
			this.postRenderBlocks(EnumWorldBlockLayer.TRANSLUCENT, x, y, z, worldrenderer, compiledchunk);
		}
	}

	public void rebuildChunk(float x, float y, float z, ChunkCompileTaskGenerator generator) {
		CompiledChunk compiledchunk = new CompiledChunk();
		BlockPos blockpos = this.position;
		BlockPos blockpos1 = blockpos.add(15, 15, 15);
		generator.getLock().lock();
		RegionRenderCache regionrendercache;

		try {
			if (generator.getStatus() != ChunkCompileTaskGenerator.Status.COMPILING)
				return;

			if (this.world == null)
				return;

			regionrendercache = this.createRegionRenderCache(this.world, blockpos.add(-1, -1, -1), blockpos1.add(1, 1, 1), 1);
			generator.setCompiledChunk(compiledchunk);
		} finally {
			generator.getLock().unlock();
		}

		VisGraph var10 = new VisGraph();
		HashSet var11 = Sets.newHashSet();

		if (!regionrendercache.extendedLevelsInChunkCache()) {
			++renderChunksUpdated;
			boolean[] aboolean = new boolean[ENUM_WORLD_BLOCK_LAYERS.length];
			BlockRendererDispatcher blockrendererdispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();

			for (Object o : BlockPosM.getAllInBoxMutable(blockpos, blockpos1)) {
				BlockPosM blockposm = (BlockPosM) o;
				IBlockState iblockstate = regionrendercache.getBlockState(blockposm);
				Block block = iblockstate.getBlock();

				if (block.isOpaqueCube()) {
					var10.func_178606_a(blockposm);
				}

				if (iblockstate.getBlock().hasTileEntity()) {
					TileEntity tileentity = regionrendercache.getTileEntity(new BlockPos(blockposm));
					TileEntitySpecialRenderer tileentityspecialrenderer = TileEntityRendererDispatcher.instance.getSpecialRenderer(tileentity);

					if (tileentity != null && tileentityspecialrenderer != null) {
						compiledchunk.addTileEntity(tileentity);

						if (tileentityspecialrenderer.func_181055_a()) {
							var11.add(tileentity);
						}
					}
				}

				EnumWorldBlockLayer[] aenumworldblocklayer;

				aenumworldblocklayer = this.blockLayersSingle;
				aenumworldblocklayer[0] = block.getBlockLayer();

				for (int i = 0; i < aenumworldblocklayer.length; ++i) {
					EnumWorldBlockLayer enumworldblocklayer = aenumworldblocklayer[i];

					enumworldblocklayer = this.fixBlockLayer(block, enumworldblocklayer);

					int j = enumworldblocklayer.ordinal();

					if (block.getRenderType() == -1) continue;
					WorldRenderer worldrenderer = generator.getRegionRenderCacheBuilder().getWorldRendererByLayerId(j);
					worldrenderer.setBlockLayer(enumworldblocklayer);

					if (!compiledchunk.isLayerStarted(enumworldblocklayer)) {
						compiledchunk.setLayerStarted(enumworldblocklayer);
						this.preRenderBlocks(worldrenderer, blockpos);
					}

					aboolean[j] |= blockrendererdispatcher.renderBlock(iblockstate, blockposm, regionrendercache, worldrenderer);
				}
			}

			for (EnumWorldBlockLayer enumworldblocklayer1 : ENUM_WORLD_BLOCK_LAYERS) {
				if (aboolean[enumworldblocklayer1.ordinal()]) compiledchunk.setLayerUsed(enumworldblocklayer1);

				if (compiledchunk.isLayerStarted(enumworldblocklayer1)) {
					if (Config.isShaders()) {
						SVertexBuilder.calcNormalChunkLayer(generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(enumworldblocklayer1));
					}

					this.postRenderBlocks(enumworldblocklayer1, x, y, z, generator.getRegionRenderCacheBuilder().getWorldRendererByLayer(enumworldblocklayer1), compiledchunk);
				}
			}
		}

		compiledchunk.setVisibility(var10.computeVisibility());
		this.lockCompileTask.lock();

		try {
			HashSet hashset1 = Sets.newHashSet(var11);
			HashSet hashset2 = Sets.newHashSet(this.field_181056_j);
			hashset1.removeAll(this.field_181056_j);
			hashset2.removeAll(var11);
			this.field_181056_j.clear();
			this.field_181056_j.addAll(var11);
			this.renderGlobal.func_181023_a(hashset2, hashset1);
		} finally {
			this.lockCompileTask.unlock();
		}
	}

	protected void finishCompileTask() {
		this.lockCompileTask.lock();

		try {
			if (this.compileTask != null && this.compileTask.getStatus() != ChunkCompileTaskGenerator.Status.DONE) {
				this.compileTask.finish();
				this.compileTask = null;
			}
		} finally {
			this.lockCompileTask.unlock();
		}
	}

	public ReentrantLock getLockCompileTask() {
		return this.lockCompileTask;
	}

	public ChunkCompileTaskGenerator makeCompileTaskChunk() {
		this.lockCompileTask.lock();
		ChunkCompileTaskGenerator chunkcompiletaskgenerator;

		try {
			this.finishCompileTask();
			this.compileTask = new ChunkCompileTaskGenerator(this, ChunkCompileTaskGenerator.Type.REBUILD_CHUNK);
			chunkcompiletaskgenerator = this.compileTask;
		} finally {
			this.lockCompileTask.unlock();
		}

		return chunkcompiletaskgenerator;
	}

	public ChunkCompileTaskGenerator makeCompileTaskTransparency() {
		this.lockCompileTask.lock();
		ChunkCompileTaskGenerator chunkcompiletaskgenerator1;

		try {
			if (this.compileTask != null && this.compileTask.getStatus() == ChunkCompileTaskGenerator.Status.PENDING) return null;

			if (this.compileTask != null && this.compileTask.getStatus() != ChunkCompileTaskGenerator.Status.DONE) {
				this.compileTask.finish();
				this.compileTask = null;
			}

			this.compileTask = new ChunkCompileTaskGenerator(this, ChunkCompileTaskGenerator.Type.RESORT_TRANSPARENCY);
			this.compileTask.setCompiledChunk(this.compiledChunk);
			chunkcompiletaskgenerator1 = this.compileTask;
		} finally {
			this.lockCompileTask.unlock();
		}

		return chunkcompiletaskgenerator1;
	}

	private void preRenderBlocks(WorldRenderer worldRendererIn, BlockPos pos) {
		worldRendererIn.begin(7, DefaultVertexFormats.BLOCK);
		worldRendererIn.setTranslation((double) -pos.getX(), (double) -pos.getY(), (double) -pos.getZ());
	}

	private void postRenderBlocks(EnumWorldBlockLayer layer, float x, float y, float z, WorldRenderer worldRendererIn, CompiledChunk compiledChunkIn) {
		if (layer == EnumWorldBlockLayer.TRANSLUCENT && !compiledChunkIn.isLayerEmpty(layer)) {
			worldRendererIn.func_181674_a(x, y, z);
			compiledChunkIn.setState(worldRendererIn.func_181672_a());
		}

		worldRendererIn.finishDrawing();
	}

	private void initModelviewMatrix() {
		G.pushMatrix();
		G.loadIdentity();
		float f = 1.000001f;
		G.translate(-8.0F, -8.0F, -8.0F);
		G.scale(f, f, f);
		G.translate(8.0F, 8.0F, 8.0F);
		G.getFloat(2982, this.modelviewMatrix);
		G.popMatrix();
	}

	public void multModelviewMatrix() {
		G.multMatrix(this.modelviewMatrix);
	}

	public CompiledChunk getCompiledChunk() {
		return this.compiledChunk;
	}

	public void setCompiledChunk(CompiledChunk compiledChunkIn) {
		this.lockCompiledChunk.lock();

		try {
			this.compiledChunk = compiledChunkIn;
		} finally {
			this.lockCompiledChunk.unlock();
		}
	}

	public void stopCompileTask() {
		this.finishCompileTask();
		this.compiledChunk = CompiledChunk.DUMMY;
	}

	public void deleteGlResources() {
		this.stopCompileTask();
		this.world = null;

		for (int i = 0; i < EnumWorldBlockLayer.values().length; ++i) {
			if (this.vertexBuffers[i] != null) {
				this.vertexBuffers[i].deleteGlBuffers();
			}
		}
	}

	public BlockPos getPosition() {
		return this.position;
	}

	public void setPosition(BlockPos pos) {
		this.stopCompileTask();
		this.position = pos;
		this.boundingBox = new AxisAlignedBB(pos, pos.add(16, 16, 16));
		this.initModelviewMatrix();

		for (int i = 0; i < this.positionOffsets16.length; ++i) {
			this.positionOffsets16[i] = null;
		}
	}

	public boolean isNeedsUpdate() {
		return this.needsUpdate;
	}

	public void setNeedsUpdate(boolean needsUpdateIn) {
		this.needsUpdate = needsUpdateIn;

		if (this.needsUpdate) {
			if (this.isWorldPlayerUpdate()) {
				this.playerUpdate = true;
			}
		} else {
			this.playerUpdate = false;
		}
	}

	public BlockPos func_181701_a(EnumFacing p_181701_1_) {
		return this.getPositionOffset16(p_181701_1_);
	}

	public BlockPos getPositionOffset16(EnumFacing p_getPositionOffset16_1_) {
		int i = p_getPositionOffset16_1_.getIndex();
		BlockPos blockpos = this.positionOffsets16[i];

		if (blockpos == null) {
			blockpos = this.getPosition().offset(p_getPositionOffset16_1_, 16);
			this.positionOffsets16[i] = blockpos;
		}

		return blockpos;
	}

	private boolean isWorldPlayerUpdate() {
		if (this.world instanceof WorldClient) {
			WorldClient worldclient = (WorldClient) this.world;
			return worldclient.isPlayerUpdate();
		}
		return false;
	}

	public boolean isPlayerUpdate() {
		return this.playerUpdate;
	}

	protected RegionRenderCache createRegionRenderCache(World p_createRegionRenderCache_1_, BlockPos p_createRegionRenderCache_2_, BlockPos p_createRegionRenderCache_3_,
														int p_createRegionRenderCache_4_) {
		return new RegionRenderCache(p_createRegionRenderCache_1_, p_createRegionRenderCache_2_, p_createRegionRenderCache_3_, p_createRegionRenderCache_4_);
	}

	private EnumWorldBlockLayer fixBlockLayer(Block p_fixBlockLayer_1_, EnumWorldBlockLayer p_fixBlockLayer_2_) {
		if (this.isMipmaps) {
			if (p_fixBlockLayer_2_ == EnumWorldBlockLayer.CUTOUT) {
				if (p_fixBlockLayer_1_ instanceof BlockRedstoneWire) {
					return p_fixBlockLayer_2_;
				}

				if (p_fixBlockLayer_1_ instanceof BlockCactus) {
					return p_fixBlockLayer_2_;
				}

				return EnumWorldBlockLayer.CUTOUT_MIPPED;
			}
		} else if (p_fixBlockLayer_2_ == EnumWorldBlockLayer.CUTOUT_MIPPED) {
			return EnumWorldBlockLayer.CUTOUT;
		}

		return p_fixBlockLayer_2_;
	}

}

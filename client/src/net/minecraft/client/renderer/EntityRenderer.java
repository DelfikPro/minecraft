package net.minecraft.client.renderer;

import com.google.common.base.Predicates;
import com.google.gson.JsonSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.game.entity.AbstractClientPlayer;
import net.minecraft.client.game.particle.EffectRenderer;
import net.minecraft.client.game.shader.ShaderGroup;
import net.minecraft.client.game.shader.ShaderLinkHelper;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.MapItemRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.Lang;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.settings.Settings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.Player;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.potion.Potion;
import net.minecraft.logging.Log;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.*;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.biome.Biome;
import optifine.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.Project;
import shadersmod.client.Shaders;
import shadersmod.client.ShadersRender;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class EntityRenderer implements IResourceManagerReloadListener {
	private static final Log logger = Log.MAIN;
	private static final ResourceLocation locationRainPng = new ResourceLocation("textures/environment/rain.png");
	private static final ResourceLocation locationSnowPng = new ResourceLocation("textures/environment/snow.png");

	/**
	 * A reference to the Minecraft object.
	 */
	private Minecraft mc;
	private final IResourceManager resourceManager;
	private Random random = new Random();
	private float farPlaneDistance;
	public ItemRenderer itemRenderer;
	private final MapItemRenderer theMapItemRenderer;

	/**
	 * Entity renderer update count
	 */
	private int rendererUpdateCount;

	/**
	 * Pointed entity
	 */
	private Entity pointedEntity;
	private MouseFilter mouseFilterXAxis = new MouseFilter();
	private MouseFilter mouseFilterYAxis = new MouseFilter();
	private float thirdPersonDistance = 4.0F;

	/**
	 * Third person distance temp
	 */
	private float thirdPersonDistanceTemp = 4.0F;

	/**
	 * Smooth cam yaw
	 */
	private float smoothCamYaw;

	/**
	 * Smooth cam pitch
	 */
	private float smoothCamPitch;

	/**
	 * Smooth cam filter X
	 */
	private float smoothCamFilterX;

	/**
	 * Smooth cam filter Y
	 */
	private float smoothCamFilterY;

	/**
	 * Smooth cam partial ticks
	 */
	private float smoothCamPartialTicks;

	/**
	 * FOV modifier hand
	 */
	private float fovModifierHand;

	/**
	 * FOV modifier hand prev
	 */
	private float fovModifierHandPrev;
	private float bossColorModifier;
	private float bossColorModifierPrev;

	/**
	 * Cloud fog mode
	 */
	private boolean cloudFog;
	private boolean renderHand = true;
	private boolean drawBlockOutline = true;

	/**
	 * Previous frame time in milliseconds
	 */
	private long prevFrameTime = Minecraft.getSystemTime();

	/**
	 * End time of last render (ns)
	 */
	private long renderEndNanoTime;

	/**
	 * The texture id of the blocklight/skylight texture used for lighting effects
	 */
	private final DynamicTexture lightmapTexture;

	/**
	 * Colors computed in updateLightmap() and loaded into the lightmap emptyTexture
	 */
	private final int[] lightmapColors;
	private final ResourceLocation locationLightMap;

	/**
	 * Is set, updateCameraAndRender() calls updateLightmap(); set by updateTorchFlicker()
	 */
	private boolean lightmapUpdateNeeded;

	/**
	 * Torch flicker X
	 */
	private float torchFlickerX;
	private float torchFlickerDX;

	/**
	 * Rain sound counter
	 */
	private int rainSoundCounter;
	private float[] rainXCoords = new float[1024];
	private float[] rainYCoords = new float[1024];

	/**
	 * Fog color buffer
	 */
	private FloatBuffer fogColorBuffer = GLAllocation.createDirectFloatBuffer(16);
	public float fogColorRed;
	public float fogColorGreen;
	public float fogColorBlue;

	/**
	 * Fog color 2
	 */
	private float fogColor2;

	/**
	 * Fog color 1
	 */
	private float fogColor1;
	private int debugViewDirection = 0;
	private boolean debugView = false;
	private double cameraZoom = 1.0D;
	private double cameraYaw;
	private double cameraPitch;
	private ShaderGroup theShaderGroup;
	private static final ResourceLocation[] shaderResourceLocations = new ResourceLocation[] {
			new ResourceLocation("shaders/post/notch.json"), new ResourceLocation("shaders/post/fxaa.json"),
			new ResourceLocation("shaders/post/art.json"), new ResourceLocation("shaders/post/bumpy.json"),
			new ResourceLocation("shaders/post/blobs2.json"), new ResourceLocation("shaders/post/pencil.json"),
			new ResourceLocation("shaders/post/color_convolve.json"),
			new ResourceLocation("shaders/post/deconverge.json"), new ResourceLocation("shaders/post/flip.json"),
			new ResourceLocation("shaders/post/invert.json"), new ResourceLocation("shaders/post/ntsc.json"),
			new ResourceLocation("shaders/post/outline.json"), new ResourceLocation("shaders/post/phosphor.json"),
			new ResourceLocation("shaders/post/scan_pincushion.json"), new ResourceLocation("shaders/post/sobel.json"),
			new ResourceLocation("shaders/post/bits.json"), new ResourceLocation("shaders/post/desaturate.json"),
			new ResourceLocation("shaders/post/green.json"), new ResourceLocation("shaders/post/blur.json"),
			new ResourceLocation("shaders/post/wobble.json"), new ResourceLocation("shaders/post/blobs.json"),
			new ResourceLocation("shaders/post/antialias.json"), new ResourceLocation("shaders/post/creeper.json"),
			new ResourceLocation("shaders/post/spider.json")
	};
	public static final int shaderCount = shaderResourceLocations.length;
	private int shaderIndex;
	private boolean useShader;
	public int frameCount;

	private boolean initialized = false;
	private World updatedWorld = null;
	private boolean showDebugInfo = false;
	public boolean fogStandard = false;
	private float clipDistance = 128.0F;
	private long lastServerTime = 0L;
	private int lastServerTicks = 0;
	private int serverWaitTime = 0;
	private int serverWaitTimeCurrent = 0;
	private float avgServerTimeDiff = 0.0F;
	private float avgServerTickDiff = 0.0F;
	private long lastErrorCheckTimeMs = 0L;
	private ShaderGroup[] fxaaShaders = new ShaderGroup[10];

	public EntityRenderer(Minecraft mcIn, IResourceManager resourceManagerIn) {
		this.shaderIndex = shaderCount;
		this.useShader = false;
		this.frameCount = 0;
		this.mc = mcIn;
		this.resourceManager = resourceManagerIn;
		this.itemRenderer = mcIn.getItemRenderer();
		this.theMapItemRenderer = new MapItemRenderer(mcIn.getTextureManager());
		this.lightmapTexture = new DynamicTexture(16, 16);
		this.locationLightMap = mcIn.getTextureManager().getDynamicTextureLocation("lightMap", this.lightmapTexture);
		this.lightmapColors = this.lightmapTexture.getTextureData();
		this.theShaderGroup = null;

		for (int i = 0; i < 32; ++i) {
			for (int j = 0; j < 32; ++j) {
				float f = (float) (j - 16);
				float f1 = (float) (i - 16);
				float f2 = MathHelper.sqrt_float(f * f + f1 * f1);
				this.rainXCoords[i << 5 | j] = -f1 / f2;
				this.rainYCoords[i << 5 | j] = f / f2;
			}
		}
	}

	public boolean isShaderActive() {
		return OpenGlHelper.shadersSupported && this.theShaderGroup != null;
	}

	public void removeShaderGroup() {
		if (this.theShaderGroup != null) {
			this.theShaderGroup.deleteShaderGroup();
		}

		this.theShaderGroup = null;
		this.shaderIndex = shaderCount;
	}

	public void switchUseShader() {
		this.useShader = !this.useShader;
	}

	/**
	 * What shader to use when spectating this entity
	 */
	public void loadEntityShader(Entity entityIn) {
		if (OpenGlHelper.shadersSupported) {
			if (this.theShaderGroup != null) {
				this.theShaderGroup.deleteShaderGroup();
			}

			this.theShaderGroup = null;

			//			if (entityIn instanceof EntityCreeper) {
			//				this.loadShader(new ResourceLocation("shaders/post/creeper.json"));
			//			} else if (entityIn instanceof EntitySpider) {
			//				this.loadShader(new ResourceLocation("shaders/post/spider.json"));
			//			} else if (entityIn instanceof EntityEnderman) {
			//				this.loadShader(new ResourceLocation("shaders/post/invert.json"));
			//			}
		}
	}

	public void activateNextShader() {
		if (OpenGlHelper.shadersSupported && this.mc.getRenderViewEntity() instanceof Player) {
			if (this.theShaderGroup != null) {
				this.theShaderGroup.deleteShaderGroup();
			}

			this.shaderIndex = (this.shaderIndex + 1) % (shaderResourceLocations.length + 1);

			if (this.shaderIndex != shaderCount) {
				this.loadShader(shaderResourceLocations[this.shaderIndex]);
			} else {
				this.theShaderGroup = null;
			}
		}
	}

	private void loadShader(ResourceLocation resourceLocationIn) {
		if (OpenGlHelper.isFramebufferEnabled()) {
			try {
				this.theShaderGroup = new ShaderGroup(this.mc.getTextureManager(), this.resourceManager, this.mc.getFramebuffer(), resourceLocationIn);
				this.theShaderGroup.createBindFramebuffers(this.mc.displayWidth, this.mc.displayHeight);
				this.useShader = true;
			} catch (IOException | JsonSyntaxException ioexception) {
				logger.warn("Failed to load shader: " + resourceLocationIn, ioexception);
				this.shaderIndex = shaderCount;
				this.useShader = false;
			}
		}
	}

	public void onResourceManagerReload(IResourceManager resourceManager) {
		if (this.theShaderGroup != null) {
			this.theShaderGroup.deleteShaderGroup();
		}

		this.theShaderGroup = null;

		if (this.shaderIndex != shaderCount) {
			this.loadShader(shaderResourceLocations[this.shaderIndex]);
		} else {
			this.loadEntityShader(this.mc.getRenderViewEntity());
		}
	}

	/**
	 * Updates the entity renderer
	 */
	public void updateRenderer() {
		if (OpenGlHelper.shadersSupported && ShaderLinkHelper.getStaticShaderLinkHelper() == null) {
			ShaderLinkHelper.setNewStaticShaderLinkHelper();
		}

		this.updateFovModifierHand();
		this.updateTorchFlicker();
		this.fogColor2 = this.fogColor1;
		this.thirdPersonDistanceTemp = this.thirdPersonDistance;

		if (Settings.SMOOTH_CAMERA.b()) {
			float f = Settings.SENSITIVITY.f() * 0.6F + 0.2F;
			float f1 = f * f * f * 8.0F;
			this.smoothCamFilterX = this.mouseFilterXAxis.smooth(this.smoothCamYaw, 0.05F * f1);
			this.smoothCamFilterY = this.mouseFilterYAxis.smooth(this.smoothCamPitch, 0.05F * f1);
			this.smoothCamPartialTicks = 0.0F;
			this.smoothCamYaw = 0.0F;
			this.smoothCamPitch = 0.0F;
		} else {
			this.smoothCamFilterX = 0.0F;
			this.smoothCamFilterY = 0.0F;
			this.mouseFilterXAxis.reset();
			this.mouseFilterYAxis.reset();
		}

		if (this.mc.getRenderViewEntity() == null) {
			this.mc.setRenderViewEntity(this.mc.thePlayer);
		}

		Entity entity = this.mc.getRenderViewEntity();
		double d0 = entity.posX;
		double d1 = entity.posY + (double) entity.getEyeHeight();
		double d2 = entity.posZ;
		float f3 = this.mc.theWorld.getLightBrightness(new BlockPos(d0, d1, d2));
		float f4 = Settings.RENDER_DISTANCE.f() / 16.0F;
		f4 = MathHelper.clamp_float(f4, 0.0F, 1.0F);
		float f2 = f3 * (1.0F - f4) + f4;
		this.fogColor1 += (f2 - this.fogColor1) * 0.1F;
		++this.rendererUpdateCount;
		this.itemRenderer.updateEquippedItem();
		this.addRainParticles();
		this.bossColorModifierPrev = this.bossColorModifier;
		// ToDo: Разобраться с тем, что это такое
		//		if (BossStatus.hasColorModifier) {
		//			this.bossColorModifier += 0.05F;
		//
		//			if (this.bossColorModifier > 1.0F) {
		//				this.bossColorModifier = 1.0F;
		//			}
		//
		//			BossStatus.hasColorModifier = false;
		//		} else if (this.bossColorModifier > 0.0F) {
		//			this.bossColorModifier -= 0.0125F;
		//		}
	}

	public ShaderGroup getShaderGroup() {
		return this.theShaderGroup;
	}

	public void updateShaderGroupSize(int width, int height) {
		if (OpenGlHelper.shadersSupported) {
			if (this.theShaderGroup != null) {
				this.theShaderGroup.createBindFramebuffers(width, height);
			}

			this.mc.renderGlobal.createBindEntityOutlineFbs(width, height);
		}
	}

	/**
	 * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
	 */
	public void getMouseOver(float partialTicks) {
		Entity entity = this.mc.getRenderViewEntity();

		if (entity != null && this.mc.theWorld != null) {
			mc.getProfiler().startSection("pick");
			this.mc.pointedEntity = null;
			double d0 = (double) this.mc.playerController.getBlockReachDistance();
			this.mc.objectMouseOver = entity.rayTrace(d0, partialTicks);
			double d1 = d0;
			Vec3d vec3D = entity.getPositionEyes(partialTicks);
			boolean flag = false;
			boolean flag1 = true;

			if (this.mc.playerController.extendedReach()) {
				d0 = 6.0D;
				d1 = 6.0D;
			} else if (d0 > 3.0D) flag = true;

			if (this.mc.objectMouseOver != null) {
				d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3D);
			}

			Vec3d vec31D = entity.getLook(partialTicks);
			Vec3d vec32D = vec3D.addVector(vec31D.xCoord * d0, vec31D.yCoord * d0, vec31D.zCoord * d0);
			this.pointedEntity = null;
			Vec3d vec33D = null;
			float f = 1.0F;
			List list = this.mc.theWorld.getEntitiesInAABBexcluding(
					entity,
					entity.getEntityBoundingBox().addCoord(vec31D.xCoord * d0, vec31D.yCoord * d0, vec31D.zCoord * d0).expand((double) f, (double) f, (double) f),
					Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith));
			double d2 = d1;

			for (Object aList : list) {
				Entity entity1 = (Entity) aList;
				float f1 = entity1.getCollisionBorderSize();
				AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand((double) f1, (double) f1, (double) f1);
				MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3D, vec32D);

				if (axisalignedbb.isVecInside(vec3D)) {
					if (d2 >= 0.0D) {
						this.pointedEntity = entity1;
						vec33D = movingobjectposition == null ? vec3D : movingobjectposition.hitVec;
						d2 = 0.0D;
					}
				} else if (movingobjectposition != null) {
					double d3 = vec3D.distanceTo(movingobjectposition.hitVec);

					if (d3 < d2 || d2 == 0.0D) {
						boolean flag2 = false;

						if (entity1 == entity.ridingEntity && !flag2) {
							if (d2 == 0.0D) {
								this.pointedEntity = entity1;
								vec33D = movingobjectposition.hitVec;
							}
						} else {
							this.pointedEntity = entity1;
							vec33D = movingobjectposition.hitVec;
							d2 = d3;
						}
					}
				}
			}

			if (this.pointedEntity != null && flag && vec3D.distanceTo(vec33D) > 3.0D) {
				this.pointedEntity = null;
				this.mc.objectMouseOver = new MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33D, null, new BlockPos(vec33D));
			}

			if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null)) {
				this.mc.objectMouseOver = new MovingObjectPosition(this.pointedEntity, vec33D);

				if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
					this.mc.pointedEntity = this.pointedEntity;
				}
			}

			mc.getProfiler().endSection();
		}
	}

	/**
	 * Update FOV modifier hand
	 */
	private void updateFovModifierHand() {
		float f = 1.0F;

		if (this.mc.getRenderViewEntity() instanceof AbstractClientPlayer) {
			AbstractClientPlayer abstractclientplayer = (AbstractClientPlayer) this.mc.getRenderViewEntity();
			f = abstractclientplayer.getFovModifier();
		}

		this.fovModifierHandPrev = this.fovModifierHand;
		this.fovModifierHand += (f - this.fovModifierHand) * 0.5F;

		if (this.fovModifierHand > 1.5F) {
			this.fovModifierHand = 1.5F;
		}

		if (this.fovModifierHand < 0.1F) {
			this.fovModifierHand = 0.1F;
		}
	}

	/**
	 * Changes the field of view of the player depending on if they are underwater or not
	 */
	private float getFOVModifier(float partialTicks, boolean doSettingsMatter) {
		if (this.debugView) {
			return 90.0F;
		}
		Entity entity = this.mc.getRenderViewEntity();
		float f = 70.0F;

		if (doSettingsMatter) {
			f = Settings.FOV.f();

			if (Config.isDynamicFov()) {
				f *= this.fovModifierHandPrev + (this.fovModifierHand - this.fovModifierHandPrev) * partialTicks;
			}
		}

		boolean flag = false;

		if (this.mc.currentScreen == null) flag = KeyBinding.ZOOM.isClicked();

		if (flag) {
			if (!Config.zoomMode) {
				Config.zoomMode = true;
				Settings.SMOOTH_CAMERA.set(true);
			}
			if (Config.zoomMode) f /= 4.0F;

		} else if (Config.zoomMode) {
			Config.zoomMode = false;
			Settings.SMOOTH_CAMERA.set(false);
			this.mouseFilterXAxis = new MouseFilter();
			this.mouseFilterYAxis = new MouseFilter();
			this.mc.renderGlobal.displayListEntitiesDirty = true;
		}

		if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).getHealth() <= 0.0F) {
			float f1 = (float) ((EntityLivingBase) entity).deathTime + partialTicks;
			f /= (1.0F - 500.0F / (f1 + 500.0F)) * 2.0F + 1.0F;
		}

		Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);

		if (block.getMaterial() == Material.water) {
			f = f * 60.0F / 70.0F;
		}

		return f;
	}

	private void hurtCameraEffect(float partialTicks) {
		if (this.mc.getRenderViewEntity() instanceof EntityLivingBase) {
			EntityLivingBase entitylivingbase = (EntityLivingBase) this.mc.getRenderViewEntity();
			float f = (float) entitylivingbase.hurtTime - partialTicks;

			if (entitylivingbase.getHealth() <= 0.0F) {
				float f1 = (float) entitylivingbase.deathTime + partialTicks;
				G.rotate(40.0F - 8000.0F / (f1 + 200.0F), 0.0F, 0.0F, 1.0F);
			}

			if (f < 0.0F) {
				return;
			}

			f = f / (float) entitylivingbase.maxHurtTime;
			f = MathHelper.sin(f * f * f * f * (float) Math.PI);
			float f2 = entitylivingbase.attackedAtYaw;
			G.rotate(-f2, 0.0F, 1.0F, 0.0F);
			G.rotate(-f * 14.0F, 0.0F, 0.0F, 1.0F);
			G.rotate(f2, 0.0F, 1.0F, 0.0F);
		}
	}

	/**
	 * Setups all the GL settings for view bobbing. Args: partialTickTime
	 */
	private void setupViewBobbing(float partialTicks) {
		if (this.mc.getRenderViewEntity() instanceof Player) {
			Player entityplayer = (Player) this.mc.getRenderViewEntity();
			float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
			float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
			float f2 = entityplayer.prevCameraYaw + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
			float f3 = entityplayer.prevCameraPitch + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
			G.translate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
			G.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, 1.0F);
			G.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, 1.0F, 0.0F, 0.0F);
			G.rotate(f3, 1.0F, 0.0F, 0.0F);
		}
	}

	/**
	 * sets up player's eye (or camera in third person mode)
	 */
	private void orientCamera(float partialTicks) {
		Entity entity = this.mc.getRenderViewEntity();
		float f = entity.getEyeHeight();
		double d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
		double d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
		double d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;

		if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPlayerSleeping()) {
			f = (float) ((double) f + 1.0D);
			G.translate(0.0F, 0.3F, 0.0F);


			BlockPos blockpos = new BlockPos(entity);
			IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);
			Block block = iblockstate.getBlock();
			if (block == Blocks.bed) {
				int j = iblockstate.getValue(BlockBed.FACING).getHorizontalIndex();
				G.rotate((float) (j * 90), 0.0F, 1.0F, 0.0F);
			}

			G.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks + 180.0F, 0.0F, -1.0F, 0.0F);
			G.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, -1.0F, 0.0F, 0.0F);
		} else if (Settings.PERSPECTIVE.i() > 0) {
			double d3 = (double) (this.thirdPersonDistanceTemp + (this.thirdPersonDistance - this.thirdPersonDistanceTemp) * partialTicks);

			float f1 = entity.rotationYaw;
			float f2 = entity.rotationPitch;

			if (Settings.PERSPECTIVE.i() == 2) {
				f2 += 180.0F;
			}

			double d4 = (double) (-MathHelper.sin(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d3;
			double d5 = (double) (MathHelper.cos(f1 / 180.0F * (float) Math.PI) * MathHelper.cos(f2 / 180.0F * (float) Math.PI)) * d3;
			double d6 = (double) -MathHelper.sin(f2 / 180.0F * (float) Math.PI) * d3;

			for (int i = 0; i < 8; ++i) {
				float f3 = (float) ((i & 1) * 2 - 1);
				float f4 = (float) ((i >> 1 & 1) * 2 - 1);
				float f5 = (float) ((i >> 2 & 1) * 2 - 1);
				f3 = f3 * 0.1F;
				f4 = f4 * 0.1F;
				f5 = f5 * 0.1F;
				MovingObjectPosition movingobjectposition = this.mc.theWorld.rayTraceBlocks(new Vec3d(d0 + (double) f3, d1 + (double) f4, d2 + (double) f5),
						new Vec3d(d0 - d4 + (double) f3 + (double) f5, d1 - d6 + (double) f4, d2 - d5 + (double) f5));

				if (movingobjectposition != null) {
					double d7 = movingobjectposition.hitVec.distanceTo(new Vec3d(d0, d1, d2));

					if (d7 < d3) {
						d3 = d7;
					}
				}
			}

			if (Settings.PERSPECTIVE.i() == 2) {
				G.rotate(180.0F, 0.0F, 1.0F, 0.0F);
			}

			G.rotate(entity.rotationPitch - f2, 1.0F, 0.0F, 0.0F);
			G.rotate(entity.rotationYaw - f1, 0.0F, 1.0F, 0.0F);
			G.translate(0.0F, 0.0F, (float) -d3);
			G.rotate(f1 - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
			G.rotate(f2 - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
		} else {
			G.translate(0.0F, 0.0F, -0.1F);
		}

		G.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 1.0F, 0.0F, 0.0F);

		G.rotate(entity.getSpectatorRotation(partialTicks), 0.0F, 1.0F, 0.0F);

		G.translate(0.0F, -f, 0.0F);
		d0 = entity.prevPosX + (entity.posX - entity.prevPosX) * (double) partialTicks;
		d1 = entity.prevPosY + (entity.posY - entity.prevPosY) * (double) partialTicks + (double) f;
		d2 = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * (double) partialTicks;
		this.cloudFog = this.mc.renderGlobal.hasCloudFog(d0, d1, d2, partialTicks);
	}

	/**
	 * sets up projection, view effects, camera position/rotation
	 */
	public void setupCameraTransform(float partialTicks, int pass) {
		this.farPlaneDistance = Settings.RENDER_DISTANCE.f() * 16;

		if (Config.isFogFancy()) {
			this.farPlaneDistance *= 0.95F;
		}

		if (Config.isFogFast()) {
			this.farPlaneDistance *= 0.83F;
		}

		G.matrixMode(5889);
		G.loadIdentity();
		float f = 0.07F;

		//		if (this.mc.gameSettings.anaglyph) {
		//			GlStateManager.translate((float) (-(pass * 2 - 1)) * f, 0.0F, 0.0F);
		//		}

		this.clipDistance = this.farPlaneDistance * 2.0F;

		if (this.clipDistance < 173.0F) {
			this.clipDistance = 173.0F;
		}

		if (this.mc.theWorld.provider.getDimensionId() == 1) {
			this.clipDistance = 256.0F;
		}

		if (this.cameraZoom != 1.0D) {
			G.translate((float) this.cameraYaw, (float) -this.cameraPitch, 0.0F);
			G.scale(this.cameraZoom, this.cameraZoom, 1.0D);
		}

		Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.clipDistance);
		G.matrixMode(5888);
		G.loadIdentity();

		//		if (this.mc.gameSettings.anaglyph) {
		//			GlStateManager.translate((float) (pass * 2 - 1) * 0.1F, 0.0F, 0.0F);
		//		}

		this.hurtCameraEffect(partialTicks);

		if (Settings.VIEW_BOBBING.b()) this.setupViewBobbing(partialTicks);

		float f1 = this.mc.thePlayer.prevTimeInPortal + (this.mc.thePlayer.timeInPortal - this.mc.thePlayer.prevTimeInPortal) * partialTicks;

		if (f1 > 0.0F) {
			byte b0 = 20;

			if (this.mc.thePlayer.isPotionActive(Potion.confusion)) {
				b0 = 7;
			}

			float f2 = 5.0F / (f1 * f1 + 5.0F) - f1 * 0.04F;
			f2 = f2 * f2;
			G.rotate(((float) this.rendererUpdateCount + partialTicks) * (float) b0, 0.0F, 1.0F, 1.0F);
			G.scale(1.0F / f2, 1.0F, 1.0F);
			G.rotate(-((float) this.rendererUpdateCount + partialTicks) * (float) b0, 0.0F, 1.0F, 1.0F);
		}

		this.orientCamera(partialTicks);

		if (this.debugView) {
			switch (this.debugViewDirection) {
				case 0:
					G.rotate(90.0F, 0.0F, 1.0F, 0.0F);
					break;

				case 1:
					G.rotate(180.0F, 0.0F, 1.0F, 0.0F);
					break;

				case 2:
					G.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
					break;

				case 3:
					G.rotate(90.0F, 1.0F, 0.0F, 0.0F);
					break;

				case 4:
					G.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
			}
		}
	}

	/**
	 * Render player hand
	 */
	private void renderHand(float partialTicks, int xOffset) {
		this.renderHand(partialTicks, xOffset, true, true, false);
	}

	public void renderHand(float partialTicks, int xOffset, boolean p_renderHand_3_, boolean p_renderHand_4_, boolean p_renderHand_5_) {
		if (this.debugView) return;

		G.matrixMode(GL11.GL_PROJECTION);
		G.loadIdentity();
		float f = 0.07F;

		if (Config.isShaders()) {
			Shaders.applyHandDepth();
		}

		Project.gluPerspective(this.getFOVModifier(partialTicks, false), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.farPlaneDistance * 2.0F);
		G.matrixMode(5888);
		G.loadIdentity();

		boolean flag = false;

		if (p_renderHand_3_) {
			G.pushMatrix();
			this.hurtCameraEffect(partialTicks);

			if (Settings.VIEW_BOBBING.b()) {
				this.setupViewBobbing(partialTicks);
			}

			flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase) this.mc.getRenderViewEntity()).isPlayerSleeping();

			if (Settings.PERSPECTIVE.i() == 0 && !flag && !Settings.HIDE_GUI.b() && !this.mc.playerController.isSpectator()) {
				this.enableLightmap();

				if (Config.isShaders()) {
					ShadersRender.renderItemFP(this.itemRenderer, partialTicks, p_renderHand_5_);
				} else {
					this.itemRenderer.renderItemInFirstPerson(partialTicks);
				}

				this.disableLightmap();
			}

			G.popMatrix();
		}

		if (!p_renderHand_4_) {
			return;
		}

		this.disableLightmap();

		if (Settings.PERSPECTIVE.i() == 0 && !flag) {
			this.itemRenderer.renderOverlays(partialTicks);
			this.hurtCameraEffect(partialTicks);
		}

		if (Settings.VIEW_BOBBING.b()) {
			this.setupViewBobbing(partialTicks);
		}
	}

	public void disableLightmap() {
		G.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		G.disableTexture2D();
		G.setActiveTexture(OpenGlHelper.defaultTexUnit);

		if (Config.isShaders()) {
			Shaders.disableLightmap();
		}
	}

	public void enableLightmap() {
		G.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		G.matrixMode(5890);
		G.loadIdentity();
		float f = 0.00390625F;
		G.scale(f, f, f);
		G.translate(8.0F, 8.0F, 8.0F);
		G.matrixMode(5888);
		this.mc.getTextureManager().bindTexture(this.locationLightMap);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
		G.color(1.0F, 1.0F, 1.0F, 1.0F);
		G.enableTexture2D();
		G.setActiveTexture(OpenGlHelper.defaultTexUnit);

		if (Config.isShaders()) {
			Shaders.enableLightmap();
		}
	}

	/**
	 * Recompute a random value that is applied to block color in updateLightmap()
	 */
	private void updateTorchFlicker() {
		this.torchFlickerDX = (float) ((double) this.torchFlickerDX + (Math.random() - Math.random()) * Math.random() * Math.random());
		this.torchFlickerDX = (float) ((double) this.torchFlickerDX * 0.9D);
		this.torchFlickerX += (this.torchFlickerDX - this.torchFlickerX) * 1.0F;
		this.lightmapUpdateNeeded = true;
	}

	private void updateLightmap(float partialTicks) {
		if (this.lightmapUpdateNeeded) {
			mc.getProfiler().startSection("lightTex");
			WorldClient worldclient = this.mc.theWorld;

			if (worldclient != null) {
				if (Config.isCustomColors() && CustomColors.updateLightmap(worldclient, this.torchFlickerX, this.lightmapColors, this.mc.thePlayer.isPotionActive(Potion.nightVision))) {
					this.lightmapTexture.updateDynamicTexture();
					this.lightmapUpdateNeeded = false;
					mc.getProfiler().endSection();
					return;
				}

				float f = worldclient.getSunBrightness(1.0F);
				float f1 = f * 0.95F + 0.05F;

				for (int i = 0; i < 256; ++i) {
					float f2 = worldclient.provider.getLightBrightnessTable()[i / 16] * f1;
					float f3 = worldclient.provider.getLightBrightnessTable()[i % 16] * (this.torchFlickerX * 0.1F + 1.5F);

					if (worldclient.getLastLightningBolt() > 0) {
						f2 = worldclient.provider.getLightBrightnessTable()[i / 16];
					}

					float f4 = f2 * (f * 0.65F + 0.35F);
					float f5 = f2 * (f * 0.65F + 0.35F);
					float f6 = f3 * ((f3 * 0.6F + 0.4F) * 0.6F + 0.4F);
					float f7 = f3 * (f3 * f3 * 0.6F + 0.4F);
					float f8 = f4 + f3;
					float f9 = f5 + f6;
					float f10 = f2 + f7;
					f8 = f8 * 0.96F + 0.03F;
					f9 = f9 * 0.96F + 0.03F;
					f10 = f10 * 0.96F + 0.03F;

					if (this.bossColorModifier > 0.0F) {
						float f11 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
						f8 = f8 * (1.0F - f11) + f8 * 0.7F * f11;
						f9 = f9 * (1.0F - f11) + f9 * 0.6F * f11;
						f10 = f10 * (1.0F - f11) + f10 * 0.6F * f11;
					}

					if (worldclient.provider.getDimensionId() == 1) {
						f8 = 0.22F + f3 * 0.75F;
						f9 = 0.28F + f6 * 0.75F;
						f10 = 0.25F + f7 * 0.75F;
					}

					if (this.mc.thePlayer.isPotionActive(Potion.nightVision)) {
						float f15 = this.getNightVisionBrightness(this.mc.thePlayer, partialTicks);
						float f12 = 1.0F / f8;

						if (f12 > 1.0F / f9) {
							f12 = 1.0F / f9;
						}

						if (f12 > 1.0F / f10) {
							f12 = 1.0F / f10;
						}

						f8 = f8 * (1.0F - f15) + f8 * f12 * f15;
						f9 = f9 * (1.0F - f15) + f9 * f12 * f15;
						f10 = f10 * (1.0F - f15) + f10 * f12 * f15;
					}

					if (f8 > 1.0F) {
						f8 = 1.0F;
					}

					if (f9 > 1.0F) {
						f9 = 1.0F;
					}

					if (f10 > 1.0F) {
						f10 = 1.0F;
					}

					float f16 = Settings.GAMMA.f();
					float f17 = 1.0F - f8;
					float f13 = 1.0F - f9;
					float f14 = 1.0F - f10;
					f17 = 1.0F - f17 * f17 * f17 * f17;
					f13 = 1.0F - f13 * f13 * f13 * f13;
					f14 = 1.0F - f14 * f14 * f14 * f14;
					f8 = f8 * (1.0F - f16) + f17 * f16;
					f9 = f9 * (1.0F - f16) + f13 * f16;
					f10 = f10 * (1.0F - f16) + f14 * f16;
					f8 = f8 * 0.96F + 0.03F;
					f9 = f9 * 0.96F + 0.03F;
					f10 = f10 * 0.96F + 0.03F;

					if (f8 > 1.0F) {
						f8 = 1.0F;
					}

					if (f9 > 1.0F) {
						f9 = 1.0F;
					}

					if (f10 > 1.0F) {
						f10 = 1.0F;
					}

					if (f8 < 0.0F) {
						f8 = 0.0F;
					}

					if (f9 < 0.0F) {
						f9 = 0.0F;
					}

					if (f10 < 0.0F) {
						f10 = 0.0F;
					}

					short short1 = 255;
					int j = (int) (f8 * 255.0F);
					int k = (int) (f9 * 255.0F);
					int l = (int) (f10 * 255.0F);
					this.lightmapColors[i] = short1 << 24 | j << 16 | k << 8 | l;
				}

				this.lightmapTexture.updateDynamicTexture();
				this.lightmapUpdateNeeded = false;
				mc.getProfiler().endSection();
			}
		}
	}

	public float getNightVisionBrightness(EntityLivingBase entitylivingbaseIn, float partialTicks) {
		int i = entitylivingbaseIn.getActivePotionEffect(Potion.nightVision).getDuration();
		return i > 200 ? 1.0F : 0.7F + MathHelper.sin(((float) i - partialTicks) * (float) Math.PI * 0.2F) * 0.3F;
	}

	public void func_181560_a(float p_181560_1_, long p_181560_2_) {
		this.frameInit();
		boolean flag = Display.isActive();

		if (!flag && Settings.PAUSE_FOCUS.b()) {
			if (Minecraft.getSystemTime() - this.prevFrameTime > 500L) this.mc.displayInGameMenu();
		} else this.prevFrameTime = Minecraft.getSystemTime();

		mc.getProfiler().startSection("mouse");

		if (flag && Minecraft.isRunningOnMac && this.mc.inGameHasFocus && !Mouse.isInsideWindow()) {
			Mouse.setGrabbed(false);
			Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
			Mouse.setGrabbed(true);
		}

		if (this.mc.inGameHasFocus && flag) {
			this.mc.mouseHelper.mouseXYChange();
			float f = Settings.SENSITIVITY.f() * 0.6F + 0.2F;
			float f1 = f * f * f * 8.0F;
			float f2 = (float) this.mc.mouseHelper.deltaX * f1;
			float f3 = (float) this.mc.mouseHelper.deltaY * f1;

			byte b0 = 1;
			if (Settings.INVERT_MOUSE.b()) b0 = -1;

			if (Settings.SMOOTH_CAMERA.b()) {
				this.smoothCamYaw += f2;
				this.smoothCamPitch += f3;
				float f4 = p_181560_1_ - this.smoothCamPartialTicks;
				this.smoothCamPartialTicks = p_181560_1_;
				f2 = this.smoothCamFilterX * f4;
				f3 = this.smoothCamFilterY * f4;
				this.mc.thePlayer.setAngles(f2, f3 * (float) b0);
			} else {
				this.smoothCamYaw = 0.0F;
				this.smoothCamPitch = 0.0F;
				this.mc.thePlayer.setAngles(f2, f3 * (float) b0);
			}
		}

		mc.getProfiler().endSection();

		final ScaledResolution scaledresolution = new ScaledResolution(this.mc);
		int l = scaledresolution.getScaledWidth();
		int i1 = scaledresolution.getScaledHeight();
		final int j1 = Mouse.getX() * l / this.mc.displayWidth;
		final int k1 = i1 - Mouse.getY() * i1 / this.mc.displayHeight - 1;
		int l1 = (int) Settings.FRAMERATE_LIMIT.f();

		if (this.mc.theWorld != null) {
			mc.getProfiler().startSection("level");
			int i = Math.min(Minecraft.getDebugFPS(), l1);
			i = Math.max(i, 60);
			long j = System.nanoTime() - p_181560_2_;
			long k = Math.max((long) (1000000000 / i / 4) - j, 0L);
			this.renderWorld(p_181560_1_, System.nanoTime() + k);

			if (OpenGlHelper.shadersSupported) {
				this.mc.renderGlobal.renderEntityOutlineFramebuffer();

				if (this.theShaderGroup != null && this.useShader) {
					G.matrixMode(5890);
					G.pushMatrix();
					G.loadIdentity();
					this.theShaderGroup.loadShaderGroup(p_181560_1_);
					G.popMatrix();
				}

				this.mc.getFramebuffer().bindFramebuffer(true);
			}

			this.renderEndNanoTime = System.nanoTime();
			mc.getProfiler().endStartSection("gui");

			if (!Settings.HIDE_GUI.b() || this.mc.currentScreen != null) {
				G.alphaFunc(516, 0.1F);
				this.mc.ingameGUI.renderGameOverlay(p_181560_1_);

				//                    if (this.mc.gameSettings.ofShowFps && !this.mc.gameSettings.showDebugInfo)
				//                    {
				//                        Config.drawFps();
				//                    }

				if (Settings.SHOW_DEBUG.b()) {
					Lagometer.showLagometer(scaledresolution);
				}
			}

			mc.getProfiler().endSection();
		} else {
			G.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
			G.matrixMode(5889);
			G.loadIdentity();
			G.matrixMode(5888);
			G.loadIdentity();
			this.setupOverlayRendering();
			this.renderEndNanoTime = System.nanoTime();
			TileEntityRendererDispatcher.instance.renderEngine = this.mc.getTextureManager();
		}

		if (this.mc.currentScreen != null) {
			G.clear(256);

			try {
				this.mc.currentScreen.drawScreen(j1, k1, p_181560_1_);
			} catch (Throwable throwable) {
				CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Rendering screen");
				CrashReportCategory crashreportcategory = crashreport.makeCategory("Screen render details");
				crashreportcategory.addCrashSectionCallable("Screen name", () -> Minecraft.getMinecraft().currentScreen.getClass().getCanonicalName());
				crashreportcategory.addCrashSectionCallable("Mouse location", (Callable) () -> String.format("Scaled: (%d, %d). Absolute: (%d, %d)", j1, k1, Mouse.getX(), Mouse.getY()));
				crashreportcategory.addCrashSectionCallable("Screen size",
						(Callable) () -> String.format("Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d", scaledresolution.getScaledWidth(), scaledresolution.getScaledHeight(),
								EntityRenderer.this.mc.displayWidth, EntityRenderer.this.mc.displayHeight, scaledresolution.getScaleFactor()));
				throw new ReportedException(crashreport);
			}
		}

		this.frameFinish();
		this.waitForServerThread();
		Lagometer.updateLagometer();
	}

	private boolean isDrawBlockOutline() {
		if (!this.drawBlockOutline) {
			return false;
		}
		Entity entity = this.mc.getRenderViewEntity();
		boolean flag = entity instanceof Player && !Settings.HIDE_GUI.b();

		if (flag && !((Player) entity).capabilities.allowEdit) {
			if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
				BlockPos blockpos = this.mc.objectMouseOver.getBlockPos();
				IBlockState iblockstate = this.mc.theWorld.getBlockState(blockpos);
				Block block = iblockstate.getBlock();

				if (this.mc.playerController.getCurrentGameType() == WorldSettings.GameType.SPECTATOR) {
					flag = this.mc.theWorld.getTileEntity(blockpos) instanceof Inventory;
				} else {
					ItemStack itemstack = ((Player) entity).inventory.getCurrentItem();
					flag = itemstack != null && (itemstack.canDestroy(block) || itemstack.canPlaceOn(block));
				}
			}
		}

		return flag;
	}

	private void renderWorldDirections(float partialTicks) {
		if (!Settings.SHOW_DEBUG.b() || Settings.REDUCED_DEBUG_INFO.i() != 0 || Settings.HIDE_GUI.b() || this.mc.thePlayer.hasReducedDebug() || Settings.PERSPECTIVE.i() != 0) return;
		Entity entity = this.mc.getRenderViewEntity();
		G.enableBlend();
		G.tryBlendFuncSeparate(770, 771, 1, 0);
		GL11.glLineWidth(1.0F);
		G.disableTexture2D();
		G.depthMask(false);
		G.pushMatrix();
		G.matrixMode(5888);
		G.loadIdentity();
		this.orientCamera(partialTicks);
		G.translate(0.0F, entity.getEyeHeight(), 0.0F);
		RenderGlobal.func_181563_a(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 0.005D, 1.0E-4D, 1.0E-4D), 255, 0, 0, 255);
		RenderGlobal.func_181563_a(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0E-4D, 1.0E-4D, 0.005D), 0, 0, 255, 255);
		RenderGlobal.func_181563_a(new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0E-4D, 0.0033D, 1.0E-4D), 0, 255, 0, 255);
		G.popMatrix();
		G.depthMask(true);
		G.enableTexture2D();
		G.disableBlend();
	}

	public void renderWorld(float partialTicks, long finishTimeNano) {
		this.updateLightmap(partialTicks);

		if (this.mc.getRenderViewEntity() == null) {
			this.mc.setRenderViewEntity(this.mc.thePlayer);
		}

		this.getMouseOver(partialTicks);

		if (Config.isShaders()) {
			Shaders.beginRender(this.mc, partialTicks, finishTimeNano);
		}

		G.enableDepth();
		G.enableAlpha();
		G.alphaFunc(516, 0.1F);
		mc.getProfiler().startSection("center");

		//		if (this.mc.gameSettings.anaglyph) {
		//			anaglyphField = 0;
		//			GlStateManager.colorMask(false, true, true, false);
		//			this.renderWorldPass(0, partialTicks, finishTimeNano);
		//			anaglyphField = 1;
		//			GlStateManager.colorMask(true, false, false, false);
		//			this.renderWorldPass(1, partialTicks, finishTimeNano);
		//			GlStateManager.colorMask(true, true, true, false);
		//		} else
		this.renderWorldPass(2, partialTicks, finishTimeNano);

		mc.getProfiler().endSection();
	}

	private void renderWorldPass(int pass, float partialTicks, long finishTimeNano) {
		boolean flag = Config.isShaders();

		if (flag) {
			Shaders.beginRenderPass(pass, partialTicks, finishTimeNano);
		}

		RenderGlobal renderglobal = this.mc.renderGlobal;
		EffectRenderer effectrenderer = this.mc.effectRenderer;
		boolean flag1 = this.isDrawBlockOutline();
		G.enableCull();
		mc.getProfiler().endStartSection("clear");

		if (flag) {
			Shaders.setViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
		} else {
			G.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
		}

		this.updateFogColor(partialTicks);
		G.clear(16640);

		if (flag) {
			Shaders.clearRenderBuffer();
		}

		mc.getProfiler().endStartSection("camera");
		this.setupCameraTransform(partialTicks, pass);

		if (flag) {
			Shaders.setCamera(partialTicks);
		}

		ActiveRenderInfo.updateRenderInfo(this.mc.thePlayer, Settings.PERSPECTIVE.i() == 2);
		mc.getProfiler().endStartSection("frustum");
		ClippingHelperImpl.getInstance();
		mc.getProfiler().endStartSection("culling");
		Frustum frustum = new Frustum();
		Entity entity = this.mc.getRenderViewEntity();
		double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
		double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
		double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;

		if (flag) {
			ShadersRender.setFrustrumPosition(frustum, d0, d1, d2);
		} else {
			frustum.setPosition(d0, d1, d2);
		}

		if ((Config.isSkyEnabled() || Config.isSunMoonEnabled() || Config.isStarsEnabled()) && !Shaders.isShadowPass) {
			this.setupFog(-1, partialTicks);
			mc.getProfiler().endStartSection("sky");
			G.matrixMode(5889);
			G.loadIdentity();
			Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.clipDistance);
			G.matrixMode(5888);

			if (flag) {
				Shaders.beginSky();
			}

			renderglobal.renderSky(partialTicks, pass);

			if (flag) {
				Shaders.endSky();
			}

			G.matrixMode(5889);
			G.loadIdentity();
			Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.clipDistance);
			G.matrixMode(5888);
		} else {
			G.disableBlend();
		}

		this.setupFog(0, partialTicks);
		G.shadeModel(7425);

		if (entity.posY + (double) entity.getEyeHeight() < 128.0D + (double) (Settings.CLOUD_HEIGHT.f() * 128.0F)) {
			this.renderCloudsCheck(renderglobal, partialTicks, pass);
		}

		mc.getProfiler().endStartSection("prepareterrain");
		this.setupFog(0, partialTicks);
		this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
		RenderHelper.disableStandardItemLighting();
		mc.getProfiler().endStartSection("terrain_setup");

		if (flag) {
			ShadersRender.setupTerrain(renderglobal, entity, (double) partialTicks, frustum, this.frameCount++, this.mc.thePlayer.isSpectator());
		} else {
			renderglobal.setupTerrain(entity, (double) partialTicks, frustum, this.frameCount++, this.mc.thePlayer.isSpectator());
		}

		if (pass == 0 || pass == 2) {
			mc.getProfiler().endStartSection("updatechunks");
			Lagometer.timerChunkUpload.start();
			this.mc.renderGlobal.updateChunks(finishTimeNano);
			Lagometer.timerChunkUpload.end();
		}

		mc.getProfiler().endStartSection("terrain");
		Lagometer.timerTerrain.start();

		if (Settings.SMOOTH_FPS.b() && pass > 0) {
			mc.getProfiler().endStartSection("finish");
			GL11.glFinish();
			mc.getProfiler().endStartSection("terrain");
		}

		G.matrixMode(5888);
		G.pushMatrix();
		G.disableAlpha();

		if (flag) {
			ShadersRender.beginTerrainSolid();
		}

		renderglobal.renderBlockLayer(EnumWorldBlockLayer.SOLID, (double) partialTicks, pass, entity);
		G.enableAlpha();

		if (flag) {
			ShadersRender.beginTerrainCutoutMipped();
		}

		renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT_MIPPED, (double) partialTicks, pass, entity);
		this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);

		if (flag) {
			ShadersRender.beginTerrainCutout();
		}

		renderglobal.renderBlockLayer(EnumWorldBlockLayer.CUTOUT, (double) partialTicks, pass, entity);
		this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();

		if (flag) {
			ShadersRender.endTerrain();
		}

		Lagometer.timerTerrain.end();
		G.shadeModel(7424);
		G.alphaFunc(516, 0.1F);

		if (!this.debugView) {
			G.matrixMode(5888);
			G.popMatrix();
			G.pushMatrix();
			RenderHelper.enableStandardItemLighting();
			mc.getProfiler().endStartSection("entities");

			renderglobal.renderEntities(entity, frustum, partialTicks);

			if (Settings.BOW_PATH.b()) BowPathRenderer.render(partialTicks);
			//WorldEditUI.render(partialTicks);
			RenderHelper.disableStandardItemLighting();
			this.disableLightmap();
			G.matrixMode(5888);
			G.popMatrix();
			G.pushMatrix();

			if (this.mc.objectMouseOver != null && entity.isInsideOfMaterial(Material.water) && flag1) {
				Player entityplayer = (Player) entity;
				G.disableAlpha();
				mc.getProfiler().endStartSection("outline");

				renderglobal.drawSelectionBox(entityplayer, this.mc.objectMouseOver, 0, partialTicks);

				G.enableAlpha();
			}

		}

		G.matrixMode(5888);
		G.popMatrix();

		if (flag1 && this.mc.objectMouseOver != null && !entity.isInsideOfMaterial(Material.water)) {
			Player entityplayer1 = (Player) entity;
			G.disableAlpha();
			mc.getProfiler().endStartSection("outline");

			renderglobal.drawSelectionBox(entityplayer1, this.mc.objectMouseOver, 0, partialTicks);
			G.enableAlpha();
		}

		if (!renderglobal.damagedBlocks.isEmpty()) {
			mc.getProfiler().endStartSection("destroyProgress");
			G.enableBlend();
			G.tryBlendFuncSeparate(770, 1, 1, 0);
			this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false);
			renderglobal.drawBlockDamageTexture(Tessellator.getInstance(), Tessellator.getInstance().getWorldRenderer(), entity, partialTicks);
			this.mc.getTextureManager().getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap();
			G.disableBlend();
		}

		G.tryBlendFuncSeparate(770, 771, 1, 0);
		G.disableBlend();

		if (!this.debugView) {
			this.enableLightmap();
			mc.getProfiler().endStartSection("litParticles");

			if (flag) {
				Shaders.beginLitParticles();
			}

			effectrenderer.renderLitParticles(entity, partialTicks);
			RenderHelper.disableStandardItemLighting();
			this.setupFog(0, partialTicks);
			mc.getProfiler().endStartSection("particles");

			if (flag) {
				Shaders.beginParticles();
			}

			effectrenderer.renderParticles(entity, partialTicks);

			if (flag) {
				Shaders.endParticles();
			}

			this.disableLightmap();
		}

		G.depthMask(false);
		G.enableCull();
		mc.getProfiler().endStartSection("weather");

		if (flag) Shaders.beginWeather();
		this.renderRainSnow(partialTicks);
		if (flag) Shaders.endWeather();

		G.depthMask(true);
		renderglobal.renderWorldBorder(entity, partialTicks);

		if (flag) {
			ShadersRender.renderHand0(this, partialTicks, pass);
			Shaders.preWater();
		}

		G.disableBlend();
		G.enableCull();
		G.tryBlendFuncSeparate(770, 771, 1, 0);
		G.alphaFunc(516, 0.1F);
		this.setupFog(0, partialTicks);
		G.enableBlend();
		G.depthMask(false);
		this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
		G.shadeModel(7425);
		mc.getProfiler().endStartSection("translucent");

		if (flag) {
			Shaders.beginWater();
		}

		renderglobal.renderBlockLayer(EnumWorldBlockLayer.TRANSLUCENT, (double) partialTicks, pass, entity);

		if (flag) {
			Shaders.endWater();
		}

		G.shadeModel(7424);
		G.depthMask(true);
		G.enableCull();
		G.disableBlend();
		G.disableFog();

		if (entity.posY + (double) entity.getEyeHeight() >= 128.0D + (double) (Settings.CLOUD_HEIGHT.f() * 128.0F)) {
			mc.getProfiler().endStartSection("aboveClouds");
			this.renderCloudsCheck(renderglobal, partialTicks, pass);
		}

		mc.getProfiler().endStartSection("hand");

		if (this.renderHand && !Shaders.isShadowPass) {
			if (flag) {
				ShadersRender.renderHand1(this, partialTicks, pass);
				Shaders.renderCompositeFinal();
			}

			G.clear(256);

			if (flag) {
				ShadersRender.renderFPOverlay(this, partialTicks, pass);
			} else {
				this.renderHand(partialTicks, pass);
			}

			this.renderWorldDirections(partialTicks);
		}

		if (flag) {
			Shaders.endRender();
		}
	}

	private void renderCloudsCheck(RenderGlobal renderGlobalIn, float partialTicks, int pass) {
		if (Settings.RENDER_DISTANCE.f() >= 4 && !Config.isCloudsOff() && Shaders.shouldRenderClouds()) {
			mc.getProfiler().endStartSection("clouds");
			G.matrixMode(5889);
			G.loadIdentity();
			Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.clipDistance * 4.0F);
			G.matrixMode(5888);
			G.pushMatrix();
			this.setupFog(0, partialTicks);
			renderGlobalIn.renderClouds(partialTicks, pass);
			G.disableFog();
			G.popMatrix();
			G.matrixMode(5889);
			G.loadIdentity();
			Project.gluPerspective(this.getFOVModifier(partialTicks, true), (float) this.mc.displayWidth / (float) this.mc.displayHeight, 0.05F, this.clipDistance);
			G.matrixMode(5888);
		}
	}

	private void addRainParticles() {
		float f = this.mc.theWorld.getRainStrength(1.0F);

		if (!Config.isRainFancy()) {
			f /= 2.0F;
		}

		if (f != 0.0F && Config.isRainSplash()) {
			this.random.setSeed((long) this.rendererUpdateCount * 312987231L);
			Entity entity = this.mc.getRenderViewEntity();
			WorldClient worldclient = this.mc.theWorld;
			BlockPos blockpos = new BlockPos(entity);
			byte b0 = 10;
			double d0 = 0.0D;
			double d1 = 0.0D;
			double d2 = 0.0D;
			int i = 0;
			int j = (int) (100.0F * f * f);

			if (Settings.PARTICLES.i() == 1) {
				j >>= 1;
			} else if (Settings.PARTICLES.i() == 2) {
				j = 0;
			}

			for (int k = 0; k < j; ++k) {
				BlockPos blockpos1 = worldclient.getPrecipitationHeight(blockpos.add(this.random.nextInt(b0) - this.random.nextInt(b0), 0, this.random.nextInt(b0) - this.random.nextInt(b0)));
				Biome biomegenbase = worldclient.getBiomeGenForCoords(blockpos1);
				BlockPos blockpos2 = blockpos1.down();
				Block block = worldclient.getBlockState(blockpos2).getBlock();

				if (blockpos1.getY() <= blockpos.getY() + b0 && blockpos1.getY() >= blockpos.getY() - b0 && biomegenbase.canSpawnLightningBolt() && biomegenbase.getFloatTemperature(
						blockpos1) >= 0.15F) {
					double d3 = this.random.nextDouble();
					double d4 = this.random.nextDouble();

					if (block.getMaterial() == Material.lava) {
						this.mc.theWorld.spawnParticle(ParticleType.SMOKE_NORMAL, (double) blockpos1.getX() + d3, (double) ((float) blockpos1.getY() + 0.1F) - block.getBlockBoundsMinY(),
								(double) blockpos1.getZ() + d4, 0.0D, 0.0D, 0.0D);
					} else if (block.getMaterial() != Material.air) {
						block.setBlockBoundsBasedOnState(worldclient, blockpos2);
						++i;

						if (this.random.nextInt(i) == 0) {
							d0 = (double) blockpos2.getX() + d3;
							d1 = (double) ((float) blockpos2.getY() + 0.1F) + block.getBlockBoundsMaxY() - 1.0D;
							d2 = (double) blockpos2.getZ() + d4;
						}

						this.mc.theWorld.spawnParticle(ParticleType.WATER_DROP, (double) blockpos2.getX() + d3, (double) ((float) blockpos2.getY() + 0.1F) + block.getBlockBoundsMaxY(),
								(double) blockpos2.getZ() + d4, 0.0D, 0.0D, 0.0D);
					}
				}
			}

			if (i > 0 && this.random.nextInt(3) < this.rainSoundCounter++) {
				this.rainSoundCounter = 0;

				if (d1 > (double) (blockpos.getY() + 1) && worldclient.getPrecipitationHeight(blockpos).getY() > MathHelper.floor_float((float) blockpos.getY())) {
					this.mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.1F, 0.5F, false);
				} else {
					this.mc.theWorld.playSound(d0, d1, d2, "ambient.weather.rain", 0.2F, 1.0F, false);
				}
			}
		}
	}

	/**
	 * Render rain and snow
	 */
	protected void renderRainSnow(float partialTicks) {
		float f5 = this.mc.theWorld.getRainStrength(partialTicks);

		if (f5 > 0.0F) {
			if (Config.isRainOff()) {
				return;
			}

			this.enableLightmap();
			Entity entity = this.mc.getRenderViewEntity();
			WorldClient worldclient = this.mc.theWorld;
			int i = MathHelper.floor_double(entity.posX);
			int j = MathHelper.floor_double(entity.posY);
			int k = MathHelper.floor_double(entity.posZ);
			Tessellator tessellator = Tessellator.getInstance();
			WorldRenderer worldrenderer = tessellator.getWorldRenderer();
			G.disableCull();
			GL11.glNormal3f(0.0F, 1.0F, 0.0F);
			G.enableBlend();
			G.tryBlendFuncSeparate(770, 771, 1, 0);
			G.alphaFunc(516, 0.1F);
			double d0 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
			double d1 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
			double d2 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
			int l = MathHelper.floor_double(d1);
			byte b0 = 5;

			if (Config.isRainFancy()) {
				b0 = 10;
			}

			byte b1 = -1;
			float f = (float) this.rendererUpdateCount + partialTicks;
			worldrenderer.setTranslation(-d0, -d1, -d2);

			if (Config.isRainFancy()) {
				b0 = 10;
			}

			G.color(1.0F, 1.0F, 1.0F, 1.0F);
			BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

			for (int i1 = k - b0; i1 <= k + b0; ++i1) {
				for (int j1 = i - b0; j1 <= i + b0; ++j1) {
					int k1 = (i1 - k + 16) * 32 + j1 - i + 16;
					double d3 = (double) this.rainXCoords[k1] * 0.5D;
					double d4 = (double) this.rainYCoords[k1] * 0.5D;
					blockpos$mutableblockpos.setXyz(j1, 0, i1);
					Biome biomegenbase = worldclient.getBiomeGenForCoords(blockpos$mutableblockpos);

					if (biomegenbase.canSpawnLightningBolt() || biomegenbase.getEnableSnow()) {
						int l1 = worldclient.getPrecipitationHeight(blockpos$mutableblockpos).getY();
						int i2 = j - b0;
						int j2 = j + b0;

						if (i2 < l1) {
							i2 = l1;
						}

						if (j2 < l1) {
							j2 = l1;
						}

						int k2 = l1;

						if (l1 < l) {
							k2 = l;
						}

						if (i2 != j2) {
							this.random.setSeed((long) (j1 * j1 * 3121 + j1 * 45238971 ^ i1 * i1 * 418711 + i1 * 13761));
							blockpos$mutableblockpos.setXyz(j1, i2, i1);
							float f1 = biomegenbase.getFloatTemperature(blockpos$mutableblockpos);

							if (f1 >= 0.15F) {
								if (b1 != 0) {
									if (b1 >= 0) {
										tessellator.draw();
									}

									b1 = 0;
									this.mc.getTextureManager().bindTexture(locationRainPng);
									worldrenderer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
								}

								double d5 = ((double) (this.rendererUpdateCount + j1 * j1 * 3121 + j1 * 45238971 + i1 * i1 * 418711 + i1 * 13761 & 31) + (double) partialTicks) / 32.0D * (3.0D + this.random.nextDouble());
								double d6 = (double) ((float) j1 + 0.5F) - entity.posX;
								double d7 = (double) ((float) i1 + 0.5F) - entity.posZ;
								float f2 = MathHelper.sqrt_double(d6 * d6 + d7 * d7) / (float) b0;
								float f3 = ((1.0F - f2 * f2) * 0.5F + 0.5F) * f5;
								blockpos$mutableblockpos.setXyz(j1, k2, i1);
								int l2 = worldclient.getCombinedLight(blockpos$mutableblockpos, 0);
								int i3 = l2 >> 16 & 65535;
								int j3 = l2 & 65535;
								worldrenderer.pos((double) j1 - d3 + 0.5D, (double) i2, (double) i1 - d4 + 0.5D).tex(0.0D, (double) i2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(i3,
										j3).endVertex();
								worldrenderer.pos((double) j1 + d3 + 0.5D, (double) i2, (double) i1 + d4 + 0.5D).tex(1.0D, (double) i2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(i3,
										j3).endVertex();
								worldrenderer.pos((double) j1 + d3 + 0.5D, (double) j2, (double) i1 + d4 + 0.5D).tex(1.0D, (double) j2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(i3,
										j3).endVertex();
								worldrenderer.pos((double) j1 - d3 + 0.5D, (double) j2, (double) i1 - d4 + 0.5D).tex(0.0D, (double) j2 * 0.25D + d5).color(1.0F, 1.0F, 1.0F, f3).lightmap(i3,
										j3).endVertex();
							} else {
								if (b1 != 1) {
									if (b1 >= 0) {
										tessellator.draw();
									}

									b1 = 1;
									this.mc.getTextureManager().bindTexture(locationSnowPng);
									worldrenderer.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
								}

								double d8 = (double) (((float) (this.rendererUpdateCount & 511) + partialTicks) / 512.0F);
								double d9 = this.random.nextDouble() + (double) f * 0.01D * (double) (float) this.random.nextGaussian();
								double d10 = this.random.nextDouble() + (double) (f * (float) this.random.nextGaussian()) * 0.001D;
								double d11 = (double) ((float) j1 + 0.5F) - entity.posX;
								double d12 = (double) ((float) i1 + 0.5F) - entity.posZ;
								float f6 = MathHelper.sqrt_double(d11 * d11 + d12 * d12) / (float) b0;
								float f4 = ((1.0F - f6 * f6) * 0.3F + 0.5F) * f5;
								blockpos$mutableblockpos.setXyz(j1, k2, i1);
								int k3 = (worldclient.getCombinedLight(blockpos$mutableblockpos, 0) * 3 + 15728880) / 4;
								int l3 = k3 >> 16 & 65535;
								int i4 = k3 & 65535;
								worldrenderer.pos((double) j1 - d3 + 0.5D, (double) i2, (double) i1 - d4 + 0.5D).tex(0.0D + d9, (double) i2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(l3,
										i4).endVertex();
								worldrenderer.pos((double) j1 + d3 + 0.5D, (double) i2, (double) i1 + d4 + 0.5D).tex(1.0D + d9, (double) i2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(l3,
										i4).endVertex();
								worldrenderer.pos((double) j1 + d3 + 0.5D, (double) j2, (double) i1 + d4 + 0.5D).tex(1.0D + d9, (double) j2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(l3,
										i4).endVertex();
								worldrenderer.pos((double) j1 - d3 + 0.5D, (double) j2, (double) i1 - d4 + 0.5D).tex(0.0D + d9, (double) j2 * 0.25D + d8 + d10).color(1.0F, 1.0F, 1.0F, f4).lightmap(l3,
										i4).endVertex();
							}
						}
					}
				}
			}

			if (b1 >= 0) {
				tessellator.draw();
			}

			worldrenderer.setTranslation(0.0D, 0.0D, 0.0D);
			G.enableCull();
			G.disableBlend();
			G.alphaFunc(516, 0.1F);
			this.disableLightmap();
		}
	}

	/**
	 * Setup orthogonal projection for rendering GUI screen overlays
	 */
	public void setupOverlayRendering() {
		ScaledResolution scaledresolution = new ScaledResolution(this.mc);
		G.clear(256);
		G.matrixMode(5889);
		G.loadIdentity();
		G.ortho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
		G.matrixMode(5888);
		G.loadIdentity();
		G.translate(0.0F, 0.0F, -2000.0F);
	}

	/**
	 * calculates fog and calls glClearColor
	 */
	private void updateFogColor(float partialTicks) {
		WorldClient worldclient = this.mc.theWorld;
		Entity entity = this.mc.getRenderViewEntity();
		float f = 0.25F + 0.75F * Settings.RENDER_DISTANCE.f() / 32.0F;
		f = 1.0F - (float) Math.pow((double) f, 0.25D);
		Vec3d vec3D = worldclient.getSkyColor(this.mc.getRenderViewEntity(), partialTicks);
		vec3D = CustomColors.getWorldSkyColor(vec3D, worldclient, this.mc.getRenderViewEntity(), partialTicks);
		float f1 = (float) vec3D.xCoord;
		float f2 = (float) vec3D.yCoord;
		float f3 = (float) vec3D.zCoord;
		Vec3d vec31D = worldclient.getFogColor(partialTicks);
		vec31D = CustomColors.getWorldFogColor(vec31D, worldclient, this.mc.getRenderViewEntity(), partialTicks);
		this.fogColorRed = (float) vec31D.xCoord;
		this.fogColorGreen = (float) vec31D.yCoord;
		this.fogColorBlue = (float) vec31D.zCoord;

		if (Settings.RENDER_DISTANCE.f() >= 4) {
			double d0 = -1.0D;
			Vec3d vec32D = MathHelper.sin(worldclient.getCelestialAngleRadians(partialTicks)) > 0.0F ? new Vec3d(d0, 0.0D, 0.0D) : new Vec3d(1.0D, 0.0D, 0.0D);
			float f4 = (float) entity.getLook(partialTicks).dotProduct(vec32D);

			if (f4 < 0.0F) {
				f4 = 0.0F;
			}

			if (f4 > 0.0F) {
				float[] afloat = worldclient.provider.calcSunriseSunsetColors(worldclient.getCelestialAngle(partialTicks), partialTicks);

				if (afloat != null) {
					f4 = f4 * afloat[3];
					this.fogColorRed = this.fogColorRed * (1.0F - f4) + afloat[0] * f4;
					this.fogColorGreen = this.fogColorGreen * (1.0F - f4) + afloat[1] * f4;
					this.fogColorBlue = this.fogColorBlue * (1.0F - f4) + afloat[2] * f4;
				}
			}
		}

		this.fogColorRed += (f1 - this.fogColorRed) * f;
		this.fogColorGreen += (f2 - this.fogColorGreen) * f;
		this.fogColorBlue += (f3 - this.fogColorBlue) * f;
		float f10 = worldclient.getRainStrength(partialTicks);

		if (f10 > 0.0F) {
			float f5 = 1.0F - f10 * 0.5F;
			float f12 = 1.0F - f10 * 0.4F;
			this.fogColorRed *= f5;
			this.fogColorGreen *= f5;
			this.fogColorBlue *= f12;
		}

		float f11 = worldclient.getThunderStrength(partialTicks);

		if (f11 > 0.0F) {
			float f13 = 1.0F - f11 * 0.5F;
			this.fogColorRed *= f13;
			this.fogColorGreen *= f13;
			this.fogColorBlue *= f13;
		}

		Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);

		if (this.cloudFog) {
			Vec3d vec33D = worldclient.getCloudColour(partialTicks);
			this.fogColorRed = (float) vec33D.xCoord;
			this.fogColorGreen = (float) vec33D.yCoord;
			this.fogColorBlue = (float) vec33D.zCoord;
		} else if (block.getMaterial() == Material.water) {
			float f8 = (float) EnchantmentHelper.getRespiration(entity) * 0.2F;

			if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.waterBreathing)) {
				f8 = f8 * 0.3F + 0.6F;
			}

			this.fogColorRed = 0.02F + f8;
			this.fogColorGreen = 0.02F + f8;
			this.fogColorBlue = 0.2F + f8;
			Vec3d vec34D = CustomColors.getUnderwaterColor(this.mc.theWorld, this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().posY + 1.0D, this.mc.getRenderViewEntity().posZ);

			if (vec34D != null) {
				this.fogColorRed = (float) vec34D.xCoord;
				this.fogColorGreen = (float) vec34D.yCoord;
				this.fogColorBlue = (float) vec34D.zCoord;
			}
		} else if (block.getMaterial() == Material.lava) {
			this.fogColorRed = 0.6F;
			this.fogColorGreen = 0.1F;
			this.fogColorBlue = 0.0F;
		}

		float f9 = this.fogColor2 + (this.fogColor1 - this.fogColor2) * partialTicks;
		this.fogColorRed *= f9;
		this.fogColorGreen *= f9;
		this.fogColorBlue *= f9;
		double d2 = worldclient.provider.getVoidFogYFactor();
		double d1 = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks) * d2;

		if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.blindness)) {
			int i = ((EntityLivingBase) entity).getActivePotionEffect(Potion.blindness).getDuration();

			if (i < 20) {
				d1 *= (double) (1.0F - (float) i / 20.0F);
			} else {
				d1 = 0.0D;
			}
		}

		if (d1 < 1.0D) {
			if (d1 < 0.0D) {
				d1 = 0.0D;
			}

			d1 = d1 * d1;
			this.fogColorRed = (float) ((double) this.fogColorRed * d1);
			this.fogColorGreen = (float) ((double) this.fogColorGreen * d1);
			this.fogColorBlue = (float) ((double) this.fogColorBlue * d1);
		}

		if (this.bossColorModifier > 0.0F) {
			float f14 = this.bossColorModifierPrev + (this.bossColorModifier - this.bossColorModifierPrev) * partialTicks;
			this.fogColorRed = this.fogColorRed * (1.0F - f14) + this.fogColorRed * 0.7F * f14;
			this.fogColorGreen = this.fogColorGreen * (1.0F - f14) + this.fogColorGreen * 0.6F * f14;
			this.fogColorBlue = this.fogColorBlue * (1.0F - f14) + this.fogColorBlue * 0.6F * f14;
		}

		if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.nightVision)) {
			float f15 = this.getNightVisionBrightness((EntityLivingBase) entity, partialTicks);
			float f6 = 1.0F / this.fogColorRed;

			if (f6 > 1.0F / this.fogColorGreen) {
				f6 = 1.0F / this.fogColorGreen;
			}

			if (f6 > 1.0F / this.fogColorBlue) {
				f6 = 1.0F / this.fogColorBlue;
			}

			this.fogColorRed = this.fogColorRed * (1.0F - f15) + this.fogColorRed * f6 * f15;
			this.fogColorGreen = this.fogColorGreen * (1.0F - f15) + this.fogColorGreen * f6 * f15;
			this.fogColorBlue = this.fogColorBlue * (1.0F - f15) + this.fogColorBlue * f6 * f15;
		}

		//		if (this.mc.gameSettings.anaglyph) {
		//			float f16 = (this.fogColorRed * 30.0F + this.fogColorGreen * 59.0F + this.fogColorBlue * 11.0F) / 100.0F;
		//			float f17 = (this.fogColorRed * 30.0F + this.fogColorGreen * 70.0F) / 100.0F;
		//			float f7 = (this.fogColorRed * 30.0F + this.fogColorBlue * 70.0F) / 100.0F;
		//			this.fogColorRed = f16;
		//			this.fogColorGreen = f17;
		//			this.fogColorBlue = f7;
		//		}

		Shaders.setClearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
	}

	/**
	 * Sets up the fog to be rendered. If the arg passed in is -1 the fog starts at 0 and goes to 80% of far plane
	 * distance and is used for sky rendering.
	 */
	private void setupFog(int p_78468_1_, float partialTicks) {
		Entity entity = this.mc.getRenderViewEntity();
		this.fogStandard = false;

		GL11.glFog(GL11.GL_FOG_COLOR, this.setFogColorBuffer(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 1.0F));
		GL11.glNormal3f(0.0F, -1.0F, 0.0F);
		G.color(1.0F, 1.0F, 1.0F, 1.0F);
		Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, entity, partialTicks);
		float f1 = -1.0F;

		if (f1 >= 0.0F) {
			G.setFogDensity(f1);
		} else if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.blindness)) {
			float f2 = 5.0F;
			int i = ((EntityLivingBase) entity).getActivePotionEffect(Potion.blindness).getDuration();

			if (i < 20) {
				f2 = 5.0F + (this.farPlaneDistance - 5.0F) * (1.0F - (float) i / 20.0F);
			}

			if (Config.isShaders()) {
				Shaders.setFog(9729);
			} else {
				G.setFog(9729);
			}

			if (p_78468_1_ == -1) {
				G.setFogStart(0.0F);
				G.setFogEnd(f2 * 0.8F);
			} else {
				G.setFogStart(f2 * 0.25F);
				G.setFogEnd(f2);
			}

			if (GLContext.getCapabilities().GL_NV_fog_distance && Config.isFogFancy()) {
				GL11.glFogi(34138, 34139);
			}
		} else if (this.cloudFog) {
			if (Config.isShaders()) {
				Shaders.setFog(2048);
			} else {
				G.setFog(2048);
			}

			G.setFogDensity(0.1F);
		} else if (block.getMaterial() == Material.water) {
			if (Config.isShaders()) {
				Shaders.setFog(2048);
			} else {
				G.setFog(2048);
			}

			if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(Potion.waterBreathing)) {
				G.setFogDensity(0.01F);
			} else {
				G.setFogDensity(0.1F - (float) EnchantmentHelper.getRespiration(entity) * 0.03F);
			}

			if (Config.isClearWater()) {
				G.setFogDensity(0.02F);
			}
		} else if (block.getMaterial() == Material.lava) {
			if (Config.isShaders()) {
				Shaders.setFog(2048);
			} else {
				G.setFog(2048);
			}

			G.setFogDensity(2.0F);
		} else {
			float f = this.farPlaneDistance;
			this.fogStandard = true;

			if (Config.isShaders()) {
				Shaders.setFog(9729);
			} else {
				G.setFog(9729);
			}

			if (p_78468_1_ == -1) {
				G.setFogStart(0.0F);
				G.setFogEnd(f);
			} else {
				G.setFogStart(f * Config.getFogStart());
				G.setFogEnd(f);
			}

			if (GLContext.getCapabilities().GL_NV_fog_distance) {
				if (Config.isFogFancy()) {
					GL11.glFogi(34138, 34139);
				}

				if (Config.isFogFast()) {
					GL11.glFogi(34138, 34140);
				}
			}

			if (this.mc.theWorld.provider.doesXZShowFog((int) entity.posX, (int) entity.posZ)) {
				G.setFogStart(f * 0.05F);
				G.setFogEnd(f);
			}

		}

		G.enableColorMaterial();
		G.enableFog();
		G.colorMaterial(1028, 4608);
	}

	/**
	 * Update and return fogColorBuffer with the RGBA values passed as arguments
	 */
	private FloatBuffer setFogColorBuffer(float red, float green, float blue, float alpha) {
		if (Config.isShaders()) {
			Shaders.setFogColor(red, green, blue);
		}

		this.fogColorBuffer.clear();
		this.fogColorBuffer.put(red).put(green).put(blue).put(alpha);
		this.fogColorBuffer.flip();
		return this.fogColorBuffer;
	}

	public MapItemRenderer getMapItemRenderer() {
		return this.theMapItemRenderer;
	}

	private void waitForServerThread() {
		this.serverWaitTimeCurrent = 0;

		if (Config.isSmoothWorld() && Config.isSingleProcessor()) {
			if (this.mc.isIntegratedServerRunning()) {
				IntegratedServer integratedserver = this.mc.getIntegratedServer();

				if (integratedserver != null) {
					boolean flag = this.mc.isGamePaused();

					if (!flag && !(this.mc.currentScreen instanceof GuiDownloadTerrain)) {
						if (this.serverWaitTime > 0) {
							Lagometer.timerServer.start();
							Config.sleep((long) this.serverWaitTime);
							Lagometer.timerServer.end();
							this.serverWaitTimeCurrent = this.serverWaitTime;
						}

						long i = System.nanoTime() / 1000000L;

						if (this.lastServerTime != 0L && this.lastServerTicks != 0) {
							long j = i - this.lastServerTime;

							if (j < 0L) {
								this.lastServerTime = i;
								j = 0L;
							}

							if (j >= 50L) {
								this.lastServerTime = i;
								int k = integratedserver.getTickCounter();
								int l = k - this.lastServerTicks;

								if (l < 0) {
									this.lastServerTicks = k;
									l = 0;
								}

								if (l < 1 && this.serverWaitTime < 100) {
									this.serverWaitTime += 2;
								}

								if (l > 1 && this.serverWaitTime > 0) {
									--this.serverWaitTime;
								}

								this.lastServerTicks = k;
							}
						} else {
							this.lastServerTime = i;
							this.lastServerTicks = integratedserver.getTickCounter();
							this.avgServerTickDiff = 1.0F;
							this.avgServerTimeDiff = 50.0F;
						}
					} else {
						if (this.mc.currentScreen instanceof GuiDownloadTerrain) {
							Config.sleep(20L);
						}

						this.lastServerTime = 0L;
						this.lastServerTicks = 0;
					}
				}
			}
		} else {
			this.lastServerTime = 0L;
			this.lastServerTicks = 0;
		}
	}

	private void frameInit() {
		if (!this.initialized) {
			TextureUtils.registerResourceListener();

			if (Config.getBitsOs() == 64 && Config.getBitsJre() == 32) {
				Config.setNotify64BitJava(true);
			}

			this.initialized = true;
		}

		Config.checkDisplayMode();
		World world = this.mc.theWorld;

		if (world != null) {
			if (Config.getNewRelease() != null) {
				String s = "HD_U".replace("HD_U", "HD Ultra").replace("L", "Light");
				String s1 = s + " " + Config.getNewRelease();
				ChatComponentText chatcomponenttext = new ChatComponentText(Lang.format("of.message.newVersion", s1));
				this.mc.ingameGUI.getChatGUI().printChatMessage(chatcomponenttext);
				Config.setNewRelease(null);
			}

			if (Config.isNotify64BitJava()) {
				Config.setNotify64BitJava(false);
				ChatComponentText chatcomponenttext1 = new ChatComponentText(Lang.format("of.message.java64Bit"));
				this.mc.ingameGUI.getChatGUI().printChatMessage(chatcomponenttext1);
			}
		}

		if (this.mc.currentScreen instanceof GuiMainMenu) {
			this.updateMainMenu((GuiMainMenu) this.mc.currentScreen);
		}

		if (this.updatedWorld != world) {
			RandomMobs.worldChanged(this.updatedWorld, world);
			Config.updateThreadPriorities();
			this.lastServerTime = 0L;
			this.lastServerTicks = 0;
			this.updatedWorld = world;
		}

		if (!this.setFxaaShader(Shaders.configAntialiasingLevel)) {
			Shaders.configAntialiasingLevel = 0;
		}
	}

	private void frameFinish() {
		if (this.mc.theWorld != null) {
			long i = System.currentTimeMillis();

			if (i > this.lastErrorCheckTimeMs + 10000L) {
				this.lastErrorCheckTimeMs = i;
				int j = GL11.glGetError();

				if (j != 0) {
					String s = GLU.gluErrorString(j);
					ChatComponentText chatcomponenttext = new ChatComponentText(Lang.format("of.message.openglError", j, s));
					this.mc.ingameGUI.getChatGUI().printChatMessage(chatcomponenttext);
				}
			}
		}
	}

	private void updateMainMenu(GuiMainMenu p_updateMainMenu_1_) {
		try {
			String birthday, comment;
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(new Date());
			int day = calendar.get(Calendar.DATE);
			int month = calendar.get(Calendar.MONTH) + 1;

			if (day == 8 && month == 4) {
				// Восьмое апреля - день рождения оптифайна
				birthday = "OptiFine";
				comment = "Поздравим гениальную штуковину!";
			} else if (day == 14 && month == 8) {
				// Четырнадцатое августа - день рождения создателя оптифайна
				birthday = "sp614x";
				comment = "Поднимем бокалы за создателя OptiFine!";
			} else if (day == 11 && month == 9) {
				birthday = "xtrafrancyz";
				comment = "Слава бессмертному гениальному божеству!";
			} else if (day == 17 && month == 11) {
				birthday = "DelfikPro";
				comment = "Не болей, делфик!";
			} else if (day == 1 && month == 6) {
				birthday = "Notch";
				comment = "Вспомним великого бога и создателя игры!";
			} else if (day == 7 && month == 6) {
				birthday = "вкусной жирной свиньи";
				comment = "С каждым годом она жиреет всё больше";
			} else if (day == 19 && month == 9) {
				birthday = "6oogle";
				comment = "В этот день надо торжественно сделать 42 удара в бубен";
			} else {
				birthday = null;
				comment = null;
			}

			GuiMainMenu.birthday = birthday;
			GuiMainMenu.birthdayComment = comment;

		} catch (Throwable ignored) {}
	}

	public boolean setFxaaShader(int p_setFxaaShader_1_) {
		if (!OpenGlHelper.isFramebufferEnabled()) {
			return false;
		}
		if (this.theShaderGroup != null && this.theShaderGroup != this.fxaaShaders[2] && this.theShaderGroup != this.fxaaShaders[4]) {
			return true;
		}
		if (p_setFxaaShader_1_ != 2 && p_setFxaaShader_1_ != 4) {
			if (this.theShaderGroup == null) {
				return true;
			}
			this.theShaderGroup.deleteShaderGroup();
			this.theShaderGroup = null;
			return true;
		}
		if (this.theShaderGroup != null && this.theShaderGroup == this.fxaaShaders[p_setFxaaShader_1_]) {
			return true;
		}
		if (this.mc.theWorld == null) {
			return true;
		}
		this.loadShader(new ResourceLocation("shaders/post/fxaa_of_" + p_setFxaaShader_1_ + "x.json"));
		this.fxaaShaders[p_setFxaaShader_1_] = this.theShaderGroup;
		return this.useShader;
	}

	private static class MouseFilter {
		private float field_76336_a;
		private float field_76334_b;
		private float field_76335_c;

		/**
		 * Smooths mouse input
		 */
		public float smooth(float p_76333_1_, float p_76333_2_) {
			this.field_76336_a += p_76333_1_;
			p_76333_1_ = (this.field_76336_a - this.field_76334_b) * p_76333_2_;
			this.field_76335_c += (p_76333_1_ - this.field_76335_c) * 0.5F;

			if (p_76333_1_ > 0.0F && p_76333_1_ > this.field_76335_c || p_76333_1_ < 0.0F && p_76333_1_ < this.field_76335_c) {
				p_76333_1_ = this.field_76335_c;
			}

			this.field_76334_b += p_76333_1_;
			return p_76333_1_;
		}

		public void reset() {
			this.field_76336_a = 0.0F;
			this.field_76334_b = 0.0F;
			this.field_76335_c = 0.0F;
		}

	}
}

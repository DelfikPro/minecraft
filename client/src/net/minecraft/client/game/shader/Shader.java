package net.minecraft.client.game.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.G;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.JsonException;
import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Shader {

	public final Framebuffer framebufferIn;
	public final Framebuffer framebufferOut;
	private final ShaderManager manager;
	private final List<Object> listAuxFramebuffers = new ArrayList<>();
	private final List<String> listAuxNames = new ArrayList<>();
	private final List<Integer> listAuxWidths = new ArrayList<>();
	private final List<Integer> listAuxHeights = new ArrayList<>();
	private Matrix4f projectionMatrix;

	public Shader(IResourceManager p_i45089_1_, String p_i45089_2_, Framebuffer p_i45089_3_, Framebuffer p_i45089_4_) throws JsonException, IOException {
		this.manager = new ShaderManager(p_i45089_1_, p_i45089_2_);
		this.framebufferIn = p_i45089_3_;
		this.framebufferOut = p_i45089_4_;
	}

	public void deleteShader() {
		this.manager.deleteShader();
	}

	public void addAuxFramebuffer(String p_148041_1_, Object p_148041_2_, int p_148041_3_, int p_148041_4_) {
		this.listAuxNames.add(this.listAuxNames.size(), p_148041_1_);
		this.listAuxFramebuffers.add(this.listAuxFramebuffers.size(), p_148041_2_);
		this.listAuxWidths.add(this.listAuxWidths.size(), p_148041_3_);
		this.listAuxHeights.add(this.listAuxHeights.size(), p_148041_4_);
	}

	private void preLoadShader() {
		G.color(1.0F, 1.0F, 1.0F, 1.0F);
		G.disableBlend();
		G.disableDepth();
		G.disableAlpha();
		G.disableFog();
		G.disableLighting();
		G.disableColorMaterial();
		G.enableTexture2D();
		G.bindTexture(0);
	}

	public void setProjectionMatrix(Matrix4f p_148045_1_) {
		this.projectionMatrix = p_148045_1_;
	}

	public void loadShader(float p_148042_1_) {
		this.preLoadShader();
		this.framebufferIn.unbindFramebuffer();
		float f = (float) this.framebufferOut.framebufferTextureWidth;
		float f1 = (float) this.framebufferOut.framebufferTextureHeight;
		G.viewport(0, 0, (int) f, (int) f1);
		this.manager.addSamplerTexture("DiffuseSampler", this.framebufferIn);

		for (int i = 0; i < this.listAuxFramebuffers.size(); ++i) {
			this.manager.addSamplerTexture((String) this.listAuxNames.get(i), this.listAuxFramebuffers.get(i));
			this.manager.getShaderUniformOrDefault("AuxSize" + i).set((float) ((Integer) this.listAuxWidths.get(i)).intValue(), (float) ((Integer) this.listAuxHeights.get(i)).intValue());
		}

		this.manager.getShaderUniformOrDefault("ProjMat").set(this.projectionMatrix);
		this.manager.getShaderUniformOrDefault("InSize").set((float) this.framebufferIn.framebufferTextureWidth, (float) this.framebufferIn.framebufferTextureHeight);
		this.manager.getShaderUniformOrDefault("OutSize").set(f, f1);
		this.manager.getShaderUniformOrDefault("Time").set(p_148042_1_);
		Minecraft minecraft = Minecraft.getMinecraft();
		this.manager.getShaderUniformOrDefault("ScreenSize").set((float) minecraft.displayWidth, (float) minecraft.displayHeight);
		this.manager.useShader();
		this.framebufferOut.framebufferClear();
		this.framebufferOut.bindFramebuffer(false);
		G.depthMask(false);
		G.colorMask(true, true, true, true);
		Tessellator tessellator = Tessellator.getInstance();
		WorldRenderer worldrenderer = tessellator.getWorldRenderer();
		worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
		worldrenderer.pos(0.0D, (double) f1, 500.0D).color(255, 255, 255, 255).endVertex();
		worldrenderer.pos((double) f, (double) f1, 500.0D).color(255, 255, 255, 255).endVertex();
		worldrenderer.pos((double) f, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
		worldrenderer.pos(0.0D, 0.0D, 500.0D).color(255, 255, 255, 255).endVertex();
		tessellator.draw();
		G.depthMask(true);
		G.colorMask(true, true, true, true);
		this.manager.endShader();
		this.framebufferOut.unbindFramebuffer();
		this.framebufferIn.unbindFramebufferTexture();

		for (Object object : this.listAuxFramebuffers) {
			if (object instanceof Framebuffer) {
				((Framebuffer) object).unbindFramebufferTexture();
			}
		}
	}

	public ShaderManager getShaderManager() {
		return this.manager;
	}

}

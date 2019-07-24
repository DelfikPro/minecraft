package net.minecraft.client.gui.achievement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.G;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.Lang;
import net.minecraft.stats.Achievement;
import net.minecraft.util.ResourceLocation;

public class GuiAchievement extends Gui {

	private static final ResourceLocation achievementBg = new ResourceLocation("textures/gui/achievement/achievement_background.png");
	private Minecraft mc;
	private int width;
	private int height;
	private String achievementTitle;
	private String achievementDescription;
	private Achievement theAchievement;
	private long notificationTime;
	private RenderItem renderItem;
	private boolean permanentNotification;

	public GuiAchievement(Minecraft mc) {
		this.mc = mc;
		this.renderItem = mc.getRenderItem();
	}

	public void displayAchievement(Achievement ach) {
		this.achievementTitle = Lang.format("achievement.get", new Object[0]);
		this.achievementDescription = ach.getStatName().getUnformattedText();
		this.notificationTime = Minecraft.getSystemTime();
		this.theAchievement = ach;
		this.permanentNotification = false;
	}

	public void displayUnformattedAchievement(Achievement achievementIn) {
		this.achievementTitle = achievementIn.getStatName().getUnformattedText();
		this.achievementDescription = achievementIn.getDescription();
		this.notificationTime = Minecraft.getSystemTime() + 2500L;
		this.theAchievement = achievementIn;
		this.permanentNotification = true;
	}

	private void updateAchievementWindowScale() {
		G.viewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
		G.matrixMode(5889);
		G.loadIdentity();
		G.matrixMode(5888);
		G.loadIdentity();
		this.width = this.mc.displayWidth;
		this.height = this.mc.displayHeight;
		ScaledResolution scaledresolution = new ScaledResolution(this.mc);
		this.width = scaledresolution.getScaledWidth();
		this.height = scaledresolution.getScaledHeight();
		G.clear(256);
		G.matrixMode(5889);
		G.loadIdentity();
		G.ortho(0.0D, (double) this.width, (double) this.height, 0.0D, 1000.0D, 3000.0D);
		G.matrixMode(5888);
		G.loadIdentity();
		G.translate(0.0F, 0.0F, -2000.0F);
	}

	public void updateAchievementWindow() {
		if (this.theAchievement != null && this.notificationTime != 0L && Minecraft.getMinecraft().thePlayer != null) {
			double d0 = (double) (Minecraft.getSystemTime() - this.notificationTime) / 3000.0D;

			if (!this.permanentNotification) {
				if (d0 < 0.0D || d0 > 1.0D) {
					this.notificationTime = 0L;
					return;
				}
			} else if (d0 > 0.5D) {
				d0 = 0.5D;
			}

			this.updateAchievementWindowScale();
			G.disableDepth();
			G.depthMask(false);
			double d1 = d0 * 2.0D;

			if (d1 > 1.0D) {
				d1 = 2.0D - d1;
			}

			d1 = d1 * 4.0D;
			d1 = 1.0D - d1;

			if (d1 < 0.0D) {
				d1 = 0.0D;
			}

			d1 = d1 * d1;
			d1 = d1 * d1;
			int i = this.width - 160;
			int j = 0 - (int) (d1 * 36.0D);
			G.color(1.0F, 1.0F, 1.0F, 1.0F);
			G.enableTexture2D();
			this.mc.getTextureManager().bindTexture(achievementBg);
			G.disableLighting();
			this.drawTexturedModalRect(i, j, 96, 202, 160, 32);

			if (this.permanentNotification) {
				this.mc.fontRenderer.drawSplitString(this.achievementDescription, i + 30, j + 7, 120, -1);
			} else {
				this.mc.fontRenderer.drawString(this.achievementTitle, i + 30, j + 7, -256);
				this.mc.fontRenderer.drawString(this.achievementDescription, i + 30, j + 18, -1);
			}

			RenderHelper.enableGUIStandardItemLighting();
			G.disableLighting();
			G.enableRescaleNormal();
			G.enableColorMaterial();
			G.enableLighting();
			this.renderItem.renderItemAndEffectIntoGUI(this.theAchievement.theItemStack, i + 8, j + 8);
			G.disableLighting();
			G.depthMask(true);
			G.enableDepth();
		}
	}

	public void clearAchievements() {
		this.theAchievement = null;
		this.notificationTime = 0L;
	}

}

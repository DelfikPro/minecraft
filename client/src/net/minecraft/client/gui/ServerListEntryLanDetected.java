package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.LanServerDetector;
import net.minecraft.client.resources.Lang;
import net.minecraft.client.settings.Settings;

public class ServerListEntryLanDetected implements GuiListExtended.IGuiListEntry {

	protected final Minecraft mc;
	protected final LanServerDetector.LanServer field_148291_b;
	private final GuiMultiplayer field_148292_c;
	private long field_148290_d = 0L;

	protected ServerListEntryLanDetected(GuiMultiplayer p_i45046_1_, LanServerDetector.LanServer p_i45046_2_) {
		this.field_148292_c = p_i45046_1_;
		this.field_148291_b = p_i45046_2_;
		this.mc = Minecraft.getMinecraft();
	}

	public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
		this.mc.fontRenderer.drawString(Lang.format("lanServer.title"), x + 32 + 3, y + 1, 16777215);
		this.mc.fontRenderer.drawString(this.field_148291_b.getServerMotd(), x + 32 + 3, y + 12, 8421504);

		if (Settings.HIDE_SERVER_ADDRESS.b()) {
			this.mc.fontRenderer.drawString(Lang.format("selectServer.hiddenAddress"), x + 32 + 3, y + 12 + 11, 3158064);
		} else {
			this.mc.fontRenderer.drawString(this.field_148291_b.getServerIpPort(), x + 32 + 3, y + 12 + 11, 3158064);
		}
	}

	/**
	 * Returns true if the mouse has been pressed on this control.
	 */
	public boolean mousePressed(int slotIndex, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
		this.field_148292_c.selectServer(slotIndex);

		if (Minecraft.getSystemTime() - this.field_148290_d < 250L) {
			this.field_148292_c.connectToSelected();
		}

		this.field_148290_d = Minecraft.getSystemTime();
		return false;
	}

	public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
	}

	/**
	 * Fired when the mouse button is released. Arguments: index, x, y, mouseEvent, relativeX, relativeY
	 */
	public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
	}

	public LanServerDetector.LanServer getLanServer() {
		return this.field_148291_b;
	}

}

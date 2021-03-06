package net.minecraft.client.gui.spectator;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.game.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.G;
import net.minecraft.network.protocol.minecraft_47.play.client.C18PacketSpectate;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

public class PlayerMenuObject implements ISpectatorMenuObject {

	private final GameProfile profile;
	private final ResourceLocation resourceLocation;

	public PlayerMenuObject(GameProfile profileIn) {
		this.profile = profileIn;
		this.resourceLocation = AbstractClientPlayer.getLocationSkin(profileIn.getName());
		AbstractClientPlayer.getDownloadImageSkin(this.resourceLocation, profileIn.getName());
	}

	public void func_178661_a(SpectatorMenu menu) {
		Minecraft.getMinecraft().getNetHandler().addToSendQueue(new C18PacketSpectate(this.profile.getId()));
	}

	public IChatComponent getSpectatorName() {
		return new ChatComponentText(this.profile.getName());
	}

	public void render(float p_178663_1_, int alpha) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(this.resourceLocation);
		G.color(1.0F, 1.0F, 1.0F, (float) alpha / 255.0F);
		Gui.drawScaledCustomSizeModalRect(2, 2, 8.0F, 8.0F, 8, 8, 12, 12, 64.0F, 64.0F);
		Gui.drawScaledCustomSizeModalRect(2, 2, 40.0F, 8.0F, 8, 8, 12, 12, 64.0F, 64.0F);
	}

	public boolean func_178662_A_() {
		return true;
	}

}

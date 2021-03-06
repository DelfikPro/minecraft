package net.minecraft.client.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import net.minecraft.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.protocol.IProtocolsClient;
import net.minecraft.client.network.protocol.implario.ProtocolImplarioClient;
import net.minecraft.client.network.protocol.minecraft_47.NetHandlerPlayClient;
import net.minecraft.client.network.protocol.minecraft_47.Protocol47Client;
import net.minecraft.logging.Log;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.implario.login.INetHandlerLoginClientImplario;
import net.minecraft.network.protocol.implario.login.client.C02PacketClientInfo;
import net.minecraft.network.protocol.implario.login.server.S04PacketServerInfo;
import net.minecraft.network.protocol.minecraft_47.Protocol47;
import net.minecraft.network.protocol.minecraft.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.protocol.minecraft.login.server.S00PacketDisconnect;
import net.minecraft.network.protocol.minecraft.login.server.S01PacketEncryptionRequest;
import net.minecraft.network.protocol.minecraft.login.server.S02PacketLoginSuccess;
import net.minecraft.network.protocol.minecraft.login.server.S03PacketEnableCompression;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.util.CryptManager;
import net.minecraft.util.IChatComponent;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.PublicKey;

public class NetHandlerLoginClient implements INetHandlerLoginClientImplario {

	private static final Log logger = Log.MAIN;
	private final Minecraft mc;
	private final GuiScreen previousGuiScreen;
	private final NetworkManager networkManager;
	private GameProfile gameProfile;

	public NetHandlerLoginClient(NetworkManager p_i45059_1_, Minecraft mcIn, GuiScreen p_i45059_3_) {
		this.networkManager = p_i45059_1_;
		this.mc = mcIn;
		this.previousGuiScreen = p_i45059_3_;
		networkManager.setProtocol(Protocol47Client.protocol);
	}

	public void handleEncryptionRequest(S01PacketEncryptionRequest packetIn) {
		final SecretKey secretkey = CryptManager.createNewSharedKey();
		String s = packetIn.getServerId();
		PublicKey publickey = packetIn.getPublicKey();
		String s1 = new BigInteger(CryptManager.getServerIdHash(s, publickey, secretkey)).toString(16);

		if (this.mc.getCurrentServerData() != null && this.mc.getCurrentServerData().isLAN()) {
			try {
				this.getSessionService().joinServer(this.mc.getSession().getProfile(), this.mc.getSession().getToken(), s1);
			} catch (AuthenticationException var10) {
				logger.warn("Couldn\'t connect to auth servers but will continue to join LAN");
			}
		} else {
			try {
				this.getSessionService().joinServer(this.mc.getSession().getProfile(), this.mc.getSession().getToken(), s1);
			} catch (AuthenticationUnavailableException var7) {
				this.networkManager.closeChannel(
						new ChatComponentTranslation("disconnect.loginFailedInfo", new ChatComponentTranslation("disconnect.loginFailedInfo.serversUnavailable")));
				return;
			} catch (InvalidCredentialsException var8) {
				this.networkManager.closeChannel(
						new ChatComponentTranslation("disconnect.loginFailedInfo", new ChatComponentTranslation("disconnect.loginFailedInfo.invalidSession")));
				return;
			} catch (AuthenticationException authenticationexception) {
				this.networkManager.closeChannel(new ChatComponentTranslation("disconnect.loginFailedInfo", authenticationexception.getMessage()));
				return;
			}
		}

		this.networkManager.sendPacket(new C01PacketEncryptionResponse(secretkey, publickey, packetIn.getVerifyToken()),
				p_operationComplete_1_ -> NetHandlerLoginClient.this.networkManager.enableEncryption(secretkey));
	}

	private MinecraftSessionService getSessionService() {
		return this.mc.getSessionService();
	}

	@Override
	public void handleLoginSuccess(S02PacketLoginSuccess packetIn) {
		gameProfile = packetIn.getProfile();
		networkManager.setConnectionState(networkManager.getProtocol().getProtocolPlay());
		networkManager.setNetHandler(((IProtocolsClient)networkManager.getProtocol()).getPlayClient(mc, previousGuiScreen, networkManager, gameProfile));
	}

	/**
	 * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
	 */
	public void onDisconnect(IChatComponent reason) {
		this.mc.displayGuiScreen(new GuiDisconnected(this.previousGuiScreen, "connect.denied", reason));
	}

	public void handleDisconnect(S00PacketDisconnect packetIn) {
		this.networkManager.closeChannel(packetIn.func_149603_c());
	}

	public void handleEnableCompression(S03PacketEnableCompression packetIn) {
		if (!this.networkManager.isLocalChannel()) {
			this.networkManager.setCompressionTreshold(packetIn.getCompressionTreshold());
		}
	}

	@Override
	public void processServerInfo(S04PacketServerInfo serverInfo) {
		Utils.implarioServer = true;
		networkManager.setProtocol(ProtocolImplarioClient.protocol);
		networkManager.sendPacket(new C02PacketClientInfo());
	}
}

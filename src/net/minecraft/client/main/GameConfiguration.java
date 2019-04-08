package net.minecraft.client.main;

import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.util.Session;

import java.io.File;
import java.net.Proxy;

public class GameConfiguration {

	public final GameConfiguration.UserInformation userInfo;
	public final GameConfiguration.DisplayInformation displayInfo;
	public final GameConfiguration.FolderInformation folderInfo;
	public final GameConfiguration.GameInformation gameInfo;
	public final GameConfiguration.ServerInformation serverInfo;

	public GameConfiguration(GameConfiguration.UserInformation userInfoIn, GameConfiguration.DisplayInformation displayInfoIn, GameConfiguration.FolderInformation folderInfoIn,
							 GameConfiguration.GameInformation gameInfoIn, GameConfiguration.ServerInformation serverInfoIn) {
		this.userInfo = userInfoIn;
		this.displayInfo = displayInfoIn;
		this.folderInfo = folderInfoIn;
		this.gameInfo = gameInfoIn;
		this.serverInfo = serverInfoIn;
	}

	public static class DisplayInformation {

		public final int width;
		public final int height;
		public final boolean fullscreen;
		public final boolean checkGlErrors;

		public DisplayInformation(int widthIn, int heightIn, boolean fullscreenIn, boolean checkGlErrorsIn) {
			this.width = widthIn;
			this.height = heightIn;
			this.fullscreen = fullscreenIn;
			this.checkGlErrors = checkGlErrorsIn;
		}

	}

	public static class FolderInformation {

		public final File mcDataDir;
		public final File resourcePacksDir;
		public final File assetsDir;
		public final String assetIndex;

		public FolderInformation(File mcDataDirIn, File resourcePacksDirIn, File assetsDirIn, String assetIndexIn) {
			this.mcDataDir = mcDataDirIn;
			this.resourcePacksDir = resourcePacksDirIn;
			this.assetsDir = assetsDirIn;
			this.assetIndex = assetIndexIn;
		}

	}

	public static class GameInformation {

		public final String version;

		public GameInformation(String versionIn) {
			this.version = versionIn;
		}

	}

	public static class ServerInformation {

		public final String serverName;
		public final int serverPort;

		public ServerInformation(String serverNameIn, int serverPortIn) {
			this.serverName = serverNameIn;
			this.serverPort = serverPortIn;
		}

	}

	public static class UserInformation {

		public final Session session;
		public final PropertyMap field_181172_c;
		public final Proxy proxy;

		public UserInformation(Session session, PropertyMap properties, Proxy proxy) {
			this.session = session;
			this.field_181172_c = properties;
			this.proxy = proxy;
		}

	}

}
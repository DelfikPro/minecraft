package net.minecraft.server.dedicated;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommand;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.MPlayer;
import net.minecraft.entity.player.Player;
import net.minecraft.logging.Log;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.CryptManager;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static net.minecraft.logging.Log.MAIN;

public class DedicatedServer extends MinecraftServer {
	private final List<ServerCommand> pendingCommandList = Collections.synchronizedList(new ArrayList<>());
	private PropertyManager settings;
	private boolean canSpawnStructures;
	private WorldSettings.GameType gameType;

	public DedicatedServer(File workDir) {
		super(workDir, Proxy.NO_PROXY, USER_CACHE_FILE);
		Thread thread = new Thread("Server Infinisleeper") {
			{
				this.setDaemon(true);
				this.start();
			}
			public void run() {
				while (true) try {
					Thread.sleep(1, 1);
					Thread.sleep(0x7fffffffL);
				} catch (InterruptedException ignored) {}
			}
		};
	}

	protected boolean startServer() throws IOException {
		Thread thread = new Thread("Server console handler") {
			public void run() {
				BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(System.in));
				String s4;

				try {
					while (!DedicatedServer.this.isServerStopped() && DedicatedServer.this.isServerRunning() && (s4 = bufferedreader.readLine()) != null) {
						DedicatedServer.this.addPendingCommand(s4, DedicatedServer.this);
					}
				} catch (IOException e) {
					MAIN.error("Ошибка при обработке ввода из консоли", e);
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
		MAIN.info("Run server on ImplarioCore (Minecraft 1.8.8)...");

		MAIN.info("Загрузка свойств сервера...");
		this.settings = new PropertyManager(new File("server.properties"));

		if (this.isSinglePlayer()) this.setHostname("127.0.0.1");
		else {
			this.setOnlineMode(this.settings.getBooleanProperty("online-mode", false));
			this.setHostname(this.settings.getStringProperty("server-ip", ""));
		}

		this.setCanSpawnAnimals(this.settings.getBooleanProperty("spawn-animals", true));
		this.setCanSpawnNPCs(this.settings.getBooleanProperty("spawn-npcs", true));
		this.setAllowPvp(this.settings.getBooleanProperty("pvp", true));
		this.setAllowFlight(this.settings.getBooleanProperty("allow-flight", false));
		this.setResourcePack(this.settings.getStringProperty("resource-pack", ""), this.settings.getStringProperty("resource-pack-hash", ""));
		this.setMOTD(this.settings.getStringProperty("motd", "A Minecraft Server"));
		this.setPlayerIdleTimeout(this.settings.getIntProperty("player-idle-timeout", 0));

		if (this.settings.getIntProperty("difficulty", 1) < 0) {
			this.settings.setProperty("difficulty", 0);
		} else if (this.settings.getIntProperty("difficulty", 1) > 3) {
			this.settings.setProperty("difficulty", 3);
		}

		this.canSpawnStructures = this.settings.getBooleanProperty("generate-structures", true);
		int i = this.settings.getIntProperty("gamemode", WorldSettings.GameType.SURVIVAL.getID());
		this.gameType = WorldSettings.getGameTypeById(i);
		MAIN.info("Default gamemode: " + this.gameType);
		InetAddress inetaddress = null;

		if (this.getServerHostname().length() > 0) inetaddress = InetAddress.getByName(this.getServerHostname());
		if (this.getServerPort() < 0) this.setServerPort(this.settings.getIntProperty("server-port", 25565));

		MAIN.info("Генерирация ключи шифрования...");
		this.setKeyPair(CryptManager.generateKeyPair());
		String address = (this.getServerHostname().length() == 0 ? "*" : this.getServerHostname()) + ":" + this.getServerPort();
		MAIN.info("Создание слушателя адреса " + address);

		try {
			this.getNetworkSystem().addLanEndpoint(inetaddress, this.getServerPort());
		} catch (IOException ex) {
			if (ex instanceof BindException) {
				String msg = ex.getMessage();
				if (msg.contains("Cannot assign requested address: bind")) {
					MAIN.error("**** К адресу " + address + " отсутствует доступ!");
					return false;
				} else if (msg.contains("Address already in use: bind")) {
					MAIN.error("**** Порт " + getServerPort() + " занят!");
					return false;
				}
			}
			MAIN.error("**** При создании сетевого слушателя возникла ошибка!", ex);
			return false;
		}

		if (this.isServerInOnlineMode()) MAIN.warn("*** Сервер запущен в онлайн-режиме!");

		this.setConfigManager(new DedicatedPlayerList(this));
		long j = System.nanoTime();

		if (this.getFolderName() == null) this.setFolderName(this.settings.getStringProperty("level-name", "world"));

		String s = this.settings.getStringProperty("level-seed", "");
		String s1 = this.settings.getStringProperty("level-type", "DEFAULT");
		String s2 = this.settings.getStringProperty("generator-settings", "");
		long k = new Random().nextLong();

		if (s.length() > 0) {
			try {
				long l = Long.parseLong(s);
				if (l != 0L) k = l;
			} catch (NumberFormatException var16) {
				k = (long) s.hashCode();
			}
		}

		WorldType worldtype = WorldType.parseWorldType(s1);
		if (worldtype == null) worldtype = WorldType.VOID;

		this.isAnnouncingPlayerAchievements();
		this.isCommandBlockEnabled();
		this.getOpPermissionLevel();
		this.isSnooperEnabled();
		this.getNetworkCompressionTreshold();
		this.setBuildLimit(this.settings.getIntProperty("max-build-height", 256));
		this.setBuildLimit((this.getBuildLimit() + 8) / 16 * 16);
		this.setBuildLimit(MathHelper.clamp_int(this.getBuildLimit(), 64, 256));
		this.settings.setProperty("max-build-height", this.getBuildLimit());
		MAIN.info("[World] Инициализируем \"" + this.getFolderName() + "\"");
		this.loadAllWorlds(this.getFolderName(), this.getFolderName(), k, worldtype, s2);
		long i1 = System.nanoTime() - j;
		String s3 = String.format("%.3fs", (double) i1 / 1000000000);
		MAIN.info("Сервер запущен (" + s3 + ")!");

		if (this.getMaxTickTime() > 0L) {
			Thread thread1 = new Thread(new ServerHangWatchdog(this));
			thread1.setName("Server Watchdog");
			thread1.setDaemon(true);
			thread1.start();
		}

		return true;
	}

	public void setGameType(WorldSettings.GameType gameMode) {
		super.setGameType(gameMode);
		this.gameType = gameMode;
	}

	public boolean canStructuresSpawn() {
		return this.canSpawnStructures;
	}

	public WorldSettings.GameType getGameType() {
		return this.gameType;
	}

	/**
	 * Get the server's difficulty
	 */
	public EnumDifficulty getDifficulty() {
		return EnumDifficulty.getDifficultyEnum(this.settings.getIntProperty("difficulty", EnumDifficulty.NORMAL.getDifficultyId()));
	}

	/**
	 * Defaults to false.
	 */
	public boolean isHardcore() {
		return this.settings.getBooleanProperty("hardcore", false);
	}

	/**
	 * Called on exit from the main run() loop.
	 */
	protected void finalTick(CrashReport report) {
	}

	/**
	 * Adds the server info, including from theWorldServer, to the crash report.
	 */
	public CrashReport addServerInfoToCrashReport(CrashReport report) {
		report = super.addServerInfoToCrashReport(report);
		report.getCategory().addCrashSectionCallable("Type", () -> "Dedicated Server (map_server.txt)");
		return report;
	}

	/**
	 * Directly calls System.exit(0), instantly killing the program.
	 */
	protected void systemExitNow() {
		System.exit(0);
	}

	public void updateTimeLightAndEntities() {
		super.updateTimeLightAndEntities();
		this.executePendingCommands();
	}

	public boolean getAllowNether() {
		return this.settings.getBooleanProperty("allow-nether", true);
	}

	public boolean allowSpawnMonsters() {
		return this.settings.getBooleanProperty("spawn-monsters", true);
	}

	/**
	 * Returns whether snooping is enabled or not.
	 */
	public boolean isSnooperEnabled() {
		return this.settings.getBooleanProperty("snooper-enabled", true);
	}

	public void addPendingCommand(String input, ICommandSender sender) {
		this.pendingCommandList.add(new ServerCommand(input, sender));
	}

	public void executePendingCommands() {
		while (!this.pendingCommandList.isEmpty()) {
			ServerCommand servercommand = this.pendingCommandList.remove(0);
			this.getCommandManager().executeCommand(servercommand.sender, servercommand.command);
		}
	}

	public boolean isDedicatedServer() {
		return true;
	}

	public boolean useEpoll() {
		return this.settings.getBooleanProperty("use-native-transport", true);
	}

	public DedicatedPlayerList getConfigurationManager() {
		return (DedicatedPlayerList) super.getConfigurationManager();
	}

	/**
	 * Gets an integer property. If it does not exist, set it to the specified value.
	 */
	public int getIntProperty(String key, int defaultValue) {
		return this.settings.getIntProperty(key, defaultValue);
	}

	/**
	 * Gets a string property. If it does not exist, set it to the specified value.
	 */
	public String getStringProperty(String key, String defaultValue) {
		return this.settings.getStringProperty(key, defaultValue);
	}

	/**
	 * Gets a boolean property. If it does not exist, set it to the specified value.
	 */
	public boolean getBooleanProperty(String key, boolean defaultValue) {
		return this.settings.getBooleanProperty(key, defaultValue);
	}

	/**
	 * Saves an Object with the given property name.
	 */
	public void setProperty(String key, Object value) {
		this.settings.setProperty(key, value);
	}

	/**
	 * Saves all of the server properties to the properties file.
	 */
	public void saveProperties() {
		this.settings.saveProperties();
	}

	@Override
	public void sendMessage(IChatComponent component) {
		Log.CHAT.info(component.getUnformattedText().replaceAll("§.", ""));
	}

	/**
	 * Returns the filename where server properties are stored
	 */
	public String getSettingsFilename() {
		File file1 = this.settings.getPropertiesFile();
		return file1 != null ? file1.getAbsolutePath() : "No settings file";
	}

	/**
	 * On dedicated does nothing. On integrated, sets commandsAllowedForAll, gameType and allows external connections.
	 */
	public String shareToLAN(WorldSettings.GameType type, boolean allowCheats) {
		return "";
	}

	/**
	 * Return whether command blocks are enabled.
	 */
	public boolean isCommandBlockEnabled() {
		return this.settings.getBooleanProperty("enable-command-block", false);
	}

	/**
	 * Return the spawn protection area's size.
	 */
	public int getSpawnProtectionSize() {
		return this.settings.getIntProperty("spawn-protection", super.getSpawnProtectionSize());
	}

	public boolean isBlockProtected(World worldIn, BlockPos pos, Player playerIn) {
		if (worldIn.provider.getDimensionId() != 0) return false;
		if (this.getConfigurationManager().canSendCommands((MPlayer)playerIn)) return false;
		if (this.getSpawnProtectionSize() <= 0) return false;
		BlockPos blockpos = worldIn.getSpawnPoint();
		int i = MathHelper.abs_int(pos.getX() - blockpos.getX());
		int j = MathHelper.abs_int(pos.getZ() - blockpos.getZ());
		int k = Math.max(i, j);
		return k <= this.getSpawnProtectionSize();
	}

	public int getOpPermissionLevel() {
		return this.settings.getIntProperty("op-permission-level", 4);
	}

	public void setPlayerIdleTimeout(int idleTimeout) {
		super.setPlayerIdleTimeout(idleTimeout);
		this.settings.setProperty("player-idle-timeout", idleTimeout);
		this.saveProperties();
	}

	public boolean opsSeeConsole() {
		return this.settings.getBooleanProperty("broadcast-console-to-ops", true);
	}

	public boolean isAnnouncingPlayerAchievements() {
		return this.settings.getBooleanProperty("announce-player-achievements", true);
	}

	public int getMaxWorldSize() {
		int i = this.settings.getIntProperty("max-world-size", super.getMaxWorldSize());

		if (i < 1) i = 1;
		else if (i > super.getMaxWorldSize()) i = super.getMaxWorldSize();

		return i;
	}

	/**
	 * The compression treshold. If the packet is larger than the specified amount of bytes, it will be compressed
	 */
	public int getNetworkCompressionTreshold() {
		return this.settings.getIntProperty("network-compression-threshold", super.getNetworkCompressionTreshold());
	}

	public long getMaxTickTime() {
		return this.settings.getLongProperty("max-tick-time", TimeUnit.MINUTES.toMillis(1L));
	}

}

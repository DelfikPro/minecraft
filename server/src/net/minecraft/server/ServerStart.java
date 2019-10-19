package net.minecraft.server;

import net.minecraft.init.Bootstrap;
import net.minecraft.resources.Datapack;
import net.minecraft.resources.Datapacks;
import net.minecraft.resources.load.JarDatapackLoader;
import net.minecraft.resources.load.SimpleDatapackLoader;
import net.minecraft.security.MinecraftSecurityManager;
import net.minecraft.security.Restart;
import net.minecraft.server.dedicated.DedicatedServer;
import vanilla.Vanilla;

import java.io.File;

public class ServerStart {
	static{
		if(System.getSecurityManager() == null) System.setSecurityManager(new MinecraftSecurityManager());
	}

	public static void main(String[] args) {
		Restart.setArgs(args);
		String serverOwner = null;
		String workDir = ".";
		String worldName = null;
		boolean starterKit = false;
		int i = -1;

		for (int j = 0; j < args.length; ++j) {
			String s3 = args[j];
			String s4 = j == args.length - 1 ? null : args[j + 1];
			boolean wasArgumentUsed = false;

			if (s3.equals("--port") && s4 != null) {
				wasArgumentUsed = true;

				try {
					i = Integer.parseInt(s4);
				} catch (NumberFormatException ignored) {}
			} else if (s3.equals("--singleplayer") && s4 != null) {
				wasArgumentUsed = true;
				serverOwner = s4;
			} else if (s3.equals("--universe") && s4 != null) {
				wasArgumentUsed = true;
				workDir = s4;
			} else if (s3.equals("--world") && s4 != null) {
				wasArgumentUsed = true;
				worldName = s4;
			} else if (s3.equals("--bonusChest")) starterKit = true;

			if (wasArgumentUsed) ++j;
		}

		Datapacks.fullInitializeDatapacks(new File("datapacks"));

		final DedicatedServer dedicatedserver = new DedicatedServer(new File(workDir));

		if (serverOwner != null) dedicatedserver.setServerOwner(serverOwner);

		if (worldName != null) dedicatedserver.setFolderName(worldName);

		if (i >= 0) dedicatedserver.setServerPort(i);

		if (starterKit) dedicatedserver.canCreateBonusChest(true);

		dedicatedserver.startServerThread();
		Runtime.getRuntime().addShutdownHook(new Thread("Server Shutdown Thread") {
			public void run() {
				if (!dedicatedserver.isServerStopped())
				dedicatedserver.stopServer();
			}
		});
	}

}

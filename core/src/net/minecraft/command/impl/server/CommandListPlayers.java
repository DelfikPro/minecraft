package net.minecraft.command.impl.server;

import net.minecraft.command.impl.core.CommandBase;
import net.minecraft.command.impl.core.CommandException;
import net.minecraft.command.impl.core.CommandResultStats;
import net.minecraft.command.api.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.chat.ChatComponentText;
import net.minecraft.util.chat.ChatComponentTranslation;

public class CommandListPlayers extends CommandBase {

	/**
	 * Gets the name of the command
	 */
	public String getCommandName() {
		return "list";
	}

	/**
	 * Return the required permission level for this command.
	 */
	public int getRequiredPermissionLevel() {
		return 0;
	}

	/**
	 * Gets the usage string for the command.
	 */
	public String getCommandUsage(ICommandSender sender) {
		return "commands.players.usage";
	}

	/**
	 * Callback when the command is invoked
	 */
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		int i = MinecraftServer.getServer().getCurrentPlayerCount();
		sender.sendMessage(new ChatComponentTranslation("commands.players.list", i, MinecraftServer.getServer().getMaxPlayers()));
		sender.sendMessage(new ChatComponentText(MinecraftServer.getServer().getConfigurationManager().serializeOnlinePlayers(args.length > 0 && "uuids".equalsIgnoreCase(args[0]))));
		sender.setCommandStat(CommandResultStats.Type.QUERY_RESULT, i);
	}

}
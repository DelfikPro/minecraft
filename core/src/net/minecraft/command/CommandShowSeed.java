package net.minecraft.command;

import net.minecraft.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.chat.ChatComponentTranslation;
import net.minecraft.world.World;

public class CommandShowSeed extends CommandBase {

	/**
	 * Returns true if the given command sender is allowed to use this command.
	 */
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return MinecraftServer.getServer().isSinglePlayer() || super.canCommandSenderUseCommand(sender);
	}

	/**
	 * Gets the name of the command
	 */
	public String getCommandName() {
		return "seed";
	}

	/**
	 * Return the required permission level for this command.
	 */
	public int getRequiredPermissionLevel() {
		return 2;
	}

	/**
	 * Gets the usage string for the command.
	 */
	public String getCommandUsage(ICommandSender sender) {
		return "commands.seed.usage";
	}

	/**
	 * Callback when the command is invoked
	 */
	public void processCommand(ICommandSender sender, String[] args) throws CommandException {
		World world = (World) (sender instanceof Player ? ((Player) sender).worldObj : MinecraftServer.getServer().worldServerForDimension(0));
		sender.sendMessage(new ChatComponentTranslation("commands.seed.success", world.getSeed()));
	}

}
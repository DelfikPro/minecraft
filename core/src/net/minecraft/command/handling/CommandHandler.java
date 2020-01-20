package net.minecraft.command.handling;


import com.google.common.collect.Maps;
import lombok.Getter;
import net.minecraft.command.ICommand;
import net.minecraft.command.api.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.api.Command;
import net.minecraft.entity.player.MPlayer;
import net.minecraft.logging.Log;
import net.minecraft.util.BlockPos;
import net.minecraft.util.functional.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static net.minecraft.util.functional.ArrayUtils.*;

@Getter
public class CommandHandler implements ICommandManager {

	private final Map<String, Command> commands = Maps.newHashMap();

	@Override
	public int executeCommand(ICommandSender sender, String rawCommand) {
		rawCommand = rawCommand.trim();

		if (rawCommand.startsWith("/"))
			rawCommand = rawCommand.substring(1);

		String[] argsArray = rawCommand.split(" ");
		String commandName = argsArray[0];
		Command cmd = commands.get(commandName);

		if (cmd == null) {
			sender.sendMessage("§cКоманда §f/" + commandName + "§c не найдена.");
			return 0;
		}
		if (!sender.canCommandSenderUseCommand(cmd.getPermissionLevel(), cmd.getAddress())) {
			sender.sendMessage("§7/" + commandName + ": §fТребуется " + cmd.getPermissionLevel() + " уровень доступа " + cmd.getPermissionLadder());
			return 0;
		}

		argsArray = dropFirstArg(argsArray);

		try {
			return cmd.execute(sender, argsArray);
		} catch (Throwable t) {
			Log.MAIN.error("При выполнении команды произошла ошибка: " + rawCommand, t);
			sender.sendMessage("§cПри выполнении команды произошла ошибка: " + t.getMessage());
			return 0;
		}

	}

	public Command registerCommand(Command cmd) {
		commands.put(cmd.getAddress(), cmd);
		return cmd;
	}

	public void unregisterCommand(Command command) {
		commands.remove(command.getAddress());
	}

	public Command getCommand(String command) {
		return commands.get(command);
	}

	public Collection<String> getTabCompletionOptions(MPlayer player, String input, BlockPos pos) {
		String[] argsArray = input.split(" ", -1);
		String s = argsArray[0];

		if (argsArray.length == 1) {
			List<String> list = new ArrayList<>();

			for (Map.Entry<String, Command> entry : commands.entrySet()) {
				if (StringUtils.doesStringStartWith(s, entry.getKey()) && ((ICommand) entry.getValue()).canCommandSenderUseCommand(player)) {
					list.add(entry.getKey());
				}
			}

			return list;
		}
		if (argsArray.length > 1) {
			Command command = commands.get(s);
			argsArray = dropFirstArg(argsArray);

			return command.tabComplete(player, pos, argsArray);
		}

		return null;
	}

	public List<Command> getPossibleCommands(ICommandSender sender) {
		List<Command> list = new ArrayList<>();
		for (Command cmd : commands.values())
			if (sender.canCommandSenderUseCommand(cmd.getPermissionLevel(), cmd.getAddress()))
				list.add(cmd);

		return list;
	}

}


package net.minecraft.command.api.context.args;

import net.minecraft.command.api.context.ArgsParser;
import net.minecraft.entity.player.MPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArgPlayers extends ArgEntities<MPlayer> {

	public ArgPlayers(String caption, String description) {
		super(caption, description, MPlayer.class);
	}

	@Override
	public Collection<MPlayer> get(ArgsParser parser) {
		Collection<MPlayer> selector = super.get(parser);
		if (selector != null) return selector;

		String username = parser.lookPrevious().toLowerCase();
		MPlayer player = parser.getServer().getConfigurationManager().getPlayerByUsername(username);
		if (player == null) {
			parser.error("§7Игрок с ником §f" + username + "§7 не найден.");
			return null;
		}
		parser.error(null);
		return Collections.singleton(player);
	}

}

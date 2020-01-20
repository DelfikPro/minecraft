package net.minecraft.command.handling.args;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.command.api.Arg;
import net.minecraft.command.api.OptionalArgFiller;
import net.minecraft.command.api.TabCompleter;
import net.minecraft.entity.player.MPlayer;
import net.minecraft.entity.player.Player;
import net.minecraft.util.BlockPos;

import java.util.Collection;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractArg<T> implements Arg<T> {

	private final String caption;

	@Getter
	private final String description;

	private OptionalArgFiller<T> filler;
	private TabCompleter tabCompleter = (player, pos) -> player.getServerForPlayer().getMinecraftServer().getConfigurationManager().getPlayers().stream().map(Player::getName).collect(Collectors.toList());

	public Collection<String> performTabCompletion(MPlayer player, BlockPos pos) {
		return tabCompleter.tabComplete(player, pos);
	}

	public AbstractArg<T> tabCompleter(TabCompleter tabCompleter) {
		this.tabCompleter = tabCompleter;
		return this;
	}

	public String getCaption() {
		return "<" + caption + ">";
	}

	public abstract T get(ArgsParser parser);

	public T getDefaultValue(ArgsParser parser) {
		return filler.fill(parser);
	}

	public AbstractArg<T> defaults(OptionalArgFiller<T> filler) {
		this.filler = filler;
		return this;
	}

	public boolean isEssential() {
		return filler == null;
	}

	public AbstractArg<T> defaults(final T value) {
		this.filler = parser -> value;
		return this;
	}

	public int getEssentialPartsAmount() {
		return 1;
	}

}

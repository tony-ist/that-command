package tony.thatcommand.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.fabric.FabricAdapter;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;

import tony.thatcommand.ThatCommandConfig;
import tony.thatcommand.selection.BoxExpansion;
import tony.thatcommand.selection.BuildBox;

/**
 * {@code //that}: makes the build the player is looking at their WorldEdit selection. The block in the player's line
 * of sight, found the way {@code //hpos1} finds it, is grown into a box until only air surrounds it, see
 * {@link BoxExpansion}, so the box covers everything connected to that block; the corners are then set as
 * {@code //pos1} and {@code //pos2} would set them. Anything touching the build pulls the box out to cover it, which
 * is why a build should hover in the air: looking at a build standing on the ground would take in the ground, and the
 * command refuses rather than select more than {@link ThatCommandConfig#maxVolume} blocks.
 */
public final class ThatSelectCommand {
	/** Vanilla permission required to run the command (gamemasters = op level 2 / cheats). */
	public static final PermissionCheck PERMISSION = Commands.LEVEL_GAMEMASTERS;
	/** How far away, in blocks, the block the player is looking at may be; the range {@code //hpos1} looks within. */
	public static final int RANGE = 300;

	private ThatSelectCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("/that")
				.requires(Commands.hasPermission(PERMISSION))
				.executes(context -> run(context.getSource()))));
	}

	static int run(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		FabricAdapter adapter = FabricAdapter.get();
		Player actor = adapter.fromNativePlayer(player);

		// The first non-air block along the line of sight, as //hpos1 picks it.
		Location target = actor.getBlockTrace(RANGE);
		if (target == null) {
			source.sendFailure(Component.literal("No block in sight within " + RANGE + " blocks"));
			return 0;
		}
		BlockPos pos = adapter.toBlockPos(target.toVector().toBlockPoint());

		long maxVolume = ThatCommandConfig.get().maxVolume();
		BoxExpansion expansion = BoxExpansion.of(BuildBox.of(pos), source.getLevel(), maxVolume);
		if (!expansion.enclosed()) {
			source.sendFailure(Component.literal("The blocks connected to " + pos.toShortString() + " reach past the limit of "
				+ maxVolume + " blocks; nothing was selected"));
			return 0;
		}
		BuildBox box = expansion.to();

		LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
		World world = actor.getWorld();
		RegionSelector selector = new CuboidRegionSelector(world, adapter.adapt(box.min()), adapter.adapt(box.max()));
		session.setRegionSelector(world, selector);
		// Sends the new corners to a client running WorldEditCUI, which is what //pos1 and //pos2 do after setting one.
		selector.explainRegionAdjust(actor, session);

		source.sendSuccess(() -> Component.literal("Selected " + box.min().toShortString() + " to " + box.max().toShortString()
			+ " (" + box.describeSize() + ", " + box.volume() + " blocks)"), false);
		return 1;
	}
}

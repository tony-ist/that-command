package tony.thatcommand;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod's settings, read from {@code config/that-command.json} in the game directory once at start-up, see
 * {@link #load}. A missing file is written with the defaults so it can be found and edited; a file that cannot be
 * read is left alone and the defaults are used, with the reason in the log. Field names are the JSON keys.
 */
public final class ThatCommandConfig {
	/** Blocks a selection may cover at most, unless the file says otherwise. */
	public static final long DEFAULT_MAX_VOLUME = 5_000_000;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ThatCommandConfig instance = new ThatCommandConfig();

	/** The most blocks {@code //that} may select; a build that reaches past this is not selected at all. */
	private long maxVolume = DEFAULT_MAX_VOLUME;

	private ThatCommandConfig() {
	}

	/** The settings in use; the defaults until {@link #load} has run. */
	public static ThatCommandConfig get() {
		return instance;
	}

	/** Where the settings are read from and, when missing, written to. */
	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(ThatCommand.MOD_ID + ".json");
	}

	/**
	 * Reads the settings from {@link #file}, writing the defaults there first if there is no file yet. A file that
	 * cannot be read or holds an invalid value leaves the defaults in use and is not overwritten, so a typo does not
	 * cost the rest of the file.
	 */
	public static void load() {
		Path file = file();
		if (!Files.exists(file)) {
			instance = new ThatCommandConfig();
			write(file, instance);
			return;
		}
		ThatCommandConfig read;
		try (Reader reader = Files.newBufferedReader(file)) {
			read = GSON.fromJson(reader, ThatCommandConfig.class);
		} catch (IOException | JsonParseException e) {
			ThatCommand.LOGGER.error("Could not read {}; using the default settings", file, e);
			instance = new ThatCommandConfig();
			return;
		}
		// An empty file parses as null; a missing key leaves the field at its default.
		if (read == null) {
			read = new ThatCommandConfig();
		}
		if (read.maxVolume < 1) {
			ThatCommand.LOGGER.error("maxVolume in {} must be at least 1, not {}; using the default of {}", file, read.maxVolume, DEFAULT_MAX_VOLUME);
			read.maxVolume = DEFAULT_MAX_VOLUME;
		}
		instance = read;
	}

	private static void write(Path file, ThatCommandConfig config) {
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException e) {
			ThatCommand.LOGGER.error("Could not write the default settings to {}", file, e);
		}
	}

	/** The most blocks {@code //that} may select; at least 1. */
	public long maxVolume() {
		return maxVolume;
	}
}

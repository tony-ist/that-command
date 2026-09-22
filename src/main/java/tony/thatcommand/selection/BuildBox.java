package tony.thatcommand.selection;

import net.minecraft.core.BlockPos;

/**
 * An inclusive box of blocks, {@code min} to {@code max} corner, both inside it.
 */
public record BuildBox(BlockPos min, BlockPos max) {
	public BuildBox {
		if (min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
			throw new IllegalArgumentException("min " + min + " exceeds max " + max);
		}
	}

	/** The box holding just the block at {@code pos}. */
	public static BuildBox of(BlockPos pos) {
		return new BuildBox(pos, pos);
	}

	public int sizeX() {
		return max.getX() - min.getX() + 1;
	}

	public int sizeY() {
		return max.getY() - min.getY() + 1;
	}

	public int sizeZ() {
		return max.getZ() - min.getZ() + 1;
	}

	public long volume() {
		return (long) sizeX() * sizeY() * sizeZ();
	}

	/** The box with {@code blocks} more layers on every side. */
	public BuildBox grow(int blocks) {
		return new BuildBox(min.offset(-blocks, -blocks, -blocks), max.offset(blocks, blocks, blocks));
	}

	/** The smallest box containing both this box and {@code other}. */
	public BuildBox union(BuildBox other) {
		return new BuildBox(
			new BlockPos(Math.min(min.getX(), other.min.getX()), Math.min(min.getY(), other.min.getY()), Math.min(min.getZ(), other.min.getZ())),
			new BlockPos(Math.max(max.getX(), other.max.getX()), Math.max(max.getY(), other.max.getY()), Math.max(max.getZ(), other.max.getZ()))
		);
	}

	/** The size as {@code x×y×z}, e.g. {@code 12×5×7}. */
	public String describeSize() {
		return sizeX() + "×" + sizeY() + "×" + sizeZ();
	}
}

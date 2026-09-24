package one.nxeu.thaumory.circle;

import com.mojang.math.OctahedralGroup;
import com.mojang.math.Quadrant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * How a circle drawn on a face lies in the world (requirements §4.3). A circle's front points away
 * from the face it is drawn on: up on the floor, down on the ceiling, out of a wall. Within the
 * face, the circle keeps the floor's north, east, south and west, turned into the world by the
 * same rotation that turns the floor models in the blockstates ({@link #xRotation},
 * {@link #yRotation}), so the chalk joins up where it is drawn to.
 */
public final class CirclePlane {
    private CirclePlane() {}

    /** The blockstate x rotation that tips a floor model onto the face {@code front} points away from. */
    public static Quadrant xRotation(Direction front) {
        return switch (front) {
            case UP -> Quadrant.R0;
            case DOWN -> Quadrant.R180;
            default -> Quadrant.R90;
        };
    }

    /** The blockstate y rotation that follows {@link #xRotation}. */
    public static Quadrant yRotation(Direction front) {
        return switch (front) {
            case UP, DOWN, NORTH -> Quadrant.R0;
            case EAST -> Quadrant.R90;
            case SOUTH -> Quadrant.R180;
            case WEST -> Quadrant.R270;
        };
    }

    /** The rotation from the floor to the face {@code front} points away from. */
    public static OctahedralGroup rotation(Direction front) {
        return Quadrant.fromXYAngles(xRotation(front), yRotation(front));
    }

    /** The world direction of the in-face direction {@code local} (north, east, south or west). */
    public static Direction toWorld(Direction front, Direction local) {
        if (local.getAxis() == Direction.Axis.Y) {
            throw new IllegalArgumentException("Not a direction within a face: " + local);
        }
        return rotation(front).rotate(local);
    }

    /** The world offset of the in-face cell ({@code dx}, {@code dz}), east and south being positive. */
    public static BlockPos offset(Direction front, int dx, int dz) {
        return BlockPos.ZERO.relative(toWorld(front, Direction.EAST), dx).relative(toWorld(front, Direction.SOUTH), dz);
    }
}

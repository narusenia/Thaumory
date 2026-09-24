package one.nxeu.thaumory.circle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class CirclePlaneTest {
    private static final List<Direction> IN_FACE = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    @Test
    void theFloorKeepsTheWorldDirections() {
        for (Direction local : IN_FACE) {
            assertEquals(local, CirclePlane.toWorld(Direction.UP, local));
        }
        assertEquals(new BlockPos(2, 0, -1), CirclePlane.offset(Direction.UP, 2, -1));
    }

    @Test
    void everyFaceLiesAcrossItsFront() {
        for (Direction front : Direction.values()) {
            Set<Direction> seen = new HashSet<>();
            for (Direction local : IN_FACE) {
                Direction world = CirclePlane.toWorld(front, local);
                assertNotEquals(front.getAxis(), world.getAxis(), front + " " + local);
                seen.add(world);
            }
            assertEquals(4, seen.size(), front.toString());
        }
    }

    @Test
    void onAWallTheInFaceSouthIsUp() {
        for (Direction front : Direction.Plane.HORIZONTAL) {
            assertEquals(Direction.UP, CirclePlane.toWorld(front, Direction.SOUTH));
            assertEquals(Direction.DOWN, CirclePlane.toWorld(front, Direction.NORTH));
        }
    }

    @Test
    void theBlockstateRotationsTurnEastAndSouthTheSameWay() {
        // x = 90 tips up to north (south to up); y turns north to east.
        assertEquals(Direction.EAST, CirclePlane.toWorld(Direction.NORTH, Direction.EAST));
        assertEquals(Direction.WEST, CirclePlane.toWorld(Direction.SOUTH, Direction.EAST));
        assertEquals(Direction.SOUTH, CirclePlane.toWorld(Direction.EAST, Direction.EAST));
        assertEquals(Direction.NORTH, CirclePlane.toWorld(Direction.WEST, Direction.EAST));
        assertEquals(Direction.NORTH, CirclePlane.toWorld(Direction.DOWN, Direction.SOUTH));
    }

    @Test
    void theFrontIsWhereTheFloorsUpGoes() {
        for (Direction front : Direction.values()) {
            assertEquals(front, CirclePlane.rotation(front).rotate(Direction.UP));
        }
    }

    @Test
    void offsetsFollowTheFace() {
        assertEquals(new BlockPos(1, -2, 0), CirclePlane.offset(Direction.NORTH, 1, -2));
        assertEquals(new BlockPos(0, 3, 1), CirclePlane.offset(Direction.EAST, 1, 3));
        assertEquals(new BlockPos(-1, 0, 0), CirclePlane.offset(Direction.DOWN, -1, 0));
        assertEquals(new BlockPos(0, 0, -1), CirclePlane.offset(Direction.DOWN, 0, 1));
    }
}

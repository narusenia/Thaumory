package one.nxeu.thaumory.wand.spell;

import java.util.ArrayList;
import java.util.List;

/** The geometry behind the focus spells (requirements §17.7), free of Minecraft. */
public final class SpellMath {
    private SpellMath() {}

    public record Point(double x, double y, double z) {
        double distanceSq(Point other) {
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    /**
     * Whether {@code offset} from the caster lies within {@code range} and within {@code halfAngle}
     * degrees of {@code look}, a direction of any length.
     */
    public static boolean inCone(Point offset, Point look, double range, double halfAngle) {
        double distanceSq = offset.distanceSq(new Point(0, 0, 0));
        if (distanceSq > range * range) {
            return false;
        }
        if (distanceSq == 0) {
            return true;
        }
        double lookLength = Math.sqrt(look.distanceSq(new Point(0, 0, 0)));
        double cos = (offset.x() * look.x() + offset.y() * look.y() + offset.z() * look.z()) / (Math.sqrt(distanceSq) * lookLength);
        return cos >= Math.cos(Math.toRadians(halfAngle));
    }

    /**
     * Who a chain of lightning jumps to after {@code start}: each time the nearest candidate not yet
     * struck within {@code radius} of the last one struck, up to {@code count}.
     *
     * @return indices into {@code candidates}, in the order they are struck
     */
    public static List<Integer> chain(Point start, List<Point> candidates, double radius, int count) {
        List<Integer> struck = new ArrayList<>();
        Point last = start;
        while (struck.size() < count) {
            int nearest = -1;
            double best = radius * radius;
            for (int i = 0; i < candidates.size(); i++) {
                double distance = candidates.get(i).distanceSq(last);
                // Within reach; on a tie the first candidate found wins.
                if (!struck.contains(i) && (nearest < 0 ? distance <= best : distance < best)) {
                    nearest = i;
                    best = distance;
                }
            }
            if (nearest < 0) {
                break;
            }
            struck.add(nearest);
            last = candidates.get(nearest);
        }
        return struck;
    }
}

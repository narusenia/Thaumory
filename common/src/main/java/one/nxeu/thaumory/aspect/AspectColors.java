package one.nxeu.thaumory.aspect;

import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;

public final class AspectColors {
    private AspectColors() {}

    /** The aspects' colors averaged by amount, as 0xRRGGBB. White for an empty list. */
    public static int mix(AspectList aspects) {
        long total = aspects.total();
        if (total == 0) {
            return 0xFFFFFF;
        }
        long red = 0;
        long green = 0;
        long blue = 0;
        for (AspectStack stack : aspects.stacks()) {
            int color = stack.aspect().color();
            red += (long) ((color >> 16) & 0xFF) * stack.amount();
            green += (long) ((color >> 8) & 0xFF) * stack.amount();
            blue += (long) (color & 0xFF) * stack.amount();
        }
        return (int) (Math.round((double) red / total) << 16 | Math.round((double) green / total) << 8 | Math.round((double) blue / total));
    }
}

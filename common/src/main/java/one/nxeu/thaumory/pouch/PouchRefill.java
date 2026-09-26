package one.nxeu.thaumory.pouch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;

/** How an Essentia pouch tops items up from its jars (requirements §17.6), free of Minecraft. */
public final class PouchRefill {
    private PouchRefill() {}

    /**
     * @param jars  what each jar holds afterwards, in the same order
     * @param taken what came out of them, for the item
     */
    public record Draw(List<AspectList> jars, AspectList taken) {}

    /**
     * What an item holding {@code stored} wants this time: of each aspect it {@code accepts}, up to
     * {@code perTransfer}, and no more than it has room for under {@code capacity}.
     */
    public static AspectList wanted(AspectList stored, Set<Aspect> accepts, int capacity, int perTransfer) {
        AspectList.Builder wanted = AspectList.builder();
        for (Aspect aspect : accepts) {
            wanted.add(aspect, Math.min(perTransfer, capacity - stored.amount(aspect)));
        }
        return wanted.build();
    }

    /** Takes {@code wanted} out of the jars, as far as they hold it, the first jar first. */
    public static Draw draw(List<AspectList> jars, AspectList wanted) {
        List<AspectList> left = new ArrayList<>(jars);
        AspectList.Builder taken = AspectList.builder();
        for (AspectStack stack : wanted.stacks()) {
            int remaining = stack.amount();
            for (int i = 0; i < left.size() && remaining > 0; i++) {
                int amount = Math.min(remaining, left.get(i).amount(stack.aspect()));
                if (amount > 0) {
                    left.set(i, left.get(i).minus(AspectList.of(stack.aspect(), amount)));
                    taken.add(stack.aspect(), amount);
                    remaining -= amount;
                }
            }
        }
        return new Draw(List.copyOf(left), taken.build());
    }
}

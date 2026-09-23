package one.nxeu.thaumory.pipe;

import java.util.ArrayList;
import java.util.List;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;

/** Splitting a network's Essentia in transit among its pipes, which keep it when the world saves. */
public final class PipeBuffer {
    private PipeBuffer() {}

    /**
     * {@code buffer} in {@code pipes} shares as even as they can be: each aspect's remainder goes
     * one each to the first pipes. The shares add up to {@code buffer}.
     */
    public static List<AspectList> split(AspectList buffer, int pipes) {
        List<AspectList.Builder> shares = new ArrayList<>();
        for (int i = 0; i < pipes; i++) {
            shares.add(AspectList.builder());
        }
        for (AspectStack stack : buffer.stacks()) {
            int each = stack.amount() / pipes;
            int rest = stack.amount() % pipes;
            for (int i = 0; i < pipes; i++) {
                shares.get(i).add(stack.aspect(), each + (i < rest ? 1 : 0));
            }
        }
        return shares.stream().map(AspectList.Builder::build).toList();
    }
}

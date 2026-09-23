package one.nxeu.thaumory.pipe;

import java.util.Comparator;
import java.util.List;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectList;
import one.nxeu.thaumory.api.aspect.AspectStack;

/**
 * One step of a pipe network (requirements §8.2): what is in transit reaches receivers, highest
 * priority first; then receivers draw on containers of lower priority, which arrives next step.
 */
public final class PipeFlow {
    private PipeFlow() {}

    /**
     * @param buffer    Essentia in transit
     * @param capacity  how much the network can hold in transit
     * @param endpoints in a fixed order, which breaks ties between equal priorities
     * @return what is in transit afterwards
     */
    public static AspectList step(AspectList buffer, int capacity, List<? extends PipeEndpoint> endpoints, PipeSettings settings) {
        List<? extends PipeEndpoint> receivers = endpoints.stream()
                .sorted(Comparator.comparingInt(PipeEndpoint::priority).reversed())
                .toList();
        AspectList carried = deliver(buffer, receivers, settings.rate());
        return draw(carried, capacity, receivers, settings.rate());
    }

    private static AspectList deliver(AspectList buffer, List<? extends PipeEndpoint> receivers, int rate) {
        for (PipeEndpoint receiver : receivers) {
            int budget = rate;
            for (AspectStack stack : buffer.sortedByAmount()) {
                if (budget <= 0) {
                    break;
                }
                int amount = Math.min(budget, Math.min(stack.amount(), receiver.space(stack.aspect())));
                if (amount > 0) {
                    int moved = receiver.insert(stack.aspect(), amount);
                    buffer = buffer.minus(AspectList.of(stack.aspect(), moved));
                    budget -= moved;
                }
            }
        }
        return buffer;
    }

    /** Lowest priority sources first, so the containers that matter least give first. */
    private static AspectList draw(AspectList buffer, int capacity, List<? extends PipeEndpoint> receivers, int rate) {
        for (PipeEndpoint receiver : receivers) {
            int budget = rate;
            sources:
            for (PipeEndpoint source : receivers.reversed()) {
                if (source.priority() >= receiver.priority()) {
                    break;
                }
                for (AspectStack stack : source.contents().sortedByAmount()) {
                    int free = capacity - buffer.total();
                    if (free <= 0) {
                        return buffer;
                    }
                    if (budget <= 0) {
                        break sources;
                    }
                    Aspect aspect = stack.aspect();
                    // What is already on its way counts towards the receiver's room.
                    int wanted = Math.min(Math.min(budget, free), receiver.space(aspect) - buffer.amount(aspect));
                    if (wanted > 0) {
                        int moved = source.extract(aspect, Math.min(wanted, stack.amount()));
                        buffer = buffer.plus(AspectList.of(aspect, moved));
                        budget -= moved;
                    }
                }
            }
        }
        return buffer;
    }
}

package one.nxeu.thaumory.block.pipe;

/**
 * A pipe that reaches into a Crucible next to it, which its network then draws from as the
 * lowest priority of all and never fills (requirements §8.2). Needs no power.
 */
public final class PumpBlock extends EssentiaPipeBlock {
    public PumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean joinsCrucibles() {
        return true;
    }
}

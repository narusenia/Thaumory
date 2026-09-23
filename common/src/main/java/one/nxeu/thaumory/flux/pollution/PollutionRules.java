package one.nxeu.thaumory.flux.pollution;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/**
 * What Flux turns blocks into and what purification turns them back into, loaded from
 * {@code data/<namespace>/thaumory/pollution/*.json}. When several files take the same block, the
 * one later in file id order wins.
 */
public final class PollutionRules extends SimpleJsonResourceReloadListener<PollutionFile> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private record Rule(Block polluted, List<Predicate<BlockState>> from, Block restore) {}

    private static volatile List<Rule> rules = List.of();

    public PollutionRules() {
        super(PollutionFile.CODEC, FileToIdConverter.json("thaumory/pollution"));
    }

    /** The polluted block {@code state} turns into, if Flux can turn it at all. */
    public static Optional<Block> pollutedFor(BlockState state) {
        for (Rule rule : rules) {
            if (rule.from.stream().anyMatch(matcher -> matcher.test(state))) {
                return Optional.of(rule.polluted);
            }
        }
        return Optional.empty();
    }

    /** What purification turns a polluted block back into. */
    public static Optional<Block> restoreFor(Block polluted) {
        return rules.stream().filter(rule -> rule.polluted == polluted).map(Rule::restore).findFirst();
    }

    @Override
    protected void apply(Map<Identifier, PollutionFile> files, ResourceManager manager, ProfilerFiller profiler) {
        List<Rule> loaded = new ArrayList<>();
        // Later files first, so they win when both take the same block.
        new TreeMap<>(files).descendingMap().forEach((id, file) -> {
            Optional<Block> polluted = BuiltInRegistries.BLOCK.getOptional(file.polluted());
            Optional<Block> restore = BuiltInRegistries.BLOCK.getOptional(file.restore());
            if (polluted.isEmpty() || restore.isEmpty()) {
                LOGGER.warn("Skipping pollution file {}: unknown block {}", id, polluted.isEmpty() ? file.polluted() : file.restore());
                return;
            }
            List<Predicate<BlockState>> from = new ArrayList<>();
            for (String entry : file.from()) {
                if (entry.startsWith("#")) {
                    Identifier tagId = Identifier.tryParse(entry.substring(1));
                    if (tagId == null) {
                        LOGGER.warn("Pollution file {}: skipping bad tag {}", id, entry);
                        continue;
                    }
                    TagKey<Block> tag = TagKey.create(Registries.BLOCK, tagId);
                    from.add(state -> state.is(tag));
                } else {
                    Identifier blockId = Identifier.tryParse(entry);
                    Optional<Block> block = blockId == null ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(blockId);
                    if (block.isEmpty()) {
                        LOGGER.warn("Pollution file {}: skipping unknown block {}", id, entry);
                        continue;
                    }
                    from.add(state -> state.is(block.get()));
                }
            }
            loaded.add(new Rule(polluted.get(), List.copyOf(from), restore.get()));
        });
        rules = List.copyOf(loaded);
        LOGGER.info("Loaded {} pollution rules", rules.size());
    }
}

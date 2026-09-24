package one.nxeu.thaumory.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/**
 * Reads every file of a folder whose entries stack across datapacks (item aspects, infusion capacity).
 *
 * <p>Files come back from the lowest-priority datapack to the highest, and by file id within a pack,
 * so applying them in order lets a higher datapack override a lower one. A file at the same path in
 * a higher datapack replaces the lower file entirely, as with vanilla data. Broken files are skipped
 * with a warning.
 */
public final class OrderedDataFiles {
    private OrderedDataFiles() {}

    public static <T> List<Map.Entry<Identifier, T>> load(ResourceManager manager, FileToIdConverter files, Codec<T> codec,
            String kind, Logger logger) {
        Map<String, Integer> packPriority = new HashMap<>();
        manager.listPacks().map(PackResources::packId).forEach(id -> packPriority.putIfAbsent(id, packPriority.size()));

        record Loaded<T>(Identifier id, int priority, T file) {}
        List<Loaded<T>> loaded = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : files.listMatchingResources(manager).entrySet()) {
            Identifier id = files.fileToId(entry.getKey());
            Resource resource = entry.getValue();
            try (Reader reader = resource.openAsReader()) {
                codec.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
                        .ifSuccess(file -> loaded.add(new Loaded<>(id, packPriority.getOrDefault(resource.sourcePackId(), -1), file)))
                        .ifError(error -> logger.warn("Skipping {} file {} from {}: {}", kind, id, resource.sourcePackId(), error.message()));
            } catch (IOException | RuntimeException e) {
                logger.warn("Skipping {} file {} from {}", kind, id, resource.sourcePackId(), e);
            }
        }

        loaded.sort(Comparator.comparingInt((Loaded<T> l) -> l.priority()).thenComparing(Loaded::id));
        return loaded.stream().map(l -> Map.entry(l.id(), l.file())).toList();
    }
}

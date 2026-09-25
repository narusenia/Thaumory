package one.nxeu.thaumory.research;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.circle.CircleDefinitionReloadListener;
import one.nxeu.thaumory.knowledge.CircleCombination;
import one.nxeu.thaumory.knowledge.PlayerKnowledge;

/** Research facts read from a player's knowledge, with item tags and circle effects as the server knows them. */
public record KnowledgeFacts(PlayerKnowledge knowledge) implements ResearchFacts {
    @Override
    public Set<Identifier> scannedItems() {
        return knowledge.scanned(PlayerKnowledge.ITEMS);
    }

    @Override
    public boolean scannedAnyIn(Identifier tag) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, tag);
        return scannedItems().stream().anyMatch(id -> BuiltInRegistries.ITEM.getOptional(id)
                .map(item -> item.builtInRegistryHolder().is(key)).orElse(false));
    }

    @Override
    public Set<Identifier> knownAspects() {
        return knowledge.aspects();
    }

    @Override
    public int circles(boolean success, Optional<Identifier> effect) {
        PlayerKnowledge.CircleOutcome wanted = success ? PlayerKnowledge.CircleOutcome.SUCCESS : PlayerKnowledge.CircleOutcome.FAILURE;
        int count = 0;
        for (Map.Entry<CircleCombination, PlayerKnowledge.CircleOutcome> entry : knowledge.circles().entrySet()) {
            if (entry.getValue() == wanted && (effect.isEmpty() || effect.equals(effectOf(entry.getKey())))) {
                count++;
            }
        }
        return count;
    }

    private static Optional<Identifier> effectOf(CircleCombination combination) {
        Optional<Aspect> first = ThaumoryApi.aspects().get(combination.first());
        Optional<Aspect> second = ThaumoryApi.aspects().get(combination.second());
        Optional<Aspect> parameter = combination.parameter().flatMap(ThaumoryApi.aspects()::get);
        Optional<Aspect> slot4 = combination.slot4().flatMap(ThaumoryApi.aspects()::get);
        if (first.isEmpty() || second.isEmpty() || (combination.parameter().isPresent() && parameter.isEmpty())
                || (combination.slot4().isPresent() && slot4.isEmpty())) {
            return Optional.empty();
        }
        return CircleDefinitionReloadListener.definitions().find(first.get(), second.get(), parameter, slot4)
                .map(definition -> definition.effect());
    }
}

package one.nxeu.thaumory.fabric.transfer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import java.util.Optional;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.api.ThaumoryApi;
import one.nxeu.thaumory.api.aspect.Aspect;
import org.jspecify.annotations.Nullable;

/**
 * One aspect's Essentia as a Fabric Transfer API resource. Amounts are plain Essentia: 1 is 1.
 * Essentia carries no components.
 */
public final class EssentiaVariant implements TransferVariant<@Nullable Aspect> {
    private static final EssentiaVariant BLANK = new EssentiaVariant(null);

    /** By aspect id; a blank variant is an empty string, and an unknown aspect fails. */
    public static final Codec<EssentiaVariant> CODEC = Codec.STRING.comapFlatMap(
            id -> id.isEmpty() ? DataResult.success(BLANK)
                    : Optional.ofNullable(Identifier.tryParse(id)).flatMap(ThaumoryApi.aspects()::get)
                            .map(aspect -> DataResult.success(of(aspect)))
                            .orElseGet(() -> DataResult.error(() -> "Unknown aspect " + id)),
            variant -> variant.isBlank() ? "" : variant.aspect.id().toString());

    private final @Nullable Aspect aspect;
    /** Aspects live in Thaumory's own registry, not a vanilla one, so the holder is a direct one. */
    private final Holder<@Nullable Aspect> holder;

    private EssentiaVariant(@Nullable Aspect aspect) {
        this.aspect = aspect;
        this.holder = Holder.direct(aspect);
    }

    public static EssentiaVariant blank() {
        return BLANK;
    }

    public static EssentiaVariant of(Aspect aspect) {
        return new EssentiaVariant(Objects.requireNonNull(aspect));
    }

    /** Empty for the blank variant. */
    public Optional<Aspect> aspect() {
        return Optional.ofNullable(aspect);
    }

    @Override
    public boolean isBlank() {
        return aspect == null;
    }

    @Override
    public @Nullable Aspect getObject() {
        return aspect;
    }

    @Override
    public Holder<@Nullable Aspect> typeHolder() {
        return holder;
    }

    @Override
    public DataComponentPatch getComponentsPatch() {
        return DataComponentPatch.EMPTY;
    }

    @Override
    public DataComponentMap getComponents() {
        return DataComponentMap.EMPTY;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EssentiaVariant variant && Objects.equals(aspect, variant.aspect);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(aspect);
    }

    @Override
    public String toString() {
        return "EssentiaVariant{" + (aspect == null ? "blank" : aspect.id()) + "}";
    }
}

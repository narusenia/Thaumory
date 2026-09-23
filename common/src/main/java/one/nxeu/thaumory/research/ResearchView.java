package one.nxeu.thaumory.research;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The chapters one player can see, as the server works them out: the book shows this as it is,
 * since only the server knows the circle definitions some conditions need.
 *
 * @param closed how many chapters are not open yet
 */
public record ResearchView(List<ChapterView> chapters, int closed) {
    public static final ResearchView EMPTY = new ResearchView(List.of(), 0);

    /** One condition, already worded with the player's progress. */
    public record ConditionLine(Component text, boolean met) {
        static final StreamCodec<RegistryFriendlyByteBuf, ConditionLine> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, ConditionLine::text,
                ByteBufCodecs.BOOL, ConditionLine::met,
                ConditionLine::new);
    }

    /** @param unlocks the items the chapter's recipes make */
    public record ChapterView(Identifier id, Identifier icon, boolean complete, List<ConditionLine> conditions, List<Identifier> unlocks) {
        static final StreamCodec<RegistryFriendlyByteBuf, ChapterView> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, ChapterView::id,
                Identifier.STREAM_CODEC, ChapterView::icon,
                ByteBufCodecs.BOOL, ChapterView::complete,
                ConditionLine.STREAM_CODEC.apply(ByteBufCodecs.list()), ChapterView::conditions,
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), ChapterView::unlocks,
                ChapterView::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchView> STREAM_CODEC = StreamCodec.composite(
            ChapterView.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchView::chapters,
            ByteBufCodecs.VAR_INT, ResearchView::closed,
            ResearchView::new);
}

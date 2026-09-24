package one.nxeu.thaumory.research;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * What one player's book shows, as the server works it out: the book shows this as it is, since
 * only the server knows the circle definitions some conditions need.
 *
 * @param categories the bookmarks, in order
 * @param chapters   open and complete chapters
 * @param unknown    chapters shown only as "?": where they sit, never what they are
 */
public record ResearchView(List<CategoryView> categories, List<ChapterView> chapters, List<Node> unknown) {
    public static final ResearchView EMPTY = new ResearchView(List.of(), List.of(), List.of());

    public record CategoryView(Identifier id, Identifier icon, Identifier background) {
        static final StreamCodec<RegistryFriendlyByteBuf, CategoryView> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, CategoryView::id,
                Identifier.STREAM_CODEC, CategoryView::icon,
                Identifier.STREAM_CODEC, CategoryView::background,
                CategoryView::new);
    }

    /** A place in a category's tree, and the chapters it draws lines from. */
    public record Node(Identifier category, int x, int y, List<Identifier> requires) {
        static final StreamCodec<RegistryFriendlyByteBuf, Node> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Node::category,
                ByteBufCodecs.VAR_INT, Node::x,
                ByteBufCodecs.VAR_INT, Node::y,
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), Node::requires,
                Node::new);
    }

    /** One condition, already worded with the player's progress. */
    public record ConditionLine(Component text, boolean met) {
        static final StreamCodec<RegistryFriendlyByteBuf, ConditionLine> STREAM_CODEC = StreamCodec.composite(
                ComponentSerialization.STREAM_CODEC, ConditionLine::text,
                ByteBufCodecs.BOOL, ConditionLine::met,
                ConditionLine::new);
    }

    /** @param unlocks the items the chapter's recipes make */
    public record ChapterView(Identifier id, Identifier icon, Node node, boolean complete, List<ConditionLine> conditions,
            List<Identifier> unlocks) {
        static final StreamCodec<RegistryFriendlyByteBuf, ChapterView> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, ChapterView::id,
                Identifier.STREAM_CODEC, ChapterView::icon,
                Node.STREAM_CODEC, ChapterView::node,
                ByteBufCodecs.BOOL, ChapterView::complete,
                ConditionLine.STREAM_CODEC.apply(ByteBufCodecs.list()), ChapterView::conditions,
                Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), ChapterView::unlocks,
                ChapterView::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchView> STREAM_CODEC = StreamCodec.composite(
            CategoryView.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchView::categories,
            ChapterView.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchView::chapters,
            Node.STREAM_CODEC.apply(ByteBufCodecs.list()), ResearchView::unknown,
            ResearchView::new);
}

package one.nxeu.thaumory.client.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.vex.VexModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.VexRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import one.nxeu.thaumory.entity.VoidRemnant;
import org.jspecify.annotations.Nullable;

/** The Void Remnant: a vex's shape with the End portal's starfield showing through it. */
public final class VoidRemnantRenderer extends MobRenderer<VoidRemnant, VexRenderState, VexModel> {
    /** The renderer needs a texture; the body is drawn with the portal's starfield instead. */
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/illager/vex.png");

    public VoidRemnantRenderer(EntityRendererProvider.Context context) {
        super(context, new VexModel(context.bakeLayer(ModelLayers.VEX)), 0.3f);
    }

    @Override
    protected int getBlockLightLevel(VoidRemnant entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public Identifier getTextureLocation(VexRenderState state) {
        return TEXTURE;
    }

    @Override
    protected @Nullable RenderType getRenderType(VexRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        return isBodyVisible || forceTransparent ? RenderTypes.endPortal() : super.getRenderType(state, isBodyVisible, forceTransparent, appearGlowing);
    }

    @Override
    public VexRenderState createRenderState() {
        return new VexRenderState();
    }

    @Override
    public void extractRenderState(VoidRemnant entity, VexRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, itemModelResolver, partialTicks);
        state.isCharging = entity.isAggressive();
    }
}

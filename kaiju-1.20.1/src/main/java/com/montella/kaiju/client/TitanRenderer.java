package com.montella.kaiju.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.montella.kaiju.KaijuMod;
import com.montella.kaiju.entity.TitanEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;

/** Renders a skinned titan mesh in several material passes, then emissive passes, then its energy effects. */
public abstract class TitanRenderer<T extends TitanEntity> extends EntityRenderer<T> {
    private final Map<T, Mesh> meshes = new WeakHashMap<>();
    protected final ResourceLocation[] mats, glows;
    protected final Mesh.Out out = new Mesh.Out();

    protected TitanRenderer(EntityRendererProvider.Context ctx, String folder, String[] mats, String[] glows) {
        super(ctx);
        this.mats = new ResourceLocation[mats.length];
        for (int i = 0; i < mats.length; i++) this.mats[i] = tex(folder, mats[i]);
        this.glows = new ResourceLocation[glows.length];
        for (int i = 0; i < glows.length; i++) this.glows[i] = tex(folder, glows[i]);
        this.shadowRadius = 0;
    }

    static ResourceLocation tex(String folder, String name) { return new ResourceLocation(KaijuMod.ID, "textures/entity/" + folder + "/" + name + ".png"); }

    protected abstract Mesh build(T e);

    protected abstract float glowLevel(T e, int glowTex, int group, float pt);

    protected abstract int glowGroups(int glowTex);

    protected abstract float[] glowColor(int glowTex);

    protected IntPredicate visible(T e) { return v -> true; }

    protected abstract void effects(T e, float pt, PoseStack ps, MultiBufferSource buf);

    @Override
    public ResourceLocation getTextureLocation(T e) { return mats[0]; }

    @Override
    public boolean shouldRender(T e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) { return true; }

    @Override
    public void render(T e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        Mesh mesh = meshes.computeIfAbsent(e, this::build);
        e.pose(pt);
        float body = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-body));
        out.pose = ps.last();
        out.light = light;
        out.overlay = LivingEntityRenderer.getOverlayCoords(e, 0);
        out.r = out.g = out.b = out.a = 1;
        IntPredicate vis = visible(e);
        for (int m = 0; m < mats.length; m++) {
            out.vc = buf.getBuffer(RenderType.entityCutoutNoCull(mats[m]));
            mesh.render(e.skel, m, out, vis);
        }
        out.light = LightTexture.FULL_BRIGHT;
        for (int g = 0; g < glows.length; g++) {
            float[] c = glowColor(g);
            for (int grp = 0; grp < glowGroups(g); grp++) {
                float lv = glowLevel(e, g, grp, pt);
                if (lv <= 0.01f) continue;
                out.vc = buf.getBuffer(RenderType.eyes(glows[g]));
                out.r = c[0] * lv; out.g = c[1] * lv; out.b = c[2] * lv; out.a = 1;
                mesh.renderGlow(e.skel, g, grp, out, vis);
            }
        }
        out.r = out.g = out.b = 1;
        ps.popPose();
        effects(e, pt, ps, buf);
        super.render(e, yaw, pt, ps, buf, light);
    }

    /** Point on a joint, relative to the render origin (entity position) with the body yaw applied. */
    protected Vector3f rel(T e, String joint, float x, float y, float z, float pt) {
        Vector3f p = e.skel.get(joint).point(x, y, z, new Vector3f());
        float body = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        Vec3 w = TitanEntity.toWorld(p, 0, 0, 0, body);
        return new Vector3f((float) w.x, (float) w.y, (float) w.z);
    }

    protected Vector3f relWorld(T e, Vec3 world, float pt) {
        double x = Mth.lerp(pt, e.xo, e.getX()), y = Mth.lerp(pt, e.yo, e.getY()), z = Mth.lerp(pt, e.zo, e.getZ());
        return new Vector3f((float) (world.x - x), (float) (world.y - y), (float) (world.z - z));
    }
}

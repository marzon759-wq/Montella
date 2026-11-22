package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.entity.KaijuEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** F7: the server's collision spheres, computed from the same pose tracks, drawn as boxes over the model */
final class DebugNodes {
    static void render(KaijuEntity e, float pt, PoseStack ps, MultiBufferSource buf) {
        KaijuSpec spec = e.spec;
        int idx = e.animIndex();
        if (idx < 0 || idx >= spec.clipNames.size()) return;
        KaijuSpec.Clip c = spec.clip(spec.clipNames.get(idx));
        float[] p = new float[3];
        VertexConsumer vc = buf.getBuffer(RenderType.lines());
        float yaw = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        for (int i = 0; i < spec.nodes.size(); i++) {
            float r = e.nodeR(i);
            if (r <= 0) continue;
            spec.nodePx(c, e.animTick(), i, p);
            Vec3 w = KaijuSpec.toWorld(p[0], p[1], p[2], yaw, e.k());
            String n = spec.nodes.get(i).name();
            float cr = n.startsWith("tail") ? 1f : n.startsWith("hand") || n.startsWith("foot") ? 1f : 0.3f;
            float cg = n.startsWith("tail") ? 0.6f : 1f;
            LevelRenderer.renderLineBox(ps, vc, new AABB(w, w).inflate(r), cr, cg, 0.3f, 1f);
        }
    }
}

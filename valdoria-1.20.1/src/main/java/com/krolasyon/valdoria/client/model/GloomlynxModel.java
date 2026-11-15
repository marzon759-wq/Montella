package com.krolasyon.valdoria.client.model;

import com.krolasyon.valdoria.entity.Gloomlynx;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public class GloomlynxModel extends HierarchicalModel<Gloomlynx> {
    private final ModelPart root, body, head, tail, earL, earR, legFL, legFR, legBL, legBR;

    public GloomlynxModel(ModelPart model) {
        super(RenderType::entityCutoutNoCull);
        root = model.getChild("root");
        body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("tail");
        earL = head.getChild("ear_l");
        earR = head.getChild("ear_r");
        legFL = root.getChild("leg_fl");
        legFR = root.getChild("leg_fr");
        legBL = root.getChild("leg_bl");
        legBR = root.getChild("leg_br");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Gloomlynx e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (e.isNapping()) {
            // curled up: body on the ground, legs tucked, tail wrapped forward, head resting
            body.y += 5.5F;
            head.xRot += 0.35F;
            head.yRot += 0.6F;
            head.y += 1.0F;
            tail.xRot = -0.1F;
            tail.yRot = 1.9F;
            for (ModelPart leg : new ModelPart[]{legFL, legFR, legBL, legBR}) {
                leg.y += 5.0F;
                leg.xRot = leg == legFL || leg == legFR ? -1.5F : 1.5F;
            }
            body.y += Mth.sin(age * 0.08F) * 0.15F;
            earL.zRot -= 0.3F;
            earR.zRot += 0.3F;
            return;
        }
        float amt = Math.min(limbAmount, 1.0F);
        float run = limbSwing * 0.75F;
        // gallop: fronts together, backs together
        legFL.xRot = Mth.cos(run) * 1.0F * amt;
        legFR.xRot = Mth.cos(run + 0.4F) * 1.0F * amt;
        legBL.xRot = Mth.cos(run + Mth.PI) * 1.0F * amt;
        legBR.xRot = Mth.cos(run + Mth.PI + 0.4F) * 1.0F * amt;
        body.xRot += Mth.sin(run) * 0.06F * amt;
        head.yRot = yaw * Mth.DEG_TO_RAD;
        head.xRot = pitch * Mth.DEG_TO_RAD;
        tail.xRot += Mth.sin(age * 0.12F) * 0.1F + amt * 0.5F;
        tail.yRot += Mth.sin(age * 0.07F) * 0.25F;
        if (e.getTarget() != null) {
            // stalking crouch
            body.y += 1.5F * (1 - amt);
            earL.zRot += 0.5F;
            earR.zRot -= 0.5F;
        }
    }
}

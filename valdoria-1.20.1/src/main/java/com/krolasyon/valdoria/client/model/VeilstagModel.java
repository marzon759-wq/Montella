package com.krolasyon.valdoria.client.model;

import com.krolasyon.valdoria.entity.Veilstag;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public class VeilstagModel extends HierarchicalModel<Veilstag> {
    private final ModelPart root, body, neck, head, tail, earL, earR, antlerL, antlerR, mossL, mossR, legFL, legFR, legBL, legBR;

    public VeilstagModel(ModelPart model) {
        super(RenderType::entityCutoutNoCull);
        root = model.getChild("root");
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        tail = body.getChild("tail");
        earL = head.getChild("ear_l");
        earR = head.getChild("ear_r");
        antlerL = head.getChild("antler_l");
        antlerR = head.getChild("antler_r");
        mossL = antlerL.getChild("moss_l");
        mossR = antlerR.getChild("moss_r");
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
    public void setupAnim(Veilstag e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float walk = limbSwing * 0.6F;
        float amt = Math.min(limbAmount, 1.0F);
        legFL.xRot = Mth.cos(walk) * 1.1F * amt;
        legBR.xRot = Mth.cos(walk) * 1.1F * amt;
        legFR.xRot = Mth.cos(walk + Mth.PI) * 1.1F * amt;
        legBL.xRot = Mth.cos(walk + Mth.PI) * 1.1F * amt;
        body.y += Math.abs(Mth.sin(walk)) * -0.6F * amt;

        boolean grazing = Veilstag.isGrazing(e);
        if (grazing) {
            float t = e.grazeAnim / 40.0F;
            float dip = Mth.clamp(Math.min(t * 4.0F, (1 - t) * 4.0F + 0.2F), 0, 1);
            neck.xRot += 1.35F * dip;
            head.xRot += 0.25F * dip + Mth.sin(age * 0.9F) * 0.06F * dip;
        } else {
            neck.yRot += yaw * Mth.DEG_TO_RAD * 0.5F;
            head.yRot += yaw * Mth.DEG_TO_RAD * 0.4F;
            neck.xRot += pitch * Mth.DEG_TO_RAD * 0.4F;
            if (e.isAlarmed()) neck.xRot -= 0.25F;
        }
        // ears and tail flick, moss strands sway
        earL.zRot += Mth.sin(age * 0.11F) * 0.08F + (e.isAlarmed() ? 0.5F : 0);
        earR.zRot -= Mth.sin(age * 0.11F + 1.3F) * 0.08F + (e.isAlarmed() ? 0.5F : 0);
        tail.xRot += Mth.sin(age * 0.2F) * 0.08F + (e.isAlarmed() ? -0.9F : 0) + amt * -0.3F;
        mossL.xRot += Mth.sin(age * 0.07F) * 0.15F + amt * 0.3F;
        mossR.xRot += Mth.sin(age * 0.07F + 2.0F) * 0.15F + amt * 0.3F;

        boolean baby = e.isBaby();
        antlerL.visible = !baby;
        antlerR.visible = !baby;
        float hs = baby ? 1.35F : 1.0F;
        head.xScale = head.yScale = head.zScale = hs;
    }
}

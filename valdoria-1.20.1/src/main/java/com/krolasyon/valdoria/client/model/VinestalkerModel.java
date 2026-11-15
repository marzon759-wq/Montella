package com.krolasyon.valdoria.client.model;

import com.krolasyon.valdoria.entity.Vinestalker;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public class VinestalkerModel extends HierarchicalModel<Vinestalker> {
    private final ModelPart root, body, abdomen, head, mandL, mandR;
    private final ModelPart[] legs = new ModelPart[6], shins = new ModelPart[6], tendrils = new ModelPart[4];

    public VinestalkerModel(ModelPart model) {
        super(RenderType::entityCutoutNoCull);
        root = model.getChild("root");
        body = root.getChild("body");
        abdomen = body.getChild("abdomen");
        head = body.getChild("head");
        mandL = head.getChild("mandible_l");
        mandR = head.getChild("mandible_r");
        for (int i = 0; i < 3; i++) {
            legs[i] = body.getChild("leg" + i + "_l");
            legs[i + 3] = body.getChild("leg" + i + "_r");
            shins[i] = legs[i].getChild("shin" + i + "_l");
            shins[i + 3] = legs[i + 3].getChild("shin" + i + "_r");
        }
        tendrils[0] = body.getChild("tendril_1");
        tendrils[1] = body.getChild("tendril_2");
        tendrils[2] = abdomen.getChild("tendril_3");
        tendrils[3] = abdomen.getChild("tendril_4");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Vinestalker e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float amt = Math.min(limbAmount, 1.0F);
        float walk = limbSwing * 0.9F;
        for (int i = 0; i < 6; i++) {
            boolean left = i < 3;
            float phase = (i % 3) * 2.1F + (left ? 0 : Mth.PI);
            float lift = Math.max(0, Mth.sin(walk + phase)) * 0.45F * amt;
            float swing = Mth.cos(walk + phase) * 0.35F * amt;
            legs[i].zRot += left ? -lift : lift;
            legs[i].yRot += swing;
        }
        head.yRot = yaw * Mth.DEG_TO_RAD * 0.6F;
        float chew = e.getTarget() != null ? Mth.sin(age * 0.8F) * 0.35F : Mth.sin(age * 0.1F) * 0.05F;
        mandL.yRot = -0.2F - chew;
        mandR.yRot = 0.2F + chew;
        abdomen.xRot += Mth.sin(age * 0.05F) * 0.05F;
        for (int i = 0; i < 4; i++) {
            tendrils[i].xRot = Mth.sin(age * 0.06F + i * 1.7F) * 0.18F + amt * 0.5F;
            tendrils[i].zRot = Mth.cos(age * 0.05F + i) * 0.1F;
        }
        if (e.isHanging()) {
            // clinging upside-down: legs gripping up around the branch, tendrils hanging (they hang "up" in model space)
            for (int i = 0; i < 6; i++) {
                boolean left = i < 3;
                legs[i].zRot += left ? 0.25F : -0.25F;
                shins[i].zRot += left ? -0.35F : 0.35F;
            }
            for (ModelPart t : tendrils) t.xRot = Mth.PI + Mth.sin(age * 0.04F + t.x) * 0.12F;
        }
    }
}

package com.krolasyon.valdoria.client.model;

import com.krolasyon.valdoria.entity.LumenMoth;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public class LumenMothModel extends HierarchicalModel<LumenMoth> {
    private final ModelPart root, body, wingL, wingR, hindL, hindR, antL, antR;

    public LumenMothModel(ModelPart model) {
        super(RenderType::entityCutoutNoCull);
        root = model.getChild("root");
        body = root.getChild("body");
        wingL = body.getChild("wing_l");
        wingR = body.getChild("wing_r");
        hindL = body.getChild("hindwing_l");
        hindR = body.getChild("hindwing_r");
        antL = body.getChild("antenna_l");
        antR = body.getChild("antenna_r");
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(LumenMoth e, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        float phase = age * 1.3F + e.getId() * 0.7F;
        float flap = Mth.sin(phase) * 0.85F - 0.15F;
        wingL.zRot = -flap;
        wingR.zRot = flap;
        hindL.zRot = -flap * 0.8F;
        hindR.zRot = flap * 0.8F;
        hindL.yRot = -0.15F;
        hindR.yRot = 0.15F;
        body.y += Mth.sin(phase) * 0.6F;
        body.xRot = 0.15F;
        antL.xRot += Mth.sin(age * 0.2F) * 0.1F;
        antR.xRot += Mth.sin(age * 0.2F + 1) * 0.1F;
    }
}

package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.compat.Compat;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Vector3f;

/** How blood looks: thick and dark, and from inside it, almost nothing - a red murk two blocks deep. */
public final class BloodFluidClient implements IClientFluidTypeExtensions {
    public static final BloodFluidClient INSTANCE = new BloodFluidClient();

    private BloodFluidClient() {}

    @Override public ResourceLocation getStillTexture() { return Compat.rl("block/blood_still"); }
    @Override public ResourceLocation getFlowingTexture() { return Compat.rl("block/blood_flow"); }
    @Override public ResourceLocation getOverlayTexture() { return Compat.rl("block/blood_overlay"); }
    @Override public ResourceLocation getRenderOverlayTexture(Minecraft mc) { return Compat.rl("textures/misc/under_blood.png"); }
    @Override public int getTintColor() { return 0xFFFFFFFF; }

    @Override
    public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
        return new Vector3f(0.24f, 0.0f, 0.01f);
    }

    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
        RenderSystem.setShaderFogStart(-1.5f);
        RenderSystem.setShaderFogEnd(5.5f);
    }
}

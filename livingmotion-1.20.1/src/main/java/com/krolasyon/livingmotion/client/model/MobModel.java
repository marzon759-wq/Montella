package com.krolasyon.livingmotion.client.model;

import com.krolasyon.livingmotion.LMConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import java.util.ArrayList;
import java.util.List;

/**
 * GeckoLib model for a remodelled mob. Keyframe animations drive the body; this class adds the procedural layer
 * on top: head look, blinking / squinting / angry eyelids, mouth and jaw, fangs, bow aiming and baby proportions.
 */
public class MobModel extends GeoModel<ReplacedMob> {
    private final MobKind kind;
    private BakedGeoModel bakedFor;
    private final List<GeoBone> allBones = new ArrayList<>();
    private GeoBone head, skull, lidL, lidR, mouth, fangL, fangR, armR, armL, tail, earL, earR, brow, nose, arms;

    private static final boolean FACE_TEST = System.getProperty("livingmotion.autotest", "").contains("faces");

    public MobModel(MobKind kind) {
        this.kind = kind;
    }

    @Override
    public ResourceLocation getModelResource(ReplacedMob a) { return kind.geo; }

    @Override
    public ResourceLocation getTextureResource(ReplacedMob a) { return kind.texture; }

    @Override
    public ResourceLocation getAnimationResource(ReplacedMob a) { return kind.anim; }

    private void bind() {
        BakedGeoModel baked = getBakedModel(kind.geo);
        if (baked == bakedFor) return;   // re-bind after a resource reload
        bakedFor = baked;
        allBones.clear();
        for (GeoBone b : baked.topLevelBones()) collect(b);
        head = bone("head"); skull = bone("skull");
        lidL = bone("eyelid_left"); lidR = bone("eyelid_right"); mouth = bone("mouth");
        fangL = bone("fang_left"); fangR = bone("fang_right");
        armR = bone("right_arm"); armL = bone("left_arm");
        tail = bone("tail"); earL = bone("ear_left"); earR = bone("ear_right");
        brow = bone("brow"); nose = bone("nose"); arms = bone("arms");
    }

    private void collect(GeoBone b) {
        allBones.add(b);
        for (GeoBone c : b.getChildBones()) collect(c);
    }

    private GeoBone bone(String n) {
        return getBone(n).orElse(null);
    }

    /** Main pass visibility: every bone visible except the overlay shells. Overlay layers flip this temporarily. */
    public void showMain() {
        bind();
        for (GeoBone b : allBones) {
            b.setHidden(b.getName().endsWith("_outer"));
            b.setChildrenHidden(false);
        }
    }

    /** Overlay pass visibility: only the *_outer shells draw cubes; the hierarchy stays intact for transforms. */
    public void showOverlay() {
        bind();
        for (GeoBone b : allBones) {
            b.setHidden(!b.getName().endsWith("_outer"));
            b.setChildrenHidden(false);
        }
    }

    @Override
    public void setCustomAnimations(ReplacedMob animatable, long instanceId, AnimationState<ReplacedMob> st) {
        if (!(st.getData(DataTickets.ENTITY) instanceof LivingEntity e)) return;
        showMain();
        EntityModelData data = st.getData(DataTickets.ENTITY_MODEL_DATA);
        Track t = animatable.track(e);
        float pt = st.getPartialTick();
        float age = e.tickCount + pt;
        boolean alive = e.deathTime <= 0 && e.isAlive();
        boolean grazing = t.variant != null;

        // --- head look -----------------------------------------------------------------------------------
        if (head != null) {
            if (alive && !grazing && data != null && LMConfig.on(LMConfig.LOOK_AT)) {
                head.setRotX(data.headPitch() * Mth.DEG_TO_RAD);
                head.setRotY(data.netHeadYaw() * Mth.DEG_TO_RAD);
            } else if (!grazing) {
                head.setRotX(0);
                head.setRotY(0);
            }
        }
        // babies: big head on a half-size body (the body scale itself is done in the renderer)
        if (skull != null) {
            float s = data != null && data.isChild() ? (kind.humanoid() ? 1.5f : 1.4f) : 1f;
            skull.setScaleX(s); skull.setScaleY(s); skull.setScaleZ(s);
        }

        // --- bow aiming: arms follow the head like vanilla -------------------------------------------------
        if (kind.family == MobKind.Family.SKELETON && alive && ReplacedMob.aimingBow(e) && armR != null && data != null) {
            float yaw = data.netHeadYaw() * Mth.DEG_TO_RAD, pitch = data.headPitch() * Mth.DEG_TO_RAD;
            boolean drawing = e.isUsingItem();
            float pull = drawing ? Math.min(1f, e.getTicksUsingItem() / 20f) : 0.3f;
            armR.setRotX(Mth.HALF_PI + pitch);
            armR.setRotY(yaw + 0.1f + pull * 0.15f);
            armR.setRotZ(0);
            armL.setRotX(Mth.HALF_PI + pitch);
            armL.setRotY(yaw - 0.5f);
            armL.setRotZ(0);
        }

        // --- face ------------------------------------------------------------------------------------------
        boolean faces = LMConfig.on(LMConfig.FACES);
        float lid = 0f, brow = 0f, open = 0f, fang = 0f;
        if (faces) {
            int period = 70 + (t.seed % 90);
            float bt = (age + (t.seed % 997)) % period;
            if (bt < 4f) lid = Mth.sin(bt / 4f * Mth.PI);                         // blink
            else if ((t.seed & 3) == 0 && bt >= 7f && bt < 11f) lid = Mth.sin((bt - 7f) / 4f * Mth.PI); // double blink
            boolean angry = e instanceof Mob m && m.isAggressive();
            float swell = e instanceof Creeper c ? c.getSwelling(pt) : 0f;
            if (angry || swell > 0) {
                lid = Math.max(lid, 0.4f + swell * 0.3f);
                brow = 0.38f;
            }
            if (e.hurtTime > 0) lid = Math.max(lid, 0.75f * e.hurtTime / 10f);
            if (!alive || e.isSleeping()) lid = 1f;
            if (FACE_TEST) {   // dev only: cycle idle / blink / scowl / open mouth every second for screenshots
                int ph = (e.tickCount / 20) % 4;
                lid = ph == 1 ? 1f : ph == 2 ? 0.45f : 0f;
                brow = ph == 2 ? 0.38f : 0f;
            }

            // mouth: snap on attack, gasp when hurt, slack jaw when dead, groans and chewing at idle
            float sinceAttack = age - t.attackFlash;
            if (t.attackFlash > 0 && sinceAttack < 10f) open = Math.max(open, Mth.sin(sinceAttack / 10f * Mth.PI) * 2.2f);
            if (e.hurtTime > 0) open = Math.max(open, 1.6f * e.hurtTime / 10f);
            if (swell > 0) open = Math.max(open, 1f + swell * 2f);
            if (!alive) open = 2f;
            if (alive && e.hurtTime == 0) {
                if (kind.family == MobKind.Family.ZOMBIE) {
                    float g = (age + t.seed % 400) % 160f;                         // groan every 8 s
                    if (g < 30f) open = Math.max(open, Mth.sin(g / 30f * Mth.PI) * 1.6f);
                    if (angry) open = Math.max(open, 0.6f + 0.6f * Mth.sin(age * 0.9f)); // chomping while chasing
                } else if (kind.family == MobKind.Family.SKELETON) {
                    float g = (age + t.seed % 300) % 120f;                         // rattling jaw
                    if (g < 16f) open = Math.max(open, Math.abs(Mth.sin(g * 1.2f)) * 1.3f);
                } else if (kind.family == MobKind.Family.QUADRUPED) {
                    if (grazing) open = Math.max(open, 0.6f + 0.6f * Mth.sin(age * 0.8f)); // chewing
                    float g = (age + t.seed % 500) % 220f;                         // moo / oink
                    if (g < 20f) open = Math.max(open, Mth.sin(g / 20f * Mth.PI) * 1.2f);
                }
            }
            // spider fangs: twitch at idle, flare on attack
            fang = 0.12f + 0.08f * Mth.sin(age * 0.35f + t.seed);
            if (t.attackFlash > 0 && sinceAttack < 12f) fang = Math.max(fang, Mth.sin(sinceAttack / 12f * Mth.PI) * 0.75f);
            if (!alive) fang = 0.6f;
        }
        if (FACE_TEST && faces) {
            int ph = (e.tickCount / 20) % 4;
            open = ph == 3 ? 2f : ph == 2 ? 0.8f : 0f;
        }
        lid(lidR, lid, brow);
        lid(lidL, lid, -brow);
        details(e, t, st, age, alive, faces);
        if (mouth != null) {
            boolean show = open > 0.04f;
            mouth.setHidden(!show);
            mouth.setScaleY(show ? open : 1f);
        }
        if (fangR != null) fangR.setRotZ(fang);
        if (fangL != null) fangL.setRotZ(-fang);
    }

    /** Secondary motion: tails, ears, brows, noses and species quirks. */
    private void details(LivingEntity e, Track t, AnimationState<ReplacedMob> st, float age, boolean alive, boolean faces) {
        float walk = Math.min(1f, st.getLimbSwingAmount());
        float swing = st.getLimbSwing();
        float seed = t.seed % 1000;
        if (tail != null && alive && kind.family != MobKind.Family.CHICKEN) {
            // lazy sway, a fly-swatting flick every few seconds, bounce while walking
            float flick = ((age + seed) % 140f) < 18f ? Mth.sin(((age + seed) % 140f) / 18f * Mth.TWO_PI) * 0.6f : 0f;
            tail.setRotZ(Mth.sin(age * 0.12f + seed) * 0.12f + flick + Mth.sin(swing * 0.6662f) * 0.25f * walk);
            tail.setRotX(-0.1f * walk - (e.hurtTime > 0 ? 0.5f : 0f));
        }
        for (GeoBone ear : new GeoBone[]{earL, earR}) {
            if (ear == null) continue;
            float phase = (age + seed + (ear == earL ? 37 : 0)) % 110f;
            float twitch = phase < 6f ? Mth.sin(phase / 6f * Mth.PI) * 0.45f : 0f;
            ear.setRotX(twitch + Mth.sin(swing * 0.6662f * 2) * 0.12f * walk);
            ear.setRotZ(e.hurtTime > 0 ? (ear == earL ? 0.5f : -0.5f) : 0f);
        }
        if (brow != null) {
            float y = 0f;
            if (e instanceof net.minecraft.world.entity.Mob m && m.isAggressive()) y = -0.8f;           // scowl
            if (e instanceof AbstractVillager v) {
                if (v.isTrading()) y = 0.6f;                                                           // interested
                if (v instanceof Villager vv && vv.getUnhappyCounter() > 0) y = -0.7f;
                if (e.hurtTime > 0) y = 0.9f;                                                         // shocked
            }
            brow.setPosY(faces ? y : 0f);
        }
        if (nose != null && alive) {
            float phase = (age + seed) % 90f;
            nose.setRotX(phase < 14f ? Mth.sin(phase * 1.6f) * 0.12f : 0f);
            nose.setRotZ(0f);
        }
        // villager: vanilla "no" head shake when it refuses to trade
        if (e instanceof Villager v && v.getUnhappyCounter() > 0 && skull != null) skull.setRotZ(0.3f * Mth.sin(0.45f * age));
        // enderman: head splits open when it is provoked, jitters, arms cradle the carried block
        if (e instanceof EnderMan en && skull != null) {
            boolean creepy = en.isCreepy();
            skull.setPosY(creepy && alive ? 5f : 0f);
            if (head != null) head.setRotZ(creepy && alive ? Mth.sin(age * 2.3f) * 0.04f + Mth.sin(age * 5.1f) * 0.02f : 0f);
            if (en.getCarriedBlock() != null && armR != null && armL != null && alive) {
                armR.setRotX(0.5f); armL.setRotX(0.5f);
                armR.setRotZ(-0.05f); armL.setRotZ(0.05f);
            }
        }
    }

    private static void lid(GeoBone b, float amount, float brow) {
        if (b == null) return;
        boolean show = amount > 0.04f;
        b.setHidden(!show);
        b.setScaleY(show ? amount : 1f);
        b.setRotZ(show ? brow : 0f);
    }
}

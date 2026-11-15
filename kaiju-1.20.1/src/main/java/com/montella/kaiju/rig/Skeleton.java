package com.montella.kaiju.rig;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny skeletal rig shared by client and server. Units are blocks, +Y up, +Z forward, +X to the creature's left.
 * The server uses it to find where mouths, tails and claws really are, the client skins meshes onto it.
 */
public final class Skeleton {
    public static final class Joint {
        public final String name;
        public final int index;
        public final Joint parent;
        public final Vector3f offset = new Vector3f();
        public float bx, by, bz;          // rest rotation
        public float rx, ry, rz;          // animated rotation (added)
        public float tx, ty, tz;          // animated translation (added)
        public float scale = 1f;
        /** Optional model-space orientation this joint is pulled towards (0..1), e.g. a tail lying on the ground. */
        public final org.joml.Quaternionf blendTarget = new org.joml.Quaternionf();
        public float blend;
        public final Matrix4f world = new Matrix4f();

        Joint(String name, int index, Joint parent) { this.name = name; this.index = index; this.parent = parent; }

        public Joint rest(float x, float y, float z) { bx = x; by = y; bz = z; return this; }

        public Vector3f point(float x, float y, float z, Vector3f out) { return world.transformPosition(x, y, z, out); }

        public Vector3f origin() { return world.getTranslation(new Vector3f()); }
    }

    public final List<Joint> joints = new ArrayList<>();
    private final Map<String, Joint> byName = new HashMap<>();
    public final Matrix4f rootMatrix = new Matrix4f();

    public Joint add(String name, String parent, float x, float y, float z) {
        Joint p = parent == null ? null : byName.get(parent);
        if (parent != null && p == null) throw new IllegalArgumentException("no joint " + parent);
        Joint j = new Joint(name, joints.size(), p);
        j.offset.set(x, y, z);
        joints.add(j);
        byName.put(name, j);
        return j;
    }

    public Joint get(String name) {
        Joint j = byName.get(name);
        if (j == null) throw new IllegalArgumentException("no joint " + name);
        return j;
    }

    public boolean has(String name) { return byName.containsKey(name); }

    public void resetPose() {
        for (Joint j : joints) { j.rx = j.ry = j.rz = 0; j.tx = j.ty = j.tz = 0; j.scale = 1f; j.blend = 0; }
        rootMatrix.identity();
    }

    /** Joints are stored parent-first, so one pass is enough. */
    public void compute() {
        for (Joint j : joints) {
            Matrix4f m = j.world;
            if (j.parent == null) m.set(rootMatrix); else m.set(j.parent.world);
            m.translate(j.offset.x + j.tx, j.offset.y + j.ty, j.offset.z + j.tz);
            float ax = j.bx + j.rx, ay = j.by + j.ry, az = j.bz + j.rz;
            if (ay != 0) m.rotateY(ay);
            if (ax != 0) m.rotateX(ax);
            if (az != 0) m.rotateZ(az);
            if (j.blend > 0) {
                Vector3f pos = m.getTranslation(new Vector3f());
                org.joml.Quaternionf q = m.getNormalizedRotation(new org.joml.Quaternionf());
                q.slerp(j.blendTarget, Math.min(1f, j.blend));
                m.translationRotateScale(pos, q, new Vector3f(1, 1, 1));
            }
            if (j.scale != 1f) m.scale(j.scale);
        }
    }
}

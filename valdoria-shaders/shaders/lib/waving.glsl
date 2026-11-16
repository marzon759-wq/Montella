// Wind: plants sway from the ground, leaves rustle, hanging vines swing.
vec3 wave(vec3 worldPos, float id, float topVertex) {
    float t = frameTimeCounter * (1.0 + rainStrength * 0.8);
    float strength = 1.0 + rainStrength * 1.5;
    vec3 off = vec3(0.0);
    if (id == 10001.0 || id == 10007.0) {
        if (topVertex > 0.5 || id == 10007.0) {
            off.x = sin(t * 1.7 + worldPos.x * 0.6 + worldPos.z * 0.2) * 0.07;
            off.z = sin(t * 1.3 + worldPos.z * 0.7) * 0.05;
            off.xz += vec2(sin(t * 3.7 + worldPos.x * 2.0), cos(t * 3.1 + worldPos.z * 2.0)) * 0.015;
        }
    } else if (id == 10002.0 || id == 10006.0) {
        off.x = sin(t * 1.1 + worldPos.x * 0.4 + worldPos.y * 0.3) * 0.025;
        off.y = sin(t * 1.9 + worldPos.z * 0.5) * 0.015;
        off.z = cos(t * 1.4 + worldPos.y * 0.3 + worldPos.z * 0.4) * 0.025;
    } else if (id == 10005.0) {
        off.x = sin(t * 0.9 + worldPos.y * 0.25 + worldPos.x) * 0.05;
        off.z = cos(t * 0.8 + worldPos.y * 0.3 + worldPos.z) * 0.05;
    }
    return off * strength;
}

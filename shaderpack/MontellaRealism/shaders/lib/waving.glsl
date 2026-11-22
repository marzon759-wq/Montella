// Wind for grass, flowers, crops and leaves. worldPos = absolute world position of the vertex.

vec3 windOffset(vec3 worldPos, float amount) {
    float t = frameTimeCounter;
    float strength = WIND_STRENGTH * (0.65 + rainStrength * 1.1);
    // slow gusts that travel across the landscape
    float gust = noise2s(worldPos.xz * 0.035 + vec2(t * 0.35, t * 0.12)) * 1.6 - 0.3;
    gust = max(gust, 0.15);
    vec2 dir = normalize(vec2(1.0, 0.4));
    float sway = sin(t * 1.9 + dot(worldPos.xz, vec2(0.65, 0.4)) * 0.9) * 0.5 + sin(t * 3.3 + worldPos.x * 1.7 + worldPos.z * 1.3) * 0.25;
    vec3 o;
    o.xz = dir * (sway + gust * 0.9) * 0.085;
    o.xz += vec2(sin(t * 4.7 + worldPos.z * 3.0), cos(t * 4.1 + worldPos.x * 3.0)) * 0.018;
    o.y = -abs(sway) * 0.02;
    return o * amount * strength;
}

vec3 leafOffset(vec3 worldPos) {
    float t = frameTimeCounter;
    float strength = WIND_STRENGTH * (0.6 + rainStrength * 1.0);
    float gust = noise2s(worldPos.xz * 0.03 + vec2(t * 0.3, t * 0.1)) + 0.3;
    vec3 o = vec3(sin(t * 1.6 + worldPos.x * 0.7 + worldPos.y * 0.5),
                  sin(t * 2.1 + worldPos.z * 0.9 + worldPos.x * 0.3) * 0.5,
                  cos(t * 1.8 + worldPos.z * 0.6 + worldPos.y * 0.4)) * 0.035;
    o += vec3(sin(t * 5.3 + worldPos.y * 4.0 + worldPos.x * 2.0), 0.0, cos(t * 4.9 + worldPos.x * 3.0)) * 0.012;
    return o * gust * strength;
}

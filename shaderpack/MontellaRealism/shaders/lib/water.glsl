// Water surface: layered moving waves -> normal. p = absolute world xz.

float waterHeight(vec2 p) {
    float t = frameTimeCounter * 0.9;
    float h = 0.0;
    vec2 q = p * 0.18;
    h += noise2s(q * 1.0 + vec2(t * 0.30, t * 0.11)) * 0.50;
    h += noise2s(q * vec2(1.9, 2.4) + vec2(-t * 0.42, t * 0.31) + 7.0) * 0.26;
    h += noise2s(q * 4.1 + vec2(t * 0.65, -t * 0.55) + 13.0) * 0.13;
    h += noise2s(q * 8.3 + vec2(-t * 0.9, t * 0.8) + 21.0) * 0.06;
    h += noise2s(q * 16.7 + vec2(t * 1.3, t * 1.1) + 33.0) * 0.03;
    return h;
}

vec3 waterNormal(vec2 p, float strength) {
    float e = 0.06;
    float h0 = waterHeight(p);
    float hx = waterHeight(p + vec2(e, 0.0));
    float hz = waterHeight(p + vec2(0.0, e));
    float k = 0.28 * WAVE_HEIGHT * strength * (1.0 + rainStrength * 0.6);
    vec3 n = normalize(vec3((h0 - hx) / e * k, 1.0, (h0 - hz) / e * k));
    return n;
}

// rain drops: expanding ripples on water and puddles
vec2 rainRipples(vec2 p) {
    vec2 g = vec2(0.0);
    for (int i = 0; i < 2; i++) {
        vec2 q = p * (2.0 + float(i) * 1.3) + float(i) * 17.0;
        vec2 cell = floor(q);
        vec2 f = fract(q);
        float h = hash12(cell + float(i) * 31.0);
        float t = fract(frameTimeCounter * 1.4 + h);
        vec2 c = vec2(hash12(cell + 3.7), hash12(cell + 9.1)) * 0.6 + 0.2;
        vec2 d = f - c;
        float r = length(d);
        float w = sin((r - t * 0.45) * 60.0) * smoothstep(t * 0.45 + 0.05, t * 0.45, r) * smoothstep(t * 0.45 - 0.12, t * 0.45, r) * (1.0 - t);
        g += normalize(d + 1e-4) * w;
    }
    return g * 0.35;
}

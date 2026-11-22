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

// ---------------------------------------------------------------------------------------------
// Gerstner waves: the water surface really moves (vertex displacement) and its normal follows.
// p = world xz, flow = stream velocity in blocks/s (zero on still water), amp = height in blocks.
// ---------------------------------------------------------------------------------------------
const vec4 WAVES[7] = vec4[](
    //   direction(rad) wavelength relAmp steepness
    vec4(0.35, 11.0, 0.30, 0.55),
    vec4(1.30,  7.3, 0.22, 0.60),
    vec4(-0.55, 5.1, 0.17, 0.65),
    vec4(2.40,  3.7, 0.12, 0.70),
    vec4(0.90,  2.6, 0.09, 0.70),
    vec4(-1.40, 1.9, 0.06, 0.65),
    vec4(2.95,  1.3, 0.04, 0.60)
);

float waveGroups(vec2 p) {
    float t = frameTimeCounter;
    return 0.55 + 0.9 * noise2s(p * 0.045 + vec2(t * 0.035, -t * 0.02));
}

vec3 gerstner(vec2 p, vec2 flow, float amp, out vec3 nrm) {
    float t = frameTimeCounter;
    vec3 disp = vec3(0.0);
    vec3 n = vec3(0.0, 1.0, 0.0);
    float g = waveGroups(p) * amp;
    vec2 q = p - flow * t;            // waves are carried by the current
    for (int i = 0; i < 7; i++) {
        vec4 w = WAVES[i];
        vec2 d = vec2(cos(w.x), sin(w.x));
        float k = TAU / w.y;
        float c = sqrt(9.81 / k) * 0.55; // deep water dispersion, slowed to block scale
        float A = g * w.z;
        float ph = k * dot(d, q) - k * c * t + float(i) * 1.7;
        float s = sin(ph), co = cos(ph);
        disp.xz += w.w * A * d * co;
        disp.y += A * s;
        n.xz -= d * k * A * co;
        n.y -= w.w * k * A * s;
    }
    nrm = normalize(n);
    return disp;
}

float waveAmplitude(bool flowing) {
    return (flowing ? 0.045 : 0.085) * WAVE_HEIGHT * (1.0 + rainStrength * 0.7);
}

// flowing water: the sloped vanilla surface points downhill -> that is the stream direction
vec2 flowFromNormal(vec3 geoN) {
    float tilt = length(geoN.xz);
    if (tilt < 0.01) return vec2(0.0);
    return geoN.xz / tilt * (1.2 + min(tilt, 0.6) * 9.0);
}

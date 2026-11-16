// Sunlight with soft shadows, sky light, block light and wet surfaces.
uniform sampler2DShadow shadowtex0;
const int shadowMapResolution = 2048;

float shadowAt(vec3 viewPos, vec3 normalView, float dither) {
    vec4 world = gbufferModelViewInverse * vec4(viewPos + normalView * 0.05 * (1.0 + length(viewPos) / 64.0), 1.0);
    vec4 sp = shadowProjection * (shadowModelView * world);
    sp.xyz = distortShadow(sp.xyz);
    sp.xyz = sp.xyz * 0.5 + 0.5;
    if (any(lessThan(sp.xy, vec2(0.0))) || any(greaterThan(sp.xy, vec2(1.0)))) return 1.0;
    float bias = 0.0006;
    float s = 0.0;
    float r = 1.4 / float(shadowMapResolution);
    float a = dither * 6.2831;
    for (int i = 0; i < 8; i++) {
        float ang = a + float(i) * 2.39996;
        float rad = sqrt((float(i) + 0.5) / 8.0);
        vec2 o = vec2(cos(ang), sin(ang)) * rad * r * 2.5;
        s += texture(shadowtex0, vec3(sp.xy + o, sp.z - bias));
    }
    return s / 8.0;
}

// lightmap: x = block light, y = sky light (0..1)
vec3 shade(vec3 albedo, vec3 viewPos, vec3 n, vec2 lm, float mat, float dither, out vec3 specDir) {
    vec3 L = normalize(shadowLightPosition);
#ifdef DIM_NO_SKY
    specDir = L;
    vec3 amb = pow(fogColor, vec3(2.2)) * 0.6 + 0.03;
    vec3 blk = vec3(1.0, 0.62, 0.32) * (pow(lm.x, 3.5) * 2.2 + pow(lm.x, 8.0) * 3.0);
    vec3 outc = albedo * (amb + blk);
    if (mat == MAT_EMISSIVE || mat == MAT_GLOWLEAVES) outc += albedo * pow(luma(albedo), 1.5) * 6.0;
    return outc;
#endif
    float NdotL = dot(n, L);
    bool foliage = mat == MAT_FOLIAGE || mat == MAT_LEAVES || mat == MAT_VINE || mat == MAT_GLOWLEAVES;
    float diffuse = foliage ? (0.45 + 0.55 * abs(NdotL)) : saturate(NdotL);
    float sh = diffuse > 0.001 && lm.y > 0.02 ? shadowAt(viewPos, n, dither) : 0.0;
    // subsurface glow through leaves and grass when the sun is behind them
    float sss = foliage ? pow(saturate(dot(normalize(viewPos), L)), 6.0) * 0.8 : 0.0;
    vec3 direct = lightColor() * (diffuse + sss) * sh * smoothstep(0.0, 0.25, lm.y + 0.05);

    float skyL = lm.y * lm.y;
    vec3 ambientSky = pow(mix(fogColor, skyColor, 0.6), vec3(2.2));
    ambientSky = mix(ambientSky, vec3(luma(ambientSky)), 0.45) * mix(0.08, 1.0, dayFactor()) * 0.5;
    ambientSky += vec3(0.02, 0.025, 0.04);
    float up = dot(n, normalize(upPosition)) * 0.3 + 0.7;
    vec3 ambient = ambientSky * skyL * up;
    vec3 block = vec3(1.0, 0.62, 0.32) * (pow(lm.x, 3.5) * 2.2 + pow(lm.x, 8.0) * 3.0);
    vec3 minimum = vec3(0.012, 0.012, 0.016);

    specDir = L;
    vec3 col = albedo * (direct + ambient + block + minimum);
    if (mat == MAT_EMISSIVE || mat == MAT_GLOWLEAVES) {
        float e = mat == MAT_EMISSIVE ? 1.0 : 0.35;
        col += albedo * pow(luma(albedo), 1.5) * 6.0 * e;
    }
    return col;
}

// Wet ground in the rain: darker albedo, sharp sky reflections handled by the caller.
float wetnessAt(vec3 n, vec2 lm) {
    float upness = saturate(dot(n, normalize(upPosition)));
    return wetness * smoothstep(0.85, 0.95, lm.y) * (0.35 + 0.65 * upness);
}

// Shadow map helpers. Requires uniforms.glsl.

vec3 distortShadow(vec3 p) {
    float d = length(p.xy);
    float f = d * SHADOW_DISTORT + (1.0 - SHADOW_DISTORT);
    p.xy /= f;
    p.z *= 0.2;
    return p;
}

#ifndef SHADOW_PASS
uniform sampler2DShadow shadowtex1;
uniform sampler2D shadowtex0;
uniform sampler2D shadowcolor0;

const vec2 poisson16[16] = vec2[](
    vec2(-0.94201624, -0.39906216), vec2(0.94558609, -0.76890725), vec2(-0.09418410, -0.92938870), vec2(0.34495938, 0.29387760),
    vec2(-0.91588581, 0.45771432), vec2(-0.81544232, -0.87912464), vec2(-0.38277543, 0.27676845), vec2(0.97484398, 0.75648379),
    vec2(0.44323325, -0.97511554), vec2(0.53742981, -0.47373420), vec2(-0.26496911, -0.41893023), vec2(0.79197514, 0.19090188),
    vec2(-0.24188840, 0.99706507), vec2(-0.81409955, 0.91437590), vec2(0.19984126, 0.78641367), vec2(0.14383161, -0.14100790)
);

// playerPos = position relative to the camera (world aligned). nW = world normal.
// returns rgb light transmittance (colored through glass/water) ; w = 1 if a water surface is above (for caustics)
vec4 shadowLight(vec3 playerPos, vec3 nW, float dither, float sss, out float waterDepth) {
    waterDepth = 0.0;
    vec3 L = normalize(mat3(gbufferModelViewInverse) * shadowLightPosition);
    float dist = length(playerPos);
    float NdotL = dot(nW, L);
    // normal offset grows with distance (texel size grows with the distortion)
    float bias = 0.06 + dist * 0.0035;
    vec3 p = playerPos + nW * bias * (sss > 0.0 ? 0.6 : 1.0);
    vec3 sp = (shadowProjection * (shadowModelView * vec4(p, 1.0))).xyz;
    float df = length(sp.xy) * SHADOW_DISTORT + (1.0 - SHADOW_DISTORT);
    vec3 s = distortShadow(sp) * 0.5 + 0.5;
    if (any(lessThan(s.xy, vec2(0.0))) || any(greaterThan(s.xy, vec2(1.0)))) return vec4(1.0, 1.0, 1.0, 0.0);
    s.z -= 0.00006 * df;
    float texel = 1.0 / float(shadowMapResolution);
    float radius = texel * (1.2 + sss * 3.0) * SHADOW_SOFTNESS * df * 1.1;
    float a = dither * TAU;
    mat2 rot = mat2(cos(a), -sin(a), sin(a), cos(a));
    float lit = 0.0;
    for (int i = 0; i < SHADOW_FILTER; i++) {
        vec2 o = rot * poisson16[i & 15] * radius * (1.0 + float(i >> 4) * 0.5);
        lit += texture(shadowtex1, vec3(s.xy + o, s.z));
    }
    lit /= float(SHADOW_FILTER);
    vec3 col = vec3(lit);
    float water = 0.0;
#ifdef COLORED_SHADOWS
    if (lit > 0.0) {
        float d0 = texture(shadowtex0, s.xy).r;
        if (d0 < s.z - 0.0001) {
            vec4 sc = texture(shadowcolor0, s.xy);
            if (sc.a < 0.02) { // water marker
                water = 1.0;
                waterDepth = (s.z - d0) * 2.0 / shadowProjection[2][2] * -5.0;
            } else {
                vec3 tint = toLinear(sc.rgb);
                col *= mix(vec3(1.0), tint * (1.0 - sc.a * 0.4), sat(sc.a * 1.5));
            }
        }
    }
#endif
    return vec4(col, water);
}
#endif

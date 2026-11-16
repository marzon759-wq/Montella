// Water (and other translucent blocks): waves, fresnel reflections of the sky, sun glints, depth-tinted colour,
// shore foam and white frothy waterfalls.
#include "/lib/common.glsl"
#include "/lib/sky.glsl"
#include "/lib/lighting.glsl"

#ifdef VSH
in vec4 mc_Entity;
out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 viewPos;
out vec3 worldPos;
out vec3 normalW;
flat out float isWater;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    worldPos = (gbufferModelViewInverse * vp).xyz + cameraPosition;
    isWater = mc_Entity.x == 10003.0 ? 1.0 : 0.0;
    normalW = mat3(gbufferModelViewInverse) * normalize(gl_NormalMatrix * gl_Normal);
    if (isWater > 0.5 && normalW.y > 0.5) {
        // gentle swell of the surface itself
        vp.y += 0.0;
        worldPos.y += (sin(frameTimeCounter * 1.2 + worldPos.x * 0.7 + worldPos.z * 0.4) * 0.03 - 0.04);
        vp = gbufferModelView * vec4(worldPos - cameraPosition, 1.0);
    }
    viewPos = vp.xyz;
    gl_Position = gl_ProjectionMatrix * vp;
}
#endif

#ifdef FSH
uniform sampler2D gtexture;
uniform sampler2D depthtex1;
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 viewPos;
in vec3 worldPos;
in vec3 normalW;
flat in float isWater;

/* RENDERTARGETS: 0,3 */
layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outInfo;

float waveHeight(vec2 p) {
    float t = frameTimeCounter;
    float h = 0.0;
    h += sin(dot(p, vec2(0.8, 0.6)) * 1.1 + t * 1.3) * 0.18;
    h += sin(dot(p, vec2(-0.5, 0.9)) * 1.7 + t * 1.7) * 0.10;
    h += (noise2(p * 0.9 + vec2(t * 0.35, t * 0.2)) - 0.5) * 0.30;
    h += (noise2(p * 2.6 - vec2(t * 0.6, -t * 0.4)) - 0.5) * 0.12;
    h += (noise2(p * 6.0 + vec2(t * 1.1, t * 0.9)) - 0.5) * 0.04;
    return h;
}

float linDepth(float d) { return 2.0 * near * far / (far + near - (d * 2.0 - 1.0) * (far - near)); }

void main() {
    vec4 tex = texture(gtexture, texcoord);
    vec2 lm = saturate((lmcoord - 1.0 / 32.0) * 16.0 / 15.0);
    vec3 V = normalize(viewPos);
    vec2 screen = gl_FragCoord.xy / vec2(viewWidth, viewHeight);
    float dither = bayer8(gl_FragCoord.xy);

    if (isWater < 0.5) {
        // stained glass, ice, slime, honey...: simply lit
        vec4 c = tex * glcolor;
        vec3 spec;
        vec3 col = shade(pow(c.rgb, vec3(2.2)), viewPos, normalize(mat3(gbufferModelView) * normalW), lm, MAT_NONE, dither, spec);
        outColor = vec4(col, c.a);
        outInfo = vec4(0.0);
        return;
    }

    bool falling = abs(normalW.y) < 0.6;
    vec3 nW;
    float foam = 0.0;
    if (!falling) {
        vec2 p = worldPos.xz;
        float e = 0.08;
        float h0 = waveHeight(p);
        float hx = waveHeight(p + vec2(e, 0.0));
        float hz = waveHeight(p + vec2(0.0, e));
        float strength = mix(0.55, 0.12, saturate(length(viewPos) / 96.0)) * (1.0 + rainStrength);
        nW = normalize(vec3(-(hx - h0) / e * strength, 1.0, -(hz - h0) / e * strength));
        if (normalW.y < 0.0) nW = -nW;
        // raindrop ripples
        if (rainStrength > 0.01) {
            vec2 cell = floor(worldPos.xz * 1.5);
            float r = hash12(cell + floor(frameTimeCounter * 1.3));
            vec2 c = (cell + 0.5) / 1.5;
            float ring = sin(length(worldPos.xz - c) * 30.0 - frameTimeCounter * 12.0) * exp(-length(worldPos.xz - c) * 5.0);
            nW.xz += ring * 0.15 * rainStrength * step(0.6, r);
            nW = normalize(nW);
        }
        // foam where the water is shallow (shores, rocks)
        float opaque = linDepth(texture(depthtex1, screen).r);
        float here = linDepth(gl_FragCoord.z);
        float depthDiff = opaque - here;
        foam = saturate(1.0 - depthDiff / 0.6) * (0.6 + 0.4 * noise2(worldPos.xz * 3.0 + frameTimeCounter * 0.8));
    } else {
        // waterfall: streaks rushing down, broken up and frothy white
        vec3 side = normalize(vec3(normalW.x, 0.0, normalW.z));
        vec3 tangent = vec3(-side.z, 0.0, side.x);
        float along = dot(worldPos, tangent);
        float flow = worldPos.y + frameTimeCounter * 6.0;
        float streak = noise2(vec2(along * 3.0, flow * 0.35)) * 0.6 + noise2(vec2(along * 9.0, flow * 1.1)) * 0.4;
        nW = normalize(side + tangent * (streak - 0.5) * 0.9 + vec3(0.0, (noise2(vec2(along * 5.0, flow)) - 0.5) * 0.4, 0.0));
        foam = smoothstep(0.35, 0.85, streak) * 0.85 + 0.15;
    }
    vec3 n = normalize(mat3(gbufferModelView) * nW);

    // colour of the water body: the game's (biome) water tint, deepened with depth
    float opaqueDepth = linDepth(texture(depthtex1, screen).r);
    float thickness = max(0.0, opaqueDepth - linDepth(gl_FragCoord.z));
    vec3 tint = pow(glcolor.rgb, vec3(2.2));
    float absorb = 1.0 - exp(-thickness * 0.28);
    vec3 skyAmb = pow(mix(fogColor, skyColor, 0.6), vec3(2.2)) * mix(0.08, 1.0, dayFactor()) * lm.y * lm.y + 0.01;
    vec3 body = tint * skyAmb * 0.55 * (0.4 + 0.6 * absorb);

    float fres = 0.02 + 0.98 * pow(1.0 - saturate(dot(-V, n)), 5.0);
    vec3 R = reflect(V, n);
    vec3 refl = fullSky(R) * smoothstep(0.3, 0.9, lm.y);
    float sh = shadowAt(viewPos, n, dither);
    vec3 L = normalize(shadowLightPosition);
    vec3 glint = lightColor() * pow(saturate(dot(R, L)), 900.0) * 40.0 * sh;

    vec3 col = mix(body, refl, fres) + glint;
    float alpha = mix(0.35, 0.92, max(absorb, fres));
    // foam is lit like a rough white surface
    vec3 foamCol = vec3(0.9) * (lightColor() * saturate(dot(n, L) * 0.5 + 0.5) * sh + skyAmb * 1.4);
    col = mix(col, foamCol, foam * (falling ? 0.9 : 0.75));
    alpha = max(alpha, foam * 0.95);
    if (falling) alpha = max(alpha, 0.8);

    outColor = vec4(col, alpha);
    outInfo = vec4(encodeNormal(n), 1.0, fres * (1.0 - foam));
}
#endif

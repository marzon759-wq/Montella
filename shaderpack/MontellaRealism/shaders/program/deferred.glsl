// Deferred lighting for opaque geometry + sky, sun, stars and volumetric clouds.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH
out vec2 uv;
flat out vec3 directCol;
flat out vec3 ambientCol;
flat out vec3 sunCol;
void main() {
    uv = gl_MultiTexCoord0.xy;
    lightColors(directCol, ambientCol);
    sunCol = sunIlluminance();
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 uv;
flat in vec3 directCol;
flat in vec3 ambientCol;
flat in vec3 sunCol;

uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D depthtex0;

#include "/lib/shadows.glsl"
#include "/lib/clouds.glsl"

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

vec3 viewFromDepth(vec2 p, float d) { return projectDivide(gbufferProjectionInverse, vec3(p, d) * 2.0 - 1.0); }

float ssao(vec3 vpos, vec3 nView, float dither) {
    float occ = 0.0;
    float radius = 0.55;
    int samples = 8;
    for (int i = 0; i < samples; i++) {
        float fi = float(i) + dither;
        float r = sqrt(fi / float(samples)) * radius;
        float a = fi * 2.39996;
        vec3 t = normalize(cross(nView, abs(nView.y) < 0.9 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0)));
        vec3 b = cross(nView, t);
        vec3 s = vpos + (t * cos(a) + b * sin(a)) * r + nView * r * 0.6;
        vec3 sp = projectDivide(gbufferProjection, s) * 0.5 + 0.5;
        if (sp.x < 0.0 || sp.x > 1.0 || sp.y < 0.0 || sp.y > 1.0) continue;
        float d = texture(depthtex0, sp.xy).r;
        vec3 o = viewFromDepth(sp.xy, d);
        float diff = o.z - s.z;
        occ += step(0.03, diff) * smoothstep(1.0, 0.0, diff / (radius * 2.5));
    }
    return 1.0 - occ / float(samples) * 0.75;
}

void main() {
    float depth = texture(depthtex0, uv).r;
    vec3 viewPos = viewFromDepth(uv, depth);
    vec3 playerPos = mat3(gbufferModelViewInverse) * viewPos + gbufferModelViewInverse[3].xyz;
    vec3 V = normalize(playerPos);
    float dither = ign(gl_FragCoord.xy);
    vec3 L = lightDirection();
    vec3 sd = sunDirection();

    if (depth >= 1.0) {
#if WORLD == -1
        outColor = vec4(toLinear(fogColor) * 1.2 + vec3(0.05, 0.015, 0.01), 1.0);
        return;
#elif WORLD == 1
        vec3 endSky = vec3(0.012, 0.008, 0.02) + starField(normalize(vec3(V.x, abs(V.y) + 0.2, V.z))) * 6.0;
        outColor = vec4(endSky, 1.0);
        return;
#else
        vec3 sky = skyRadiance(V, 16);
        sky += sunDisk(V, sd);
#ifdef STARS
        sky += starField(V);
#endif
        // vanilla moon texture (drawn by gbuffers_skytextured)
        vec3 moon = texture(colortex0, uv).rgb;
        sky += toLinear(moon) * 0.9 * NIGHT_BRIGHTNESS * (1.0 - rainStrength) * atmosTransmittance(max(V.y, 0.01));
        vec3 cloudSun = sd.y > -0.05 ? sunCol * 3.0 : moonIlluminance() * 1.5;
        vec3 cloudL = sd.y > -0.05 ? sd : -sd;
        vec3 cloudAmb = ambientCol * 0.55;
        vec4 ci = cirrusClouds(V, cloudSun, cloudAmb, cloudL);
        sky = sky * ci.a + ci.rgb;
#ifdef VOLUMETRIC_CLOUDS
        vec4 cl = volumetricClouds(V, cloudSun, cloudAmb, cloudL, dither, 1e9);
        sky = sky * cl.a + cl.rgb;
#endif
        if (V.y < 0.0) sky = mix(sky, ambientCol * 0.22, sat(-V.y * 2.0));
        outColor = vec4(sky, 1.0);
        return;
#endif
    }

    vec4 albedoRaw = texture(colortex0, uv);
    vec4 nl = texture(colortex1, uv);
    vec4 md = texture(colortex2, uv);
    int mat = int(md.r * 255.0 + 0.5);
    vec3 albedo = toLinear(albedoRaw.rgb);
    vec3 n = octDecode(nl.xy);
    vec2 lm = nl.zw;
    float vao = md.g;
    float emission = md.b;
    bool hand = depth < 0.56;

    float skyLm = lm.y;
    float NdotL = dot(n, L);
    bool foliage = mat == MAT_PLANT || mat == MAT_LEAVES;
    float sss = 0.0;
#ifdef SUBSURFACE
    if (foliage) sss = mat == MAT_PLANT ? 0.85 : 0.65;
    if (mat == MAT_SNOW) sss = 0.25;
#endif

    vec3 shadow = vec3(0.0);
    float waterAbove = 0.0, waterDepth = 0.0;
#if WORLD == 0
    if ((NdotL > 0.0 || sss > 0.0) && !hand) {
        vec4 s = shadowLight(playerPos, n, dither, sss, waterDepth);
        // fade to a lightmap-based guess beyond the shadow distance
        float fade = smoothstep(shadowDistance * 0.8, shadowDistance * 0.98, length(playerPos.xz));
        shadow = mix(s.rgb, vec3(smoothstep(0.85, 0.98, skyLm)), fade);
        waterAbove = s.a * (1.0 - fade);
    } else if (hand) {
        shadow = vec3(smoothstep(0.85, 0.98, skyLm));
    }
    shadow *= cloudShadow(playerPos + cameraPosition, L);
#endif

    // direct light: lambert with a little wrap, plus translucency for leaves & grass
    float diffuse = sat(NdotL);
    float VdotL = dot(V, L);
    vec3 direct = vec3(0.0);
#if WORLD == 0
    direct = directCol * shadow * diffuse;
    if (sss > 0.0) {
        float back = pow(sat(VdotL), 4.0) * 1.6 + 0.35;
        direct += directCol * shadow * sss * back * mix(vec3(1.0), albedo * 2.2, 0.6) * 0.55;
    }
    if (waterAbove > 0.5) {
        // light through a water surface: absorb + caustics
        vec3 wp = playerPos + cameraPosition;
        vec3 absorb = exp(-vec3(0.35, 0.09, 0.07) * max(waterDepth, 0.0) * 0.8);
        float c = 0.0;
#ifdef WATER_CAUSTICS
        vec2 q = wp.xz - L.xz / max(L.y, 0.1) * wp.y * 0.0;
        float t = frameTimeCounter * 0.6;
        float c1 = noise2s(q * 0.9 + vec2(t, t * 0.6));
        float c2 = noise2s(q * 1.3 - vec2(t * 0.7, -t));
        c = pow(sat(1.0 - abs(c1 - c2) * 2.0), 6.0);
        c = c * 2.2 + 0.35;
#else
        c = 1.0;
#endif
        direct *= absorb * mix(1.0, c, sat(waterDepth * 0.5));
    }
#endif

    // ambient: sky light from above, bounced light from the ground below
    float ao = pow(vao, 1.4);
#ifdef SSAO
    if (!hand) ao *= ssao(viewPos, mat3(gbufferModelView) * n, dither);
#endif
    vec3 skyAmb = ambientCol * (skyLm * skyLm) * (0.75 + 0.25 * n.y);
    vec3 bounce = directCol * shadow * 0.0 + directCol * 0.035 * skyLm * sat(-n.y * 0.5 + 0.5);
    vec3 ambient = (skyAmb + bounce) * ao * (foliage ? 1.3 : 1.0);
#if WORLD == -1
    ambient = (toLinear(fogColor) * 0.8 + vec3(0.16, 0.085, 0.06)) * ao;
#elif WORLD == 1
    ambient = vec3(0.10, 0.085, 0.13) * ao;
    direct = vec3(0.0);
#endif

    // torches and other block light: warm, inverse-square-ish falloff
    float bl = lm.x;
    vec3 blockCol = vec3(1.0, 0.56, 0.24) * (pow(bl, 5.0) * 2.2 + pow(bl, 2.0) * 0.12) * BLOCK_LIGHT;
    ambient += blockCol * mix(ao, 1.0, 0.5);
    ambient += vec3(MIN_LIGHT) * ao * vec3(0.8, 0.9, 1.1);
    ambient += vec3(0.25) * nightVision;

    vec3 col = albedo * (direct + ambient);
    if (emission > 0.0) col += albedo * emission * (mat == MAT_LAVA ? 2.2 : 4.0) * BLOCK_LIGHT;
    // soft specular sheen on wet / snowy surfaces is added in composite
    outColor = vec4(col, 1.0);
}
#endif

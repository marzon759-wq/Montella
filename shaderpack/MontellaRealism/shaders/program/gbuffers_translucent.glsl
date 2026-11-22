// Translucent terrain: water is only marked here (shaded in composite), glass / ice / slime are lit forward.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH
in vec4 mc_Entity;
out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 normalW;
out vec3 playerPos;
flat out int id;
flat out vec3 directCol;
flat out vec3 ambientCol;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = sat(((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy - 1.0 / 32.0) * (32.0 / 30.0));
    glcolor = gl_Color;
    id = int(mc_Entity.x + 0.5);
    vec3 viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    playerPos = mat3(gbufferModelViewInverse) * viewPos + gbufferModelViewInverse[3].xyz;
    normalW = normalize(mat3(gbufferModelViewInverse) * (gl_NormalMatrix * gl_Normal));
    lightColors(directCol, ambientCol);
    gl_Position = gl_ProjectionMatrix * vec4(viewPos, 1.0);
}
#endif

#ifdef FSH
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 normalW;
in vec3 playerPos;
flat in int id;
flat in vec3 directCol;
flat in vec3 ambientCol;

uniform sampler2D gtexture;
#include "/lib/shadows.glsl"
#include "/lib/water.glsl"

/* RENDERTARGETS: 0,1,3 */
layout(location = 0) out vec4 outColor;
layout(location = 1) out vec4 outNormal;
layout(location = 2) out vec4 outWater;

void main() {
    vec3 n = normalize(normalW);
    if (!gl_FrontFacing) n = -n;
    vec3 worldPos = playerPos + cameraPosition;
#ifdef ENTITY_TRANSLUCENT
    bool isWater = false;
#else
    bool isWater = id == 10004;
#endif
    if (isWater) {
        float foam = 0.0;
        vec4 tex = texture(gtexture, texcoord);
        if (abs(n.y) > 0.5) {
#ifdef WATER_WAVES
            vec3 wn = waterNormal(worldPos.xz, 1.0);
            if (rainStrength > 0.01 && biome_precipitation == 1) {
                vec2 r = rainRipples(worldPos.xz) * rainStrength * sat(lmcoord.y * 4.0 - 3.0);
                wn = normalize(wn + vec3(r.x, 0.0, r.y));
            }
            n = n.y > 0.0 ? wn : vec3(wn.x, -wn.y, wn.z);
#endif
            // flowing water on a slope shows streaks in the vanilla texture: let them foam a little
            foam = sat((luma(tex.rgb) - 0.62) * 3.0) * 0.25;
        } else {
            // falling water (waterfalls): fast streaks and white water
            vec3 side = abs(n.x) > 0.5 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
            float u = dot(worldPos, side);
            float v = worldPos.y + frameTimeCounter * 4.5;
            float s1 = noise2s(vec2(u * 3.0, v * 0.35));
            float s2 = noise2s(vec2(u * 7.0 + 11.0, v * 0.9 + 3.0));
            vec3 bump = side * (s1 - 0.5) * 0.6 + vec3(0.0, (s2 - 0.5) * 0.3, 0.0);
            n = normalize(n + bump);
#ifdef WATERFALL_FOAM
            foam = sat(s1 * 0.7 + s2 * 0.6 - 0.15);
            foam = 0.55 + foam * 0.45;
#endif
        }
        outColor = vec4(0.0);
        outNormal = vec4(octEncode(n), lmcoord);
        // rg + (b - 1) = biome water tint, b >= 1 flags water, a = foam
        outWater = vec4(glcolor.rg, 1.0 + glcolor.b, foam);
        return;
    }
    vec4 albedo = texture(gtexture, texcoord) * glcolor;
    if (albedo.a < 0.02) discard;
    vec3 a = toLinear(albedo.rgb);
    vec3 L = lightDirection();
    float NdotL = sat(dot(n, L));
    float wd;
    vec3 sh = NdotL > 0.0 ? shadowLight(playerPos, n, ign(gl_FragCoord.xy), 0.0, wd).rgb : vec3(0.0);
    vec3 light = directCol * sh * NdotL + ambientCol * lmcoord.y * lmcoord.y * (0.65 + 0.35 * n.y);
    light += vec3(1.0, 0.62, 0.32) * pow(lmcoord.x, 4.0) * 1.6 * BLOCK_LIGHT + MIN_LIGHT;
    vec3 col = a * light;
    // glass reflects the sky a little
    vec3 V = normalize(playerPos);
    float f = 0.04 + 0.96 * pow(1.0 - sat(dot(-V, n)), 5.0);
    vec3 R = reflect(V, n);
    col = mix(col, skyRadiance(R, 6) * lmcoord.y, f * 0.6);
    float alpha = mix(albedo.a, 1.0, f * 0.5);
    outColor = vec4(col, alpha);
    outNormal = vec4(octEncode(n), lmcoord);
    outWater = vec4(0.0);
}
#endif

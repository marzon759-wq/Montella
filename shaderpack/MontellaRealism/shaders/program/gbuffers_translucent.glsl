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
    bool isWater = id == 10004 || id == 10014 || id == 10024;
#endif
    if (isWater) {
        float foam = 0.0;
        float t = frameTimeCounter;
        bool falling = abs(n.y) < 0.5;
        if (!falling) {
            vec3 wn = vec3(0.0, 1.0, 0.0);
#ifdef WATER_WAVES
            wn = waterNormal(worldPos.xz, 1.0);
#endif
            if (id != 10004) {
                // flowing water: the vanilla flow sprite scrolls along +v, so the texture frame gives the flow direction
                vec3 dpx = dFdx(worldPos), dpy = dFdy(worldPos);
                vec2 dtx = dFdx(texcoord), dty = dFdy(texcoord);
                vec3 B = cross(n, dpx) * dty.y + cross(dpy, n) * dtx.y;
                vec2 flow = length(B.xz) > 1e-6 ? normalize(B.xz) : vec2(0.0, 1.0);
                vec2 side = vec2(-flow.y, flow.x);
                float speed = id == 10024 ? 3.5 : 1.6;
                vec2 q = vec2(dot(worldPos.xz, side), dot(worldPos.xz, flow));
                float a1 = noise2s(vec2(q.x * 2.2, q.y * 0.55 - t * speed * 0.55));
                float a2 = noise2s(vec2(q.x * 4.7 + 9.0, q.y * 1.3 - t * speed * 1.3));
                float a3 = noise2s(vec2(q.x * 9.0 + 3.0, q.y * 2.6 - t * speed * 2.6));
                vec2 g = side * (a1 - 0.5) * 0.5 + flow * (a2 - 0.5) * 0.35 + side * (a3 - 0.5) * 0.25;
                wn = normalize(wn + vec3(g.x, 0.0, g.y));
#ifdef WATERFALL_FOAM
                // churned white water where the stream is fast (lip of the fall, slopes)
                float churn = id == 10024 ? 0.9 : 0.35;
                foam = sat((a1 * 0.5 + a2 * 0.35 + a3 * 0.25 - 0.62 + churn * 0.35) * 3.0) * churn;
#endif
            }
            if (rainStrength > 0.01 && biome_precipitation == 1) {
                vec2 r = rainRipples(worldPos.xz) * rainStrength * sat(lmcoord.y * 4.0 - 3.0);
                wn = normalize(wn + vec3(r.x, 0.0, r.y));
            }
            n = n.y > 0.0 ? wn : vec3(wn.x, -wn.y, wn.z);
        } else {
            // falling sheet: layers of streaks racing down at different speeds, never one texture sliding
            vec3 side = abs(n.x) > 0.5 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
            float u = dot(worldPos, side);
            float y = worldPos.y;
            float l1 = noise2s(vec2(u * 3.1, (y + t * 7.0) * 0.16));
            float l2 = noise2s(vec2(u * 6.3 + 17.0, (y + t * 9.5) * 0.32));
            float l3 = noise2s(vec2(u * 13.0 + 41.0, (y + t * 12.0) * 0.7));
            float l4 = noise2s(vec2(u * 26.0 + 7.0, (y + t * 14.0) * 1.6));
            float streak = l1 * 0.42 + l2 * 0.28 + l3 * 0.18 + l4 * 0.12;
            // clumps: falling water breaks into fast white packets
            float clump = noise2s(vec2(u * 1.3 + 5.0, (y + t * 8.0) * 0.45));
            vec3 bump = side * ((l2 - 0.5) * 0.9 + (l4 - 0.5) * 0.5) + vec3(0.0, (l3 - 0.5) * 0.25, 0.0);
            n = normalize(n + bump);
#ifdef WATERFALL_FOAM
            foam = smoothstep(0.36, 0.70, streak + (clump - 0.5) * 0.5);
            foam = foam * 0.9 + l4 * 0.1;
#endif
        }
        outColor = vec4(0.0);
        outNormal = vec4(octEncode(n), lmcoord);
        // rg = biome tint, b = type (1 + tint.b on surfaces, 3 + tint.b on falling sheets), a = foam
        outWater = vec4(glcolor.rg, (falling ? 3.0 : 1.0) + glcolor.b, foam);
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

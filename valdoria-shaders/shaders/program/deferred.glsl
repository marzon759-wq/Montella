// Lights every opaque pixel and paints the sky behind them.
#include "/lib/common.glsl"
#include "/lib/sky.glsl"
#include "/lib/lighting.glsl"

#ifdef VSH
out vec2 uv;
void main() { uv = gl_MultiTexCoord0.xy; gl_Position = ftransform(); }
#endif

#ifdef FSH
/*
const int colortex0Format = RGBA16F;
const int colortex1Format = RGBA16;
const int colortex2Format = RGBA8;
const int colortex3Format = RGBA16;
const int colortex4Format = RGBA16F;
const vec4 colortex3ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
const bool colortex3Clear = true;
const float sunPathRotation = -28.0;
const int shadowMapResolution = 2048;
const float shadowDistance = 128.0;
const bool shadowHardwareFiltering = true;
const float ambientOcclusionLevel = 1.0;
*/
uniform sampler2D colortex0, colortex1, colortex2, depthtex0;
in vec2 uv;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

vec3 applyFog(vec3 col, vec3 viewPos, vec3 dir) {
    float dist = length(viewPos);
    float fogFar = fogEnd > 1.0 ? min(fogEnd, far) : far;
    // atmospheric perspective, stronger in rain and when the game's own fog is pulled in (mist, dust, modded fog)
    float density = (1.4 + rainStrength * 3.0) / max(fogFar, 16.0);
    float atm = 1.0 - exp(-dist * density * 0.55);
    float border = smoothstep(fogFar * 0.7, fogFar, dist);
    vec3 fogCol = skyGradient(dir);
    if (isEyeInWater == 1) return col;
    return mix(col, fogCol, saturate(max(atm * 0.85, border)));
}

void main() {
    float depth = texture(depthtex0, uv).r;
    vec3 viewPos = toView(uv, depth);
    vec3 dir = normalize(viewPos);
    if (depth >= 1.0) {
        outColor = vec4(fullSky(dir), 1.0);
        return;
    }
    vec3 albedo = pow(texture(colortex0, uv).rgb, vec3(2.2));
    vec4 nb = texture(colortex1, uv);
    vec3 n = decodeNormal(nb.xy);
    float mat = floor(nb.z * 255.0 + 0.5);
    vec2 lm = texture(colortex2, uv).rg;
    float dither = fract(bayer8(gl_FragCoord.xy) + float(frameCounter % 8) * 0.618);

    // rain: darker, wet surfaces with puddles on flat ground
    float wet = 0.0;
    if (mat != MAT_HAND && mat != MAT_ENTITY) {
        wet = wetnessAt(n, lm);
        vec3 wp = (gbufferModelViewInverse * vec4(viewPos, 1.0)).xyz + cameraPosition;
        float puddle = smoothstep(0.55, 0.72, fbm2(wp.xz * 0.12, 3)) * saturate(dot(n, normalize(upPosition)) - 0.9) * 10.0;
        wet = max(wet, puddle * wetness * smoothstep(0.85, 0.95, lm.y));
        albedo *= mix(1.0, 0.55, wet);
    }

    vec3 L;
    vec3 col = shade(albedo, viewPos, n, lm, mat, dither, L);
    if (wet > 0.01) {
        float fres = 0.04 + 0.96 * pow(1.0 - saturate(dot(-dir, n)), 5.0);
        vec3 R = reflect(dir, n);
        col += skyGradient(R) * fres * wet * 0.8 * lm.y;
    }
    col = applyFog(col, viewPos, dir);
    outColor = vec4(col, 1.0);
}
#endif

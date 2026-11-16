// Screen-space reflections on water, sunbeams through fog and leaves, underwater haze.
#include "/lib/common.glsl"
#include "/lib/sky.glsl"

#ifdef VSH
out vec2 uv;
void main() { uv = gl_MultiTexCoord0.xy; gl_Position = ftransform(); }
#endif

#ifdef FSH
uniform sampler2D colortex0, colortex3, depthtex0, depthtex1;
uniform sampler2DShadow shadowtex0;
const int shadowMapResolution = 2048;
in vec2 uv;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

vec3 ssr(vec3 viewPos, vec3 n, out float hit) {
    vec3 R = normalize(reflect(normalize(viewPos), n));
    hit = 0.0;
    if (R.z > 0.0 && viewPos.z > -2.0) return vec3(0.0);
    vec3 start = viewPos;
    float stepLen = 0.6;
    vec3 p = start;
    float dither = bayer8(gl_FragCoord.xy);
    p += R * stepLen * dither;
    for (int i = 0; i < 28; i++) {
        p += R * stepLen;
        stepLen *= 1.18;
        vec3 sp = toScreen(p);
        if (any(lessThan(sp.xy, vec2(0.0))) || any(greaterThan(sp.xy, vec2(1.0)))) break;
        float d = texture(depthtex1, sp.xy).r;
        vec3 scene = toView(sp.xy, d);
        float diff = scene.z - p.z;
        if (diff > 0.0 && diff < stepLen * 2.5 && d < 1.0) {
            // refine
            vec3 a = p - R * stepLen, b = p;
            for (int j = 0; j < 5; j++) {
                vec3 m = (a + b) * 0.5;
                vec3 ms = toScreen(m);
                if (toView(ms.xy, texture(depthtex1, ms.xy).r).z > m.z) b = m; else a = m;
            }
            vec3 hs = toScreen(b);
            vec2 edge = smoothstep(0.0, 0.08, hs.xy) * smoothstep(1.0, 0.92, hs.xy);
            hit = edge.x * edge.y;
            return texture(colortex0, hs.xy).rgb;
        }
    }
    return vec3(0.0);
}

void main() {
    vec3 col = texture(colortex0, uv).rgb;
    float depth0 = texture(depthtex0, uv).r;
    vec3 viewPos = toView(uv, depth0);
    vec4 info = texture(colortex3, uv);

    if (info.b > 0.5 && isEyeInWater == 0) {
        vec3 n = decodeNormal(info.xy);
        float hit;
        vec3 refl = ssr(viewPos, n, hit);
        if (hit > 0.0) {
            vec3 skyR = fullSky(reflect(normalize(viewPos), n));
            col += (refl - skyR * 0.9) * info.a * hit;
            col = max(col, 0.0);
        }
    }

    // volumetric sunbeams: march the view ray through the shadow map
    float dist = min(length(viewPos), depth0 >= 1.0 ? 96.0 : length(viewPos));
    dist = min(dist, 96.0);
    vec3 dirW = mat3(gbufferModelViewInverse) * normalize(viewPos);
    vec3 L = normalize(shadowLightPosition);
    float VdotL = dot(normalize(viewPos), L);
    float phase = 0.25 + 0.6 * pow(saturate(VdotL), 6.0) + 0.25 * pow(saturate(VdotL), 2.0);
    float fogFar = fogEnd > 1.0 ? min(fogEnd, far) : far;
    float density = 0.0045 + rainStrength * 0.006 + 0.6 / max(fogFar, 16.0);
    if (isEyeInWater == 1) density = 0.03;
    float lit = 0.0;
    float dither = fract(bayer8(gl_FragCoord.xy) + float(frameCounter % 8) * 0.618);
    const int STEPS = 12;
    for (int i = 0; i < STEPS; i++) {
        float t = (float(i) + dither) / float(STEPS);
        t = t * t;
        vec3 wp = dirW * dist * t;
        vec4 sp = shadowProjection * (shadowModelView * vec4(wp, 1.0));
        sp.xyz = distortShadow(sp.xyz) * 0.5 + 0.5;
        lit += texture(shadowtex0, vec3(sp.xy, sp.z - 0.0006));
    }
    lit /= float(STEPS);
    float sky = float(eyeBrightnessSmooth.y) / 240.0;
    vec3 beams = lightColor() * lit * phase * (1.0 - exp(-dist * density)) * 0.14 * (0.25 + 0.75 * sky);
    vec3 tintFog = pow(fogColor, vec3(2.2));
    beams *= mix(vec3(1.0), tintFog * 2.0 + 0.3, 0.35);
    col += beams;

    if (isEyeInWater == 1) {
        vec3 waterFog = pow(fogColor, vec3(2.2)) * 0.4 * (0.2 + 0.8 * sky);
        col = mix(col, waterFog, 1.0 - exp(-length(viewPos) * 0.08));
    }
    outColor = vec4(col, 1.0);
}
#endif

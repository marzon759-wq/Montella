// Valdoria Shaders — shared uniforms and helpers.
uniform mat4 gbufferModelView, gbufferModelViewInverse, gbufferProjection, gbufferProjectionInverse;
uniform mat4 shadowModelView, shadowProjection;
uniform vec3 sunPosition, moonPosition, shadowLightPosition, upPosition, cameraPosition;
uniform vec3 skyColor, fogColor;
uniform float frameTimeCounter, rainStrength, wetness, viewWidth, viewHeight, near, far, sunAngle;
uniform float fogEnd;
uniform int isEyeInWater, worldTime, frameCounter;
uniform ivec2 eyeBrightnessSmooth;

// Material ids written to colortex1.a (see block.properties)
#define MAT_NONE 0.0
#define MAT_FOLIAGE 1.0
#define MAT_LEAVES 2.0
#define MAT_WATER 3.0
#define MAT_EMISSIVE 4.0
#define MAT_VINE 5.0
#define MAT_GLOWLEAVES 6.0
#define MAT_HAND 7.0
#define MAT_ENTITY 8.0

const float PI = 3.14159265;

float saturate(float x) { return clamp(x, 0.0, 1.0); }
vec3 saturate(vec3 x) { return clamp(x, 0.0, 1.0); }
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

// Octahedral normal encoding into two 0..1 channels
vec2 encodeNormal(vec3 n) {
    n /= abs(n.x) + abs(n.y) + abs(n.z);
    vec2 e = n.z >= 0.0 ? n.xy : (1.0 - abs(n.yx)) * vec2(n.x >= 0.0 ? 1.0 : -1.0, n.y >= 0.0 ? 1.0 : -1.0);
    return e * 0.5 + 0.5;
}
vec3 decodeNormal(vec2 e) {
    e = e * 2.0 - 1.0;
    vec3 n = vec3(e, 1.0 - abs(e.x) - abs(e.y));
    float t = saturate(-n.z);
    n.xy += vec2(n.x >= 0.0 ? -t : t, n.y >= 0.0 ? -t : t);
    return normalize(n);
}

vec3 toView(vec2 uv, float depth) {
    vec4 p = gbufferProjectionInverse * vec4(vec3(uv, depth) * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}
vec3 toScreen(vec3 view) {
    vec4 p = gbufferProjection * vec4(view, 1.0);
    return (p.xyz / p.w) * 0.5 + 0.5;
}

// Shadow-map distortion: more resolution near the player.
const float SHADOW_DISTORT = 0.88;
vec3 distortShadow(vec3 p) {
    float f = length(p.xy) * SHADOW_DISTORT + (1.0 - SHADOW_DISTORT);
    return vec3(p.xy / f, p.z * 0.25);
}

// Hash / value noise
float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}
float noise2(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1, 0)), u.x), mix(hash12(i + vec2(0, 1)), hash12(i + vec2(1, 1)), u.x), u.y);
}
float fbm2(vec2 p, int oct) {
    float s = 0.0, a = 0.5;
    for (int i = 0; i < oct; i++) { s += a * noise2(p); p = p * 2.03 + vec2(17.1, 9.7); a *= 0.5; }
    return s;
}
float bayer8(vec2 c) {
    ivec2 p = ivec2(mod(c, 8.0));
    int m[64] = int[](0,32,8,40,2,34,10,42,48,16,56,24,50,18,58,26,12,44,4,36,14,46,6,38,60,28,52,20,62,30,54,22,3,35,11,43,1,33,9,41,51,19,59,27,49,17,57,25,15,47,7,39,13,45,5,37,63,31,55,23,61,29,53,21);
    return float(m[p.y * 8 + p.x]) / 64.0;
}

// Day factor from the sun height (1 = full day, 0 = night)
float sunHeight() { return dot(normalize(sunPosition), normalize(upPosition)); }
float dayFactor() { return smoothstep(-0.08, 0.18, sunHeight()); }

// Montella Realism - shared helpers
#include "/lib/settings.glsl"

#define PI 3.14159265359
#define TAU 6.28318530718

// material ids (stored as id / 255 in colortex2.r)
#define MAT_DEFAULT 0
#define MAT_PLANT 1
#define MAT_LEAVES 2
#define MAT_EMISSIVE 3
#define MAT_WATER 4
#define MAT_HAND 5
#define MAT_ENTITY 6
#define MAT_SNOW 7
#define MAT_LAVA 8
#define MAT_TRANSLUCENT 9
#define MAT_SAND 10
#define MAT_PARTICLE 11

float sat(float x) { return clamp(x, 0.0, 1.0); }
vec2  sat(vec2 x)  { return clamp(x, 0.0, 1.0); }
vec3  sat(vec3 x)  { return clamp(x, 0.0, 1.0); }
float sq(float x)  { return x * x; }
float luma(vec3 c) { return dot(c, vec3(0.2126, 0.7152, 0.0722)); }

vec3 toLinear(vec3 c) { return c * (c * (c * 0.305306011 + 0.682171111) + 0.012522878); }
vec3 toSRGB(vec3 c)   { return mix(c * 12.92, 1.055 * pow(max(c, vec3(0.0)), vec3(1.0 / 2.4)) - 0.055, step(vec3(0.0031308), c)); }

vec2 octEncode(vec3 n) {
    n /= abs(n.x) + abs(n.y) + abs(n.z);
    vec2 r = n.z >= 0.0 ? n.xy : (1.0 - abs(n.yx)) * vec2(n.x >= 0.0 ? 1.0 : -1.0, n.y >= 0.0 ? 1.0 : -1.0);
    return r * 0.5 + 0.5;
}
vec3 octDecode(vec2 e) {
    e = e * 2.0 - 1.0;
    vec3 n = vec3(e, 1.0 - abs(e.x) - abs(e.y));
    float t = max(-n.z, 0.0);
    n.x += n.x >= 0.0 ? -t : t;
    n.y += n.y >= 0.0 ? -t : t;
    return normalize(n);
}

vec3 projectDivide(mat4 m, vec3 p) { vec4 h = m * vec4(p, 1.0); return h.xyz / h.w; }

float ign(vec2 p) { return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715)))); }
float hash12(vec2 p) { vec3 p3 = fract(vec3(p.xyx) * 0.1031); p3 += dot(p3, p3.yzx + 33.33); return fract((p3.x + p3.y) * p3.z); }
float hash13(vec3 p) { p = fract(p * 0.1031); p += dot(p, p.zyx + 31.32); return fract((p.x + p.y) * p.z); }
vec3  hash33(vec3 p) { p = fract(p * vec3(0.1031, 0.1030, 0.0973)); p += dot(p, p.yxz + 33.33); return fract((p.xxy + p.yxx) * p.zyx); }

// iq style value noise on the 256x256 noise texture (bilinear filtered)
uniform sampler2D noisetex;
float noise2(vec2 p) { return textureLod(noisetex, (p + 0.5) / 256.0, 0.0).g; }
float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    vec2 uv = (i.xy + vec2(37.0, 17.0) * i.z) + f.xy;
    vec2 rg = textureLod(noisetex, (uv + 0.5) / 256.0, 0.0).gr;
    return mix(rg.x, rg.y, f.z);
}
float noise2s(vec2 p) { // smooth (quintic) value noise
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return textureLod(noisetex, (i + f + 0.5) / 256.0, 0.0).g;
}

float henyeyGreenstein(float c, float g) {
    float g2 = g * g;
    return (1.0 - g2) / (4.0 * PI * pow(1.0 + g2 - 2.0 * g * c, 1.5));
}
float rayleighPhase(float c) { return 3.0 / (16.0 * PI) * (1.0 + c * c); }

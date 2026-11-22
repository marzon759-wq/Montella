// Exposure, bloom, filmic tone mapping and colour grading.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH
out vec2 uv;
flat out float exposure;
void main() {
    uv = gl_MultiTexCoord0.xy;
    // exposure follows how much sky the eye sees and the time of day
    vec3 sd = sunDirection();
    float day = sat(sd.y * 4.0 + 0.25);
    float eye = float(eyeBrightnessSmooth.y) / 240.0;
    float sceneLight = mix(0.05, 1.0, day) * mix(0.25, 1.0, eye * eye) * (1.0 - rainStrength * 0.5);
#if WORLD == -1
    sceneLight = 0.25;
#elif WORLD == 1
    sceneLight = 0.22;
#endif
    exposure = 1.0 / (0.06 + sceneLight * 1.6);
    exposure = min(exposure, 6.0) * exp2(EXPOSURE) * 1.1;
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 uv;
flat in float exposure;
uniform sampler2D colortex0;
uniform sampler2D colortex4;
layout(location = 0) out vec4 outColor;

// AgX-like filmic curve (good hue preservation for bright skies)
vec3 agxDefault(vec3 c) {
    const mat3 inMat = mat3(0.842479, 0.0423282, 0.0423756, 0.0784336, 0.878468, 0.0784336, 0.0792237, 0.0791661, 0.879142);
    const mat3 outMat = mat3(1.19688, -0.0528968, -0.0529716, -0.0980209, 1.15190, -0.0980435, -0.0990297, -0.0989612, 1.15107);
    c = inMat * c;
    c = clamp(log2(max(c, 1e-10)), -12.47393, 4.026069);
    c = (c + 12.47393) / 16.49999;
    vec3 x2 = c * c, x4 = x2 * x2;
    c = 15.5 * x4 * x2 - 40.14 * x4 * c + 31.96 * x4 - 6.868 * x2 * c + 0.4298 * x2 + 0.1191 * c - 0.00232;
    c = outMat * c;
    return pow(max(c, vec3(0.0)), vec3(2.2));
}

// fitted ACES-like filmic curve with a gentle highlight desaturation
vec3 filmic(vec3 x) {
    x *= 0.6;
    vec3 y = (x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14);
    float l = luma(y);
    return mix(y, vec3(l), sat((l - 0.9) * 2.0) * 0.35);
}

void main() {
    vec2 px = 1.0 / vec2(viewWidth, viewHeight);
    vec3 col = texture(colortex0, uv).rgb;
    if (SHARPEN > 0.0) {
    vec3 blur = (texture(colortex0, uv + vec2(px.x, 0.0)).rgb + texture(colortex0, uv - vec2(px.x, 0.0)).rgb +
                 texture(colortex0, uv + vec2(0.0, px.y)).rgb + texture(colortex0, uv - vec2(0.0, px.y)).rgb) * 0.25;
    col = max(col + (col - blur) * SHARPEN, col * 0.5);
    }
#ifdef BLOOM
    vec3 bloom = texture(colortex4, uv).rgb;
    col += bloom * 0.035 * BLOOM_STRENGTH * (1.0 + rainStrength * 1.5);
#endif
    col *= exposure;
    // night: rods take over, colours fade and shift blue
    float l = luma(col);
    float scot = sat(1.0 - l * 12.0);
    col = mix(col, vec3(l) * vec3(0.72, 0.86, 1.15), scot * 0.55);

    col = filmic(col);
    // grading towards a vivid landscape photo look
    float g = luma(col);
    col = mix(vec3(g), col, SATURATION);
    col = sat(col);
    col = mix(col, smoothstep(0.0, 1.0, col), CONTRAST - 1.0 + 0.25);
    col = toSRGB(col);
    // vignette
    vec2 v = uv - 0.5;
    col *= 1.0 - dot(v, v) * VIGNETTE * 1.2;
    col += (ign(gl_FragCoord.xy) - 0.5) / 255.0;
    outColor = vec4(col, 1.0);
}
#endif

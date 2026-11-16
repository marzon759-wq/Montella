// Bloom, auto exposure, filmic tone mapping and a light grade.
#ifdef VSH
out vec2 uv;
void main() { uv = gl_MultiTexCoord0.xy; gl_Position = ftransform(); }
#endif
#ifdef FSH
/*
const bool colortex4MipmapEnabled = true;
*/
uniform sampler2D colortex0, colortex4;
uniform ivec2 eyeBrightnessSmooth;
uniform float rainStrength;
in vec2 uv;
layout(location = 0) out vec4 outColor;

vec3 aces(vec3 x) {
    const float a = 2.51, b = 0.03, c = 2.43, d = 0.59, e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec3 col = texture(colortex0, uv).rgb;
    vec3 bloom = vec3(0.0);
    bloom += textureLod(colortex4, uv, 2.0).rgb * 0.30;
    bloom += textureLod(colortex4, uv, 3.0).rgb * 0.25;
    bloom += textureLod(colortex4, uv, 4.0).rgb * 0.20;
    bloom += textureLod(colortex4, uv, 5.0).rgb * 0.15;
    bloom += textureLod(colortex4, uv, 6.0).rgb * 0.10;
    col += bloom * 0.35;
    // brighter exposure in caves and under cover, a touch darker in open daylight
    float skyLight = float(eyeBrightnessSmooth.y) / 240.0;
    float exposure = mix(2.4, 1.05, skyLight);
    col = aces(col * exposure);
    col = pow(col, vec3(1.0 / 2.2));
    float l = dot(col, vec3(0.2126, 0.7152, 0.0722));
    col = mix(vec3(l), col, 1.12);
    vec2 v = uv - 0.5;
    col *= 1.0 - dot(v, v) * 0.35;
    outColor = vec4(col, 1.0);
}
#endif

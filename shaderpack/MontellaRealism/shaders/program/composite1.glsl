// Bloom: gather a wide soft glow from the mip chain of the HDR image.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"

#ifdef VSH
out vec2 uv;
void main() { uv = gl_MultiTexCoord0.xy; gl_Position = ftransform(); }
#endif

#ifdef FSH
in vec2 uv;
uniform sampler2D colortex0;
const bool colortex0MipmapEnabled = true;
/* RENDERTARGETS: 4 */
layout(location = 0) out vec4 outBloom;

vec3 tap(float lod) {
    vec2 px = exp2(lod) / vec2(viewWidth, viewHeight);
    vec3 c = textureLod(colortex0, uv, lod).rgb * 4.0;
    c += textureLod(colortex0, uv + vec2( px.x,  px.y), lod).rgb;
    c += textureLod(colortex0, uv + vec2(-px.x,  px.y), lod).rgb;
    c += textureLod(colortex0, uv + vec2( px.x, -px.y), lod).rgb;
    c += textureLod(colortex0, uv + vec2(-px.x, -px.y), lod).rgb;
    c += textureLod(colortex0, uv + vec2( px.x * 2.0, 0.0), lod).rgb * 0.5;
    c += textureLod(colortex0, uv + vec2(-px.x * 2.0, 0.0), lod).rgb * 0.5;
    c += textureLod(colortex0, uv + vec2(0.0,  px.y * 2.0), lod).rgb * 0.5;
    c += textureLod(colortex0, uv + vec2(0.0, -px.y * 2.0), lod).rgb * 0.5;
    return c / 10.0;
}

void main() {
    vec3 b = vec3(0.0);
    float w = 0.0;
    for (int i = 2; i <= 7; i++) {
        float wi = 1.0 / (1.0 + float(i - 2) * 0.6);
        b += tap(float(i)) * wi;
        w += wi;
    }
    outBloom = vec4(b / w, 1.0);
}
#endif

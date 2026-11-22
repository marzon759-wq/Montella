// Rain and snow: thin, slightly translucent streaks lit by the sky.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH
out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
flat out vec3 directCol;
flat out vec3 ambientCol;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = sat(((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy - 1.0 / 32.0) * (32.0 / 30.0));
    glcolor = gl_Color;
    lightColors(directCol, ambientCol);
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
flat in vec3 directCol;
flat in vec3 ambientCol;
uniform sampler2D gtexture;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.01) discard;
    bool snow = c.b > 0.0 && abs(c.r - c.b) < 0.08 && c.r > 0.75 && biome_precipitation == 2;
    vec3 light = ambientCol * 1.1 + directCol * 0.25 + vec3(1.0, 0.6, 0.3) * pow(lmcoord.x, 4.0) * 0.8;
    if (biome_precipitation == 2) {
        outColor = vec4(vec3(0.95, 0.97, 1.0) * light * 1.3, c.a * 0.9);
    } else {
        outColor = vec4(vec3(0.75, 0.82, 0.9) * light * 1.6, c.a * 0.28);
    }
}
#endif

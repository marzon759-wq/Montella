// Vanilla sky geometry. The real sky is computed in deferred; only the moon texture is kept.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"

#ifdef VSH
out vec2 texcoord;
out vec4 glcolor;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 texcoord;
in vec4 glcolor;
uniform sampler2D gtexture;
uniform int renderStage;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
#ifdef TEXTURED
    if (renderStage == MC_RENDER_STAGE_MOON) {
        vec4 c = texture(gtexture, texcoord) * glcolor;
        outColor = vec4(c.rgb * c.a, 1.0);
        return;
    }
#endif
    discard;
}
#endif

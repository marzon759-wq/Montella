// Overlays that only tint the albedo: block breaking cracks, enchantment glint.
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
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 c = texture(gtexture, texcoord) * glcolor;
#ifdef DAMAGE
    if (c.a < 0.01) discard;
#endif
    outColor = c;
}
#endif

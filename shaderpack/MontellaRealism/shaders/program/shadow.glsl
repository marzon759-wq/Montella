// Shadow map. Water writes a marker (alpha 0) into shadowcolor0 for caustics; glass writes its tint.
#define SHADOW_PASS
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"
#include "/lib/shadows.glsl"

#ifdef VSH
in vec4 mc_Entity;
in vec2 mc_midTexCoord;
out vec2 texcoord;
out vec4 glcolor;
flat out int id;
#include "/lib/waving.glsl"
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    glcolor = gl_Color;
    id = int(mc_Entity.x + 0.5);
    vec4 sv = gl_ModelViewMatrix * gl_Vertex;
    vec3 playerPos = (shadowModelViewInverse * sv).xyz;
    vec3 worldPos = playerPos + cameraPosition;
    bool top = gl_MultiTexCoord0.t < mc_midTexCoord.t;
    vec2 lm = sat(((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy - 1.0 / 32.0) * (32.0 / 30.0));
#ifdef WAVING_PLANTS
    if (id == 10001) playerPos += windOffset(worldPos, top ? 1.0 : 0.0) * sat(lm.y * 2.0 - 0.3);
    if (id == 10002) playerPos += windOffset(worldPos, top ? 0.55 : 0.0) * sat(lm.y * 2.0 - 0.3);
    if (id == 10012) playerPos += windOffset(worldPos, top ? 1.35 : 0.55) * sat(lm.y * 2.0 - 0.3);
#endif
#ifdef WAVING_LEAVES
    if (id == 10003) playerPos += leafOffset(worldPos) * sat(lm.y * 2.0 - 0.3);
#endif
    vec4 p = shadowProjection * (shadowModelView * vec4(playerPos, 1.0));
    p.xyz = distortShadow(p.xyz);
    gl_Position = p;
}
#endif

#ifdef FSH
in vec2 texcoord;
in vec4 glcolor;
flat in int id;
uniform sampler2D gtexture;
/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (id == 10004) { outColor = vec4(0.0); return; }
    if (c.a < 0.1) discard;
    outColor = c;
}
#endif

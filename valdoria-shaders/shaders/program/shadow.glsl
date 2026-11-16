// Shadow map: everything but water casts shadows; plants sway in their shadows too.
#include "/lib/common.glsl"
#ifdef VSH
in vec4 mc_Entity;
in vec4 mc_midTexCoord;
out vec2 texcoord;
#include "/lib/waving.glsl"
uniform mat4 shadowModelViewInverse;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    float id = mc_Entity.x;
    if (id == 10003.0) { gl_Position = vec4(-10.0); return; }
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    if (id >= 10000.0) {
        vec3 world = (shadowModelViewInverse * vp).xyz + cameraPosition;
        float top = float(texcoord.y < mc_midTexCoord.y);
        vp.xyz += mat3(shadowModelView) * wave(world, id, top);
    }
    vec4 p = gl_ProjectionMatrix * vp;
    p.xyz = distortShadow(p.xyz);
    gl_Position = p;
}
#endif
#ifdef FSH
uniform sampler2D gtexture;
in vec2 texcoord;
layout(location = 0) out vec4 outColor;
void main() {
    vec4 c = texture(gtexture, texcoord);
    if (c.a < 0.1) discard;
    outColor = c;
}
#endif

// Opaque geometry (terrain, block entities, entities, hand): writes the G-buffer that deferred lighting reads.
#include "/lib/common.glsl"

#ifdef VSH
in vec4 mc_Entity;
in vec4 mc_midTexCoord;
out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 normalV;
flat out float mat;
#include "/lib/waving.glsl"

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    normalV = normalize(gl_NormalMatrix * gl_Normal);
    vec4 viewPos = gl_ModelViewMatrix * gl_Vertex;
    float id = mc_Entity.x;
    mat = MAT_NONE;
#ifdef TERRAIN
    if (id >= 10000.0) {
        vec3 world = (gbufferModelViewInverse * viewPos).xyz + cameraPosition;
        float top = float(texcoord.y < mc_midTexCoord.y);
        viewPos.xyz += mat3(gbufferModelView) * wave(world, id, top);
    }
    if (id == 10001.0 || id == 10007.0) mat = MAT_FOLIAGE;
    else if (id == 10002.0) mat = MAT_LEAVES;
    else if (id == 10004.0) mat = MAT_EMISSIVE;
    else if (id == 10005.0) mat = MAT_VINE;
    else if (id == 10006.0) mat = MAT_GLOWLEAVES;
#endif
#ifdef ENTITIES
    mat = MAT_ENTITY;
#endif
#ifdef HAND
    mat = MAT_HAND;
#endif
    gl_Position = gl_ProjectionMatrix * viewPos;
}
#endif

#ifdef FSH
uniform sampler2D gtexture;
uniform vec4 entityColor;
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 normalV;
flat in float mat;

/* RENDERTARGETS: 0,1,2 */
layout(location = 0) out vec4 outAlbedo;
layout(location = 1) out vec4 outNormal;
layout(location = 2) out vec4 outLight;

void main() {
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.1) discard;
#ifdef ENTITIES
    c.rgb = mix(c.rgb, entityColor.rgb, entityColor.a);
#endif
    vec2 lm = saturate((lmcoord - 1.0 / 32.0) * 16.0 / 15.0);
    outAlbedo = vec4(c.rgb, 1.0);
    outNormal = vec4(encodeNormal(normalize(normalV)), mat / 255.0, 1.0);
    outLight = vec4(lm, 0.0, 1.0);
}
#endif

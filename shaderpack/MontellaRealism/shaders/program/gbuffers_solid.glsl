// Opaque geometry (terrain, entities, block entities, hand, particles...).
// Writes albedo, normal + lightmap and material data; lighting happens in deferred.
// Variants: TERRAIN, ENTITIES, HAND, BLOCK, PARTICLES, BASIC, EMISSIVE_ALL, BEAM, GLINT, DAMAGE

#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"

#ifdef VSH
in vec4 mc_Entity;
in vec2 mc_midTexCoord;
in vec4 at_tangent;

out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 normalW;
out vec4 tangentW;
flat out int mat;

#include "/lib/waving.glsl"

void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    vec2 lm = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    lmcoord = sat((lm - 1.0 / 32.0) * (32.0 / 30.0));
    glcolor = gl_Color;
    vec3 viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    vec3 playerPos = mat3(gbufferModelViewInverse) * viewPos + gbufferModelViewInverse[3].xyz;
    normalW = normalize(mat3(gbufferModelViewInverse) * (gl_NormalMatrix * gl_Normal));
    tangentW = vec4(normalize(mat3(gbufferModelViewInverse) * (gl_NormalMatrix * at_tangent.xyz)), at_tangent.w);
    mat = MAT_DEFAULT;

#ifdef TERRAIN
    int id = int(mc_Entity.x + 0.5);
    vec3 worldPos = playerPos + cameraPosition;
    bool top = gl_MultiTexCoord0.t < mc_midTexCoord.t;
    if (id == 10001 || id == 10002 || id == 10012) {
        mat = MAT_PLANT;
#ifdef WAVING_PLANTS
        float amount = 0.0;
        if (id == 10001) amount = top ? 1.0 : 0.0;
        else if (id == 10002) amount = top ? 0.55 : 0.0;
        else amount = top ? 1.35 : 0.55;
        playerPos += windOffset(worldPos, amount * sat(lmcoord.y * 2.0 - 0.3));
#endif
    } else if (id == 10003) {
        mat = MAT_LEAVES;
#ifdef WAVING_LEAVES
        playerPos += leafOffset(worldPos) * sat(lmcoord.y * 2.0 - 0.3);
#endif
    } else if (id == 10006) {
        mat = MAT_LEAVES;
#ifdef WAVING_LEAVES
        playerPos += leafOffset(worldPos) * 0.6 * sat(lmcoord.y * 2.0 - 0.3);
#endif
    } else if (id == 10005) mat = MAT_EMISSIVE;
    else if (id == 10008) mat = MAT_SNOW;
    else if (id == 10009) mat = MAT_LAVA;
    else if (id == 10010) mat = MAT_SAND;
#endif
#ifdef HAND
    mat = MAT_HAND;
#endif
#ifdef ENTITIES
    mat = MAT_ENTITY;
#endif
#ifdef PARTICLES
    mat = MAT_PARTICLE;
#endif
#if defined EMISSIVE_ALL || defined BEAM
    mat = MAT_EMISSIVE;
#endif
    gl_Position = gl_ProjectionMatrix * (gbufferModelView * vec4(playerPos, 1.0));
}
#endif

#ifdef FSH
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 normalW;
in vec4 tangentW;
flat in int mat;

uniform sampler2D gtexture;
uniform sampler2D normals;
uniform vec4 entityColor;
uniform int renderStage;

/* RENDERTARGETS: 0,1,2 */
layout(location = 0) out vec4 outAlbedo;
layout(location = 1) out vec4 outNormal;
layout(location = 2) out vec4 outMaterial;

void main() {
#ifdef BASIC
    vec4 albedo = glcolor;
#else
    vec4 albedo = texture(gtexture, texcoord) * vec4(glcolor.rgb, 1.0);
#endif
#if defined TERRAIN
    // separateAo: vanilla ambient occlusion lives in glcolor.a
    float ao = glcolor.a;
#else
    float ao = 1.0;
    albedo.a *= glcolor.a;
#endif
#ifdef DAMAGE
    albedo = texture(gtexture, texcoord) * glcolor;
#endif
    if (albedo.a < 0.1) discard;
#ifdef ENTITIES
    albedo.rgb = mix(albedo.rgb, entityColor.rgb, entityColor.a);
#endif
    vec3 n = normalize(normalW);
    if (!gl_FrontFacing && mat != MAT_PLANT && mat != MAT_LEAVES) n = -n;
#ifdef LABPBR_NORMALS
#if defined TERRAIN || defined BLOCK || defined ENTITIES || defined HAND
    vec3 nt = texture(normals, texcoord).xyz;
    if (nt.x + nt.y > 0.01) {
        vec2 xy = nt.xy * 2.0 - 1.0;
        vec3 tn = vec3(xy, sqrt(sat(1.0 - dot(xy, xy))));
        vec3 t = normalize(tangentW.xyz);
        vec3 b = cross(t, n) * (tangentW.w < 0.0 ? -1.0 : 1.0);
        n = normalize(mat3(t, b, n) * tn);
    }
#endif
#endif
    float emission = 0.0;
    if (mat == MAT_EMISSIVE) {
        float l = luma(albedo.rgb);
        emission = smoothstep(0.45, 0.9, l + max(albedo.r - albedo.b, 0.0) * 0.3);
#if defined EMISSIVE_ALL || defined BEAM
        emission = 1.0;
#endif
    }
    if (mat == MAT_LAVA) emission = 1.0;
#ifdef GLINT
    emission = 0.6;
#endif
    // grass & flowers: light them like a soft volume, normal points up
    if (mat == MAT_PLANT) n = normalize(mix(n, vec3(0.0, 1.0, 0.0), 0.75));

    outAlbedo = albedo;
    outNormal = vec4(octEncode(n), lmcoord);
    outMaterial = vec4(float(mat) / 255.0, ao, emission, 1.0);
}
#endif

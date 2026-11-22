// Particles (vanilla and modded spray / mist / foam), lit forward and kept in their own layer (colortex5)
// so that the water and fog passes cannot overwrite them. Composite lays the layer on top.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH
out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 playerPos;
flat out vec3 directCol;
flat out vec3 ambientCol;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = sat(((gl_TextureMatrix[1] * gl_MultiTexCoord1).xy - 1.0 / 32.0) * (32.0 / 30.0));
    glcolor = gl_Color;
    vec3 viewPos = (gl_ModelViewMatrix * gl_Vertex).xyz;
    playerPos = mat3(gbufferModelViewInverse) * viewPos + gbufferModelViewInverse[3].xyz;
    lightColors(directCol, ambientCol);
    gl_Position = gl_ProjectionMatrix * vec4(viewPos, 1.0);
}
#endif

#ifdef FSH
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 playerPos;
flat in vec3 directCol;
flat in vec3 ambientCol;
uniform sampler2D gtexture;
#include "/lib/shadows.glsl"
/* RENDERTARGETS: 5 */
layout(location = 0) out vec4 outColor;
void main() {
    vec4 c = texture(gtexture, texcoord) * glcolor;
    if (c.a < 0.004) discard;
    vec3 a = toLinear(c.rgb);
    vec3 light = ambientCol * (lmcoord.y * lmcoord.y * 0.9 + 0.1) * 1.1;
#if WORLD == 0
    float wd;
    vec3 sh = shadowLight(playerPos, vec3(0.0, 1.0, 0.0), ign(gl_FragCoord.xy), 1.0, wd).rgb;
    // particles are small and thin: lit from every side, a lot of light passes through them
    light += directCol * sh * 0.75;
#elif WORLD == -1
    light = toLinear(fogColor) * 0.8 + vec3(0.16, 0.085, 0.06);
#else
    light = vec3(0.10, 0.085, 0.13);
#endif
    light += vec3(1.0, 0.56, 0.24) * pow(lmcoord.x, 4.0) * 1.8 * BLOCK_LIGHT + MIN_LIGHT;
    outColor = vec4(a * light, c.a);
}
#endif

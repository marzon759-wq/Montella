// Translucent things drawn after deferred lighting (particles, rain/snow, glass, glowing eyes): lit directly.
#include "/lib/common.glsl"
#include "/lib/sky.glsl"

#ifdef VSH
out vec2 texcoord;
out vec2 lmcoord;
out vec4 glcolor;
out vec3 viewPos;
void main() {
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
    glcolor = gl_Color;
    vec4 vp = gl_ModelViewMatrix * gl_Vertex;
    viewPos = vp.xyz;
    gl_Position = gl_ProjectionMatrix * vp;
}
#endif

#ifdef FSH
uniform sampler2D gtexture;
in vec2 texcoord;
in vec2 lmcoord;
in vec4 glcolor;
in vec3 viewPos;

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

void main() {
#ifdef BASIC
    vec4 c = glcolor;
#else
    vec4 c = texture(gtexture, texcoord) * glcolor;
#endif
    if (c.a < 0.004) discard;
    vec2 lm = saturate((lmcoord - 1.0 / 32.0) * 16.0 / 15.0);
    vec3 albedo = pow(c.rgb, vec3(2.2));
    vec3 ambient = pow(mix(fogColor, skyColor, 0.6), vec3(2.2)) * mix(0.1, 1.0, dayFactor()) * 0.7 * lm.y * lm.y;
    vec3 light = ambient + lightColor() * 0.35 * lm.y + vec3(1.0, 0.62, 0.32) * pow(lm.x, 3.5) * 2.0 + 0.02;
#ifdef WEATHER
    // thin, cool, translucent rain streaks; soft bright snow
    float snow = step(0.8, luma(c.rgb));
    c.a *= mix(0.35, 0.8, snow);
    albedo = mix(albedo * vec3(0.75, 0.85, 1.0), albedo, snow);
#endif
#ifdef EMISSIVE
    light = vec3(4.0);
#endif
    vec3 col = albedo * light;
    // full-bright particles (glowing motes, sparks) glow
    if (lm.x > 0.99 && lm.y > 0.99) col = albedo * 3.0;
    outColor = vec4(col, c.a);
}
#endif

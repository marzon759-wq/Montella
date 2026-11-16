// Bright pass for bloom.
#ifdef VSH
out vec2 uv;
void main() { uv = gl_MultiTexCoord0.xy; gl_Position = ftransform(); }
#endif
#ifdef FSH
uniform sampler2D colortex0;
in vec2 uv;
/* RENDERTARGETS: 4 */
layout(location = 0) out vec4 outBloom;
void main() {
    vec3 c = texture(colortex0, uv).rgb;
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    outBloom = vec4(c * smoothstep(0.9, 3.0, l), 1.0);
}
#endif

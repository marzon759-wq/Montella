#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    float t = Time * 20.0;
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.30, 0.59, 0.11));
    // drained, cold, with crushed blacks
    c = mix(vec3(l), c, 0.70);
    c *= vec3(0.94, 0.98, 1.05);
    // eyes adjust: lift the toe so pitch dark rooms keep a few blocks of shape
    c = pow(c, vec3(0.80)) * 1.04;
    float n = hash(texCoord * InSize + vec2(t * 13.0, t * 29.0));
    c += (n - 0.5) * 0.045;
    vec2 d = texCoord - 0.5;
    c *= 0.74 + 0.26 * smoothstep(0.86, 0.30, length(d));
    fragColor = vec4(c, 1.0);
}

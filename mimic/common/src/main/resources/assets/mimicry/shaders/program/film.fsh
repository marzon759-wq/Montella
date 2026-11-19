#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    float t = Time * 20.0;
    vec2 d = texCoord - 0.5;
    // a little chromatic fringe toward the edges, like an old lens
    float ca = 0.0016 * length(d) * 2.0;
    vec3 c;
    c.r = texture(DiffuseSampler, texCoord + d * ca).r;
    c.g = texture(DiffuseSampler, texCoord).g;
    c.b = texture(DiffuseSampler, texCoord - d * ca).b;
    float l = dot(c, vec3(0.30, 0.59, 0.11));
    // drained, sickly: the colour of a farm at three in the morning
    c = mix(vec3(l), c, 0.62);
    c *= vec3(1.04, 0.98, 0.90);
    c = pow(c, vec3(0.86)) * 1.02;
    c = (c - 0.03) * 1.06;
    float n = hash(texCoord * InSize + vec2(t * 13.0, t * 29.0));
    c += (n - 0.5) * 0.05;
    c *= 0.70 + 0.30 * smoothstep(0.88, 0.28, length(d));
    fragColor = vec4(max(c, 0.0), 1.0);
}

#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    float t = Time * 20.0;
    vec2 uv = texCoord;
    // tape wobble: every few scanlines slip sideways for a frame
    float band = floor(uv.y * InSize.y / 4.0);
    float slip = step(0.985, hash(vec2(band, floor(t * 2.0)))) * (hash(vec2(band, t)) - 0.5) * 0.02;
    vec3 c = texture(DiffuseSampler, uv + vec2(slip, 0.0)).rgb;
    // cheap bloom so lamps bleed like an image intensifier tube
    vec2 px = 2.5 / InSize;
    vec3 b = texture(DiffuseSampler, uv + vec2(px.x, 0.0)).rgb + texture(DiffuseSampler, uv - vec2(px.x, 0.0)).rgb
           + texture(DiffuseSampler, uv + vec2(0.0, px.y)).rgb + texture(DiffuseSampler, uv - vec2(0.0, px.y)).rgb;
    float l = dot(c, vec3(0.30, 0.59, 0.11));
    float lb = dot(b * 0.25, vec3(0.30, 0.59, 0.11));
    l = pow(l, 0.72) * 1.18 + max(0.0, lb - 0.55) * 0.9;
    float n = hash(uv * InSize + vec2(t * 17.0, t * 31.0));
    l += (n - 0.5) * 0.17;
    float scan = 0.86 + 0.14 * sin(uv.y * InSize.y * 1.57);
    vec2 d = uv - 0.5;
    float vig = smoothstep(0.80, 0.22, length(d * vec2(1.0, 1.12)));
    vec3 g = vec3(0.12, 1.0, 0.26) * l * scan;
    g += vec3(0.5, 0.6, 0.5) * max(0.0, l - 0.92) * 2.0;
    g = g * (0.18 + 0.82 * vig) + vec3(0.0, 0.022, 0.004);
    fragColor = vec4(g, 1.0);
}

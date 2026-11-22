// Physically based sky: single scattering Rayleigh + Mie with ozone absorption.
// Units: metres. Output is linear HDR radiance in the pack's scene units.

const float R_PLANET = 6360e3;
const float R_ATMOS  = 6460e3;
const float H_RAY    = 8000.0;
const float H_MIE    = 1200.0;
const vec3  BETA_RAY = vec3(5.802e-6, 13.558e-6, 33.1e-6);
const vec3  BETA_MIE = vec3(2.0e-6);
const vec3  EXT_MIE  = vec3(2.22e-6);
const vec3  BETA_OZO = vec3(0.650e-6, 1.881e-6, 0.085e-6);
const float SUN_RADIANCE  = 22.0;   // sky radiance scale for the sun
const float MOON_RADIANCE = 0.03;  // moon is ~1/500 of the sun (artistic)

// all directions below are player (world-aligned) space
vec3 sunDirection()  { return normalize(mat3(gbufferModelViewInverse) * sunPosition); }
vec3 moonDirection() { return normalize(mat3(gbufferModelViewInverse) * moonPosition); }
vec3 lightDirection(){ return normalize(mat3(gbufferModelViewInverse) * shadowLightPosition); }

// relative air mass with a soft earth-shadow below the horizon (Kasten & Young, extended)
float airMass(float mu) {
    float a = acos(clamp(mu, -1.0, 1.0)) * 57.2957795;
    if (mu > -0.02) return 1.0 / (max(mu, 0.0) + 0.50572 * pow(max(96.07995 - a, 0.01), -1.6364));
    return 40.0 + (-0.02 - mu) * 4000.0;
}

vec3 opticalExtinction(float odR, float odM) {
    return BETA_RAY * odR + EXT_MIE * odM + BETA_OZO * odR * 1.6;
}

// transmittance of the whole atmosphere from the ground towards a direction
vec3 atmosTransmittance(float mu) {
    float m = airMass(mu);
    return exp(-opticalExtinction(H_RAY, H_MIE) * m);
}

float viewerHeight() { return 120.0 + max(eyeAltitude - 63.0, 0.0) * 1.0; }

// single-scattering sky radiance in direction dir for a light (sun or moon)
vec3 atmosphereRaw(vec3 dir, vec3 L, float intensity, int steps) {
    float h0 = viewerHeight();
    vec3 ro = vec3(0.0, R_PLANET + h0, 0.0);
    // flatten the lower half a little so the horizon is not a hard line
    vec3 d = normalize(vec3(dir.x, max(dir.y, 0.0) * 0.95 + 0.05, dir.z));
    float b = dot(ro, d);
    float c = dot(ro, ro) - R_ATMOS * R_ATMOS;
    float tMax = -b + sqrt(max(b * b - c, 0.0));
    float ds = tMax / float(steps);
    float odR = 0.0, odM = 0.0;
    vec3 sumR = vec3(0.0), sumM = vec3(0.0);
    for (int i = 0; i < steps; i++) {
        vec3 p = ro + d * ds * (float(i) + 0.5);
        float r = length(p);
        float h = r - R_PLANET;
        float dR = exp(-h / H_RAY) * ds;
        float dM = exp(-h / H_MIE) * ds;
        odR += dR; odM += dM;
        float mu = dot(p / r, L);
        float m = airMass(mu);
        // view path extinction is softened: stands in for multiple scattering, keeps the horizon hazy-white
        vec3 tau = opticalExtinction(odR * 0.8 + H_RAY * exp(-h / H_RAY) * m, odM * 0.8 + H_MIE * exp(-h / H_MIE) * m);
        vec3 att = exp(-tau);
        sumR += dR * att;
        sumM += dM * att;
    }
    float cosT = dot(d, L);
    float g = mix(0.76, 0.55, rainStrength);
    vec3 col = sumR * BETA_RAY * rayleighPhase(cosT) + sumM * BETA_MIE * henyeyGreenstein(cosT, g);
    return col * intensity;
}

// full sky (sun + moon + night base), no clouds, no sun disk
vec3 skyRadiance(vec3 dir, int steps) {
    vec3 sd = sunDirection();
    vec3 sky = atmosphereRaw(dir, sd, SUN_RADIANCE, steps);
    vec3 md = -sd;
    vec3 night = atmosphereRaw(dir, md, SUN_RADIANCE * MOON_RADIANCE, steps / 2 + 2);
    // moonlit sky looks blue to the eye (Purkinje shift), make it so
    night = mix(night, vec3(luma(night)) * vec3(0.55, 0.75, 1.25), 0.65);
    sky += night * NIGHT_BRIGHTNESS;
    // faint airglow / light pollution so that night is never pure black
    sky += vec3(0.0011, 0.0016, 0.0030) * (1.0 - 0.6 * sat(dir.y)) * NIGHT_BRIGHTNESS;
    // overcast: grey it down
    float r = rainStrength;
    vec3 grey = vec3(luma(sky)) * vec3(0.92, 0.97, 1.05);
    sky = mix(sky, grey * 0.55, r * 0.85);
    // below the horizon: the haze continues, slowly darkening towards the ground
    if (dir.y < 0.0) sky *= mix(1.0, 0.55, sat(-dir.y * 3.0));
    return sky;
}

// sun / moon illuminance reaching the ground, in scene units (white diffuse under noon sun ~ 1)
vec3 sunIlluminance() {
    vec3 sd = sunDirection();
    return atmosTransmittance(sd.y) * vec3(1.0, 0.95, 0.87) * 1.75 * SUN_BRIGHTNESS * sat(sd.y * 12.0 + 0.3);
}
vec3 moonIlluminance() {
    vec3 md = moonDirection();
    vec3 c = atmosTransmittance(md.y) * 0.02 * NIGHT_BRIGHTNESS * sat(md.y * 12.0 + 0.3);
    return mix(c, vec3(luma(c)) * vec3(0.62, 0.80, 1.15), 0.7);
}

// direct light (whichever of sun/moon casts the shadows) and sky ambient, cheap enough for vertex shaders
void lightColors(out vec3 direct, out vec3 ambient) {
    vec3 sd = sunDirection();
    float day = sat(sd.y * 8.0 + 0.5);
    if (dot(normalize(shadowLightPosition), normalize(sunPosition)) > 0.5) direct = sunIlluminance();
    else direct = moonIlluminance();
    // ambient: sample the sky a few times (cosine weighted-ish)
    vec3 a = skyRadiance(vec3(0.0, 1.0, 0.0), 8) * 0.40;
    a += skyRadiance(normalize(vec3(sd.x, 0.25, sd.z)), 8) * 0.20;
    a += skyRadiance(normalize(vec3(-sd.x, 0.25, -sd.z)), 8) * 0.20;
    a += skyRadiance(normalize(vec3(sd.z, 0.35, -sd.x)), 8) * 0.20;
    // real sky irradiance is less saturated than the zenith colour (haze, ground bounce)
    ambient = mix(a, vec3(luma(a)) * vec3(0.92, 0.97, 1.08), 0.45) * 3.0 * SKY_LIGHT;
    ambient += direct * vec3(0.9, 0.85, 0.75) * 0.05;
    // rain: clouds block the sun and brighten the ambient a touch
    direct *= 1.0 - rainStrength * 0.92;
    ambient = mix(ambient, vec3(luma(ambient + direct * 0.25)) * vec3(0.95, 0.98, 1.04), rainStrength * 0.8);
}

vec3 sunDisk(vec3 dir, vec3 sd) {
    float c = dot(dir, sd);
    float r = 0.99996; // ~0.53 degrees
    float edge = smoothstep(r - 0.00002, r + 0.00001, c);
    if (edge <= 0.0) return vec3(0.0);
    float mu = sqrt(sat(1.0 - (1.0 - c) / (1.0 - r + 1e-6)));
    vec3 limb = pow(vec3(max(mu, 0.0)), vec3(0.40, 0.55, 0.75)); // limb darkening
    return atmosTransmittance(sd.y) * limb * edge * 1500.0 * (1.0 - rainStrength);
}

vec3 starField(vec3 dir) {
    vec3 sd = sunDirection();
    float vis = sat((-sd.y - 0.02) * 6.0) * (1.0 - rainStrength) * sat(dir.y * 3.0);
    if (vis <= 0.0) return vec3(0.0);
    vec3 col = vec3(0.0);
    // rotate stars with the sky
    float a = sunAngle * TAU;
    vec3 d = vec3(dir.x * cos(a) - dir.y * sin(a), dir.x * sin(a) + dir.y * cos(a), dir.z);
    for (int layer = 0; layer < 2; layer++) {
        float sc = layer == 0 ? 180.0 : 360.0;
        vec3 p = d * sc;
        vec3 cell = floor(p);
        vec3 h = hash33(cell + float(layer) * 17.0);
        float star = step(1.0 - 0.012 * STAR_AMOUNT, h.x);
        vec3 c = cell + 0.25 + h * 0.5;
        float dist = length(p - c);
        float s = star * smoothstep(0.32, 0.0, dist);
        float tw = 0.75 + 0.25 * sin(frameTimeCounter * (2.0 + h.y * 4.0) + h.z * 40.0);
        vec3 tint = mix(vec3(1.0, 0.78, 0.6), vec3(0.75, 0.85, 1.0), h.y);
        col += s * tint * tw * (0.02 + 0.10 * h.z * h.z) / float(layer + 1);
    }
    // faint galactic band
    vec3 gd = normalize(vec3(0.3, 0.2, 1.0));
    float band = exp(-sq(dot(d, gd)) * 14.0);
    float n = noise3(d * 9.0) * 0.6 + noise3(d * 23.0) * 0.4;
    col += vec3(0.6, 0.65, 0.8) * band * n * n * 0.012;
    return col * vis * atmosTransmittance(max(dir.y, 0.02));
}

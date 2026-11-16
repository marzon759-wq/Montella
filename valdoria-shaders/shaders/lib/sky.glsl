// Sky: a physically-inspired gradient that takes its base colours from the game's own sky and fog colours, so every
// biome and dimension (including modded ones) gets its own matching atmosphere, plus sun, moon, stars and clouds.

vec3 sunColor() {
    float h = sunHeight();
    vec3 noon = vec3(1.0, 0.96, 0.88) * 3.2;
    vec3 low = vec3(1.0, 0.52, 0.22) * 2.4;
    vec3 c = mix(low, noon, smoothstep(0.0, 0.45, h));
    return c * smoothstep(-0.12, 0.05, h) * (1.0 - rainStrength * 0.75);
}
vec3 moonColor() { return vec3(0.35, 0.45, 0.7) * 0.22 * smoothstep(0.0, -0.1, sunHeight()) * (1.0 - rainStrength * 0.6); }
vec3 lightColor() { return sunHeight() > -0.05 ? sunColor() : moonColor(); }

vec3 skyGradient(vec3 dir) {
#ifdef DIM_NO_SKY
    return pow(fogColor, vec3(2.2)) * 0.6;
#endif
    vec3 up = normalize(upPosition);
    float y = dot(dir, up);
    float day = dayFactor();
    // base colours from the game (biome sky / fog), lifted into HDR
    vec3 zenith = pow(skyColor, vec3(2.2)) * 1.6;
    vec3 horizon = pow(mix(fogColor, skyColor, 0.35), vec3(2.2)) * 1.25;
    zenith = mix(vec3(0.004, 0.006, 0.014), zenith, day);
    horizon = mix(vec3(0.012, 0.016, 0.03), horizon, day);
    float t = pow(saturate(y), 0.45);
    vec3 col = mix(horizon, zenith, t);
    // sunset / sunrise band around the sun
    vec3 sunDir = normalize(sunPosition);
    float sd = saturate(dot(dir, sunDir));
    float sunLow = smoothstep(0.45, 0.0, abs(sunHeight()));
    col += vec3(1.0, 0.42, 0.15) * sunLow * pow(sd, 3.0) * (1.0 - t) * 1.6;
    // mie halo around the sun
    col += sunColor() * (pow(sd, 18.0) * 0.18 + pow(sd, 400.0) * 1.2) * (1.0 - rainStrength * 0.8);
    // below the horizon fade towards the horizon colour
    if (y < 0.0) col = mix(col, horizon * 0.6, saturate(-y * 3.0));
    // rain: grey it out
    col = mix(col, vec3(luma(col)) * 0.55, rainStrength * 0.8);
    return col;
}

vec3 starsAndMoon(vec3 dir) {
    float night = 1.0 - dayFactor();
    vec3 c = vec3(0.0);
    if (night > 0.01) {
        vec3 w = mat3(gbufferModelViewInverse) * dir;
        vec2 p = w.xz / (abs(w.y) + 0.35) * 220.0;
        float s = hash12(floor(p));
        c += vec3(0.9, 0.95, 1.0) * step(0.9965, s) * 2.0 * night * saturate(w.y * 4.0) * (1.0 - rainStrength);
    }
    float md = dot(dir, normalize(moonPosition));
    c += vec3(0.85, 0.9, 1.0) * smoothstep(0.99955, 0.9997, md) * 3.0 * (1.0 - rainStrength);
    float sdisk = dot(dir, normalize(sunPosition));
    c += sunColor() * smoothstep(0.99935, 0.99955, sdisk) * 25.0;
    return c;
}

// A single layer of soft cumulus clouds drifting with the wind, lit by the sun.
vec4 clouds(vec3 dir) {
    vec3 w = mat3(gbufferModelViewInverse) * dir;
    if (w.y <= 0.02) return vec4(0.0);
    float h = 320.0 - cameraPosition.y;
    if (h < 10.0) h = 10.0;
    vec2 p = (cameraPosition.xz + w.xz / w.y * h) / 900.0;
    p += vec2(frameTimeCounter * 0.004, frameTimeCounter * 0.0015);
    float cover = mix(0.55, 0.25, rainStrength);
    float n = fbm2(p * 3.0, 5);
    float d = smoothstep(cover, cover + 0.35, n);
    float shade = fbm2(p * 3.0 + 0.04, 3);
    vec3 lit = mix(skyGradient(dir) * 0.6 + sunColor() * 0.25, vec3(1.0) * (0.35 + 0.9 * dayFactor()), 0.55);
    vec3 col = mix(lit, lit * 0.55, saturate((n - shade) * 3.0 + 0.3));
    col = mix(col, vec3(luma(col)) * 0.6, rainStrength);
    float fade = smoothstep(0.02, 0.25, w.y);
    return vec4(col, d * fade);
}

vec3 fullSky(vec3 dir) {
#ifdef DIM_NO_SKY
    vec3 c0 = pow(fogColor, vec3(2.2)) * 0.6;
    vec3 w0 = mat3(gbufferModelViewInverse) * dir;
    c0 += vec3(0.8, 0.85, 1.0) * step(0.997, hash12(floor(w0.xz / (abs(w0.y) + 0.35) * 220.0))) * 1.5;
    return c0;
#endif
    vec3 c = skyGradient(dir) + starsAndMoon(dir);
    vec4 cl = clouds(dir);
    return mix(c, cl.rgb, cl.a);
}

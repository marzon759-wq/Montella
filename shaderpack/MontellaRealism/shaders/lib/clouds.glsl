// Volumetric cumulus layer + cirrus sheet. World-space metres, y relative to sea level 63.

float cloudBottom() { return CLOUD_HEIGHT; }
float cloudTop()    { return CLOUD_HEIGHT + CLOUD_THICKNESS; }

vec3 cloudWind() { return vec3(1.0, 0.0, 0.35) * frameTimeCounter * 6.0 * CLOUD_SPEED; }

float cloudCoverage() { return mix(CLOUD_COVERAGE, 0.97, rainStrength); }

float cloudShape(vec3 p, bool detail) {
    float h = sat((p.y - cloudBottom()) / CLOUD_THICKNESS);
    vec3 q = (p + cloudWind()) * 0.00042;
    // large scale weather map decides where clouds are
    float weather = noise2s(q.xz * 0.35) * 0.65 + noise2s(q.xz * 0.9 + 13.0) * 0.35;
    float cov = cloudCoverage();
    float base = noise3(q) * 0.5 + noise3(q * 2.03 + 3.1) * 0.25 + noise3(q * 4.1 + 7.7) * 0.125;
    base = base / 0.875;
    // cumulus profile: flat-ish bottom, rounded top that depends on the weather map
    float top = mix(0.35, 1.0, sat(weather * 1.4 - 0.1));
    float profile = smoothstep(0.0, 0.08, h) * smoothstep(top, top * 0.55, h);
    float d = base * 0.55 + weather * 0.55 - (1.0 - cov);
    d = d * profile;
    if (detail && d > 0.0) {
        vec3 r = (p + cloudWind() * 1.6) * 0.0031;
        float e = noise3(r) * 0.62 + noise3(r * 2.7) * 0.38;
        d -= (1.0 - e) * 0.22 * (1.0 - h * 0.5);
    }
    return sat(d * 3.2) * mix(0.022, 0.06, rainStrength);
}

// sun light transmittance through the layer above a point (used for shadows on the ground)
float cloudShadow(vec3 worldPos, vec3 L) {
#ifdef CLOUD_SHADOWS
    if (L.y <= 0.02) return 1.0;
    float mid = (cloudBottom() + cloudTop()) * 0.5;
    vec3 p = worldPos + L * ((mid - (worldPos.y - 63.0)) / L.y);
    p.y = mid;
    float d = 0.0;
    d += cloudShape(p - L * 300.0 + vec3(0.0, -300.0, 0.0) * 0.0, false);
    d += cloudShape(p, false);
    d += cloudShape(p + L * 300.0, false);
    return mix(exp(-d * 120.0), 1.0, 0.3);
#else
    return 1.0;
#endif
}

vec2 raySlab(float oy, float dy, float y0, float y1) {
    if (abs(dy) < 1e-5) return vec2(-1.0);
    float t0 = (y0 - oy) / dy;
    float t1 = (y1 - oy) / dy;
    return vec2(min(t0, t1), max(t0, t1));
}

// returns rgb = in-scattered light, a = transmittance
vec4 volumetricClouds(vec3 dir, vec3 sunCol, vec3 ambCol, vec3 L, float dither, float maxDist) {
    float oy = max(eyeAltitude - 63.0, 0.0) + 50.0;
    vec2 t = raySlab(oy, dir.y, cloudBottom(), cloudTop());
    t.x = max(t.x, 0.0);
    if (t.y <= 0.0 || t.x >= t.y) return vec4(0.0, 0.0, 0.0, 1.0);
    t.y = min(t.y, t.x + 7000.0);
    if (t.x > 60000.0 || t.x > maxDist) return vec4(0.0, 0.0, 0.0, 1.0);
    int steps = CLOUD_STEPS;
    float ds = (t.y - t.x) / float(steps);
    vec3 origin = vec3(cameraPosition.x, oy, cameraPosition.z);
    float T = 1.0;
    vec3 S = vec3(0.0);
    float cosT = dot(dir, L);
    float phase = mix(henyeyGreenstein(cosT, 0.6), henyeyGreenstein(cosT, -0.25), 0.3) * 4.0 * PI;
    phase = mix(phase, 1.0, 0.25);
    float lds = CLOUD_THICKNESS / float(CLOUD_LIGHT_STEPS) * 0.55;
    float firstHit = -1.0;
    for (int i = 0; i < steps; i++) {
        float tt = t.x + ds * (float(i) + dither);
        vec3 p = origin + dir * tt;
        float d = cloudShape(p, true);
        if (d <= 0.0) continue;
        if (firstHit < 0.0) firstHit = tt;
        // light march toward the sun
        float od = 0.0;
        for (int j = 1; j <= CLOUD_LIGHT_STEPS; j++) {
            vec3 lp = p + L * lds * (float(j) - 0.5) * (1.0 + float(j) * 0.35);
            od += cloudShape(lp, j < 3) * lds * (1.0 + float(j) * 0.35);
        }
        float h = sat((p.y - cloudBottom()) / CLOUD_THICKNESS);
        float beer = exp(-od) * 0.6 + exp(-od * 0.2) * 0.3 + exp(-od * 0.05) * 0.1;   // multiple scattering hack
        float powder = 1.0 - exp(-d * ds * 2.0);
        powder = mix(1.0, powder, 0.5 + 0.5 * cosT * 0.5);
        vec3 lightIn = sunCol * beer * phase * mix(0.6, 1.0, powder) + ambCol * mix(0.5, 1.1, h) * 0.9;
        float stepT = exp(-d * ds);
        S += T * lightIn * (1.0 - stepT);
        T *= stepT;
        if (T < 0.01) break;
    }
    // aerial perspective: distant clouds fade into the sky
    float dist = firstHit < 0.0 ? t.x : firstHit;
    float fade = exp(-dist / mix(26000.0, 9000.0, rainStrength));
    S *= fade;
    T = mix(1.0, T, fade);
    return vec4(S, T);
}

vec4 cirrusClouds(vec3 dir, vec3 sunCol, vec3 ambCol, vec3 L) {
#ifdef CIRRUS_CLOUDS
    if (dir.y <= 0.0) return vec4(0.0, 0.0, 0.0, 1.0);
    float h = 7500.0;
    vec2 p = dir.xz * (h / dir.y);
    if (length(p) > 90000.0) return vec4(0.0, 0.0, 0.0, 1.0);
    p += cloudWind().xz * 2.5;
    vec2 q = p * 0.00009;
    q = vec2(q.x * 0.6 + q.y * 0.3, q.y * 2.2 - q.x * 0.4); // streaks
    float n = noise2s(q) * 0.5 + noise2s(q * 2.1 + 5.0) * 0.25 + noise2s(q * 4.3 + 9.0) * 0.15 + noise2s(q * 9.0) * 0.1;
    float m = noise2s(p * 0.000025 + 40.0);
    float d = sat((n - 0.52 + m * 0.25) * 2.5) * (1.0 - rainStrength * 0.5);
    d *= sat(dir.y * 4.0);
    float cosT = dot(dir, L);
    vec3 c = sunCol * (0.45 + henyeyGreenstein(cosT, 0.7) * 3.0) * 0.6 + ambCol * 0.35;
    float a = d * 0.38;
    return vec4(c * a, 1.0 - a);
#else
    return vec4(0.0, 0.0, 0.0, 1.0);
#endif
}

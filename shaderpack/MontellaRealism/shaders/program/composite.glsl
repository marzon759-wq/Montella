// Water (refraction, absorption, reflections, foam), wet ground, underwater, fog and god rays.
#include "/lib/common.glsl"
#include "/lib/uniforms.glsl"
#include "/lib/atmosphere.glsl"

#ifdef VSH
out vec2 uv;
flat out vec3 directCol;
flat out vec3 ambientCol;
void main() {
    uv = gl_MultiTexCoord0.xy;
    lightColors(directCol, ambientCol);
    gl_Position = ftransform();
}
#endif

#ifdef FSH
in vec2 uv;
flat in vec3 directCol;
flat in vec3 ambientCol;

uniform sampler2D colortex0;
uniform sampler2D colortex1;
uniform sampler2D colortex2;
uniform sampler2D colortex3;
uniform sampler2D colortex5;
uniform sampler2D depthtex0;
uniform sampler2D depthtex1;

#include "/lib/shadows.glsl"
#include "/lib/clouds.glsl"
#include "/lib/water.glsl"

/* RENDERTARGETS: 0 */
layout(location = 0) out vec4 outColor;

vec3 viewFromDepth(vec2 p, float d) { return projectDivide(gbufferProjectionInverse, vec3(p, d) * 2.0 - 1.0); }
vec3 toScreen(vec3 v) { return projectDivide(gbufferProjection, v) * 0.5 + 0.5; }

// screen space reflection; returns rgb + confidence
vec4 ssr(vec3 vpos, vec3 rdir, float dither) {
    float stepLen = 0.6 + length(vpos) * 0.03;
    vec3 p = vpos;
    vec3 prev = p;
    for (int i = 0; i < 28; i++) {
        prev = p;
        p += rdir * stepLen * (i == 0 ? dither + 0.2 : 1.0);
        stepLen *= 1.18;
        vec3 sp = toScreen(p);
        if (sp.x < 0.0 || sp.x > 1.0 || sp.y < 0.0 || sp.y > 1.0 || sp.z > 1.0 || p.z > -near) break;
        float d = texture(depthtex1, sp.xy).r;
        if (d >= 1.0) continue;
        float sceneZ = viewFromDepth(sp.xy, d).z;
        float diff = sceneZ - p.z;
        if (diff > 0.0 && diff < stepLen * 2.5 + 0.5) {
            // refine
            vec3 a = prev, b = p;
            for (int j = 0; j < 5; j++) {
                vec3 m = (a + b) * 0.5;
                vec3 ms = toScreen(m);
                float md = viewFromDepth(ms.xy, texture(depthtex1, ms.xy).r).z;
                if (md > m.z) b = m; else a = m;
            }
            vec3 hs = toScreen(b);
            vec2 e = sat(min(hs.xy, 1.0 - hs.xy) * 8.0);
            return vec4(texture(colortex0, hs.xy).rgb, e.x * e.y);
        }
    }
    return vec4(0.0);
}

// what a reflected ray sees when it misses the geometry: the rendered sky (with clouds) if visible, else the analytic sky
vec3 reflectedSky(vec3 Rw, vec3 Rv, float skyLm) {
    vec3 sky;
    vec3 sp = toScreen(Rv * 1000.0);
    if (Rv.z < 0.0 && sp.x > 0.0 && sp.x < 1.0 && sp.y > 0.0 && sp.y < 1.0 && texture(depthtex1, sp.xy).r >= 1.0) {
        sky = texture(colortex0, sp.xy).rgb;
    } else {
        sky = skyRadiance(vec3(Rw.x, abs(Rw.y), Rw.z), 8);
    }
    return sky * sat(skyLm * 1.3 - 0.15);
}

vec3 waterAbsorption() { return vec3(0.42, 0.075, 0.055); }

// how much water of the other kind is close by in the world: falling sheets above a pool surface (impact zone),
// or the pool surface below a falling sheet. Screen-space search, verified with reconstructed world positions.
float nearWater(vec2 p, vec3 ppos, float dist, bool wantFalling, float dither) {
    float rUv = min(4.0 * gbufferProjection[1][1] * 0.5 / max(dist, 0.5), 0.3);
    float acc = 0.0;
    for (int i = 0; i < 16; i++) {
        float fi = float(i) + dither;
        float a = fi * 2.39996;
        float r = sqrt(fi / 16.0);
        vec2 su = p + vec2(cos(a) / aspectRatio, sin(a)) * r * rUv;
        if (su.x < 0.0 || su.x > 1.0 || su.y < 0.0 || su.y > 1.0) continue;
        float b = texture(colortex3, su).b;
        if (b < 1.0 || (b >= 3.0 && b < 5.0) != wantFalling) continue;
        vec3 sp = mat3(gbufferModelViewInverse) * viewFromDepth(su, texture(depthtex0, su).r) + gbufferModelViewInverse[3].xyz;
        vec3 d = sp - ppos;
        float dh = length(d.xz);
        bool ok = wantFalling ? (d.y > -0.7 && d.y < 10.0) : (d.y < 0.7 && d.y > -3.5);
        if (ok && dh < 4.0) acc += 1.0 - dh / 4.0;
    }
    return sat(acc / 2.5);
}

void main() {
    vec3 col = texture(colortex0, uv).rgb;
    float d0 = texture(depthtex0, uv).r;
    float d1 = texture(depthtex1, uv).r;
    vec3 vpos0 = viewFromDepth(uv, d0);
    vec3 vpos1 = viewFromDepth(uv, d1);
    vec3 ppos0 = mat3(gbufferModelViewInverse) * vpos0 + gbufferModelViewInverse[3].xyz;
    vec3 V = normalize(ppos0);
    vec3 Vv = normalize(vpos0);
    float dither = ign(gl_FragCoord.xy);
    vec3 L = lightDirection();
    vec4 wdata = texture(colortex3, uv);
    bool water = wdata.b >= 1.0 && d0 < d1 + 1e-6 && d0 < 1.0;
    vec4 nl = texture(colortex1, uv);
    vec3 n = octDecode(nl.xy);
    float skyLm = nl.w;

    float cloudSh = cloudShadow(ppos0 + cameraPosition, L);

#if WORLD == 0
    if (water) {
        bool sheet = wdata.b >= 3.0 && wdata.b < 5.0;
        bool stream = wdata.b >= 5.0;
        vec3 tint = vec3(wdata.r, wdata.g, wdata.b - (sheet ? 3.0 : (stream ? 5.0 : 1.0)));
        vec3 wp = ppos0 + cameraPosition;
        float t = frameTimeCounter;
        float foam = wdata.a;
        float impact = 0.0;
#ifdef WATERFALL_FOAM
        if (isEyeInWater == 0) {
            impact = nearWater(uv, ppos0, length(ppos0), !sheet, dither);
            if (!sheet && impact > 0.0) {
                // churning pool at the foot of the fall: bubbling foam rings pushed outwards
                float b1 = noise2s(wp.xz * 2.3 + vec2(t * 0.9, -t * 0.6));
                float b2 = noise2s(wp.xz * 5.5 - vec2(t * 1.4, t * 1.1));
                float b3 = noise2s(wp.xz * 11.0 + vec2(-t * 2.0, t * 1.7));
                float pattern = b1 * 0.5 + b2 * 0.3 + b3 * 0.2;
                foam = max(foam, sat((pattern - (1.0 - impact) * 0.85 + 0.05) * 3.0) * 0.95);
                n = normalize(n + vec3(b1 - 0.5, 0.0, b2 - 0.5) * impact * 1.4);
            }
            if (sheet) foam = max(foam, impact * 0.85);
        }
#endif
        if (!sheet && isEyeInWater == 0) {
            // ---- shoreline: water depth straight down to whatever it touches (ground, blocks, mobs, the player)
            vec3 wb = mat3(gbufferModelViewInverse) * vpos1 + gbufferModelViewInverse[3].xyz + cameraPosition;
            float vdepth = d1 >= 1.0 ? 100.0 : max(wp.y - wb.y, 0.0) * mix(1.0, sat(abs(V.y) * 3.0 + 0.3), 0.5);
            float br1 = noise2s(wp.xz * 3.7 + vec2(t * 0.45, -t * 0.3));
            float br2 = noise2s(wp.xz * 9.0 - vec2(t * 0.8, t * 0.6));
            float broken = br1 * 0.65 + br2 * 0.35;
            // contact foam: a thin lacy line where the surface meets something
            float contact = 1.0 - smoothstep(0.0, stream ? 0.05 : 0.28, vdepth);
            contact *= smoothstep(0.25, 0.6, broken + contact * 0.35);
            // swells slowing down in the shallows, steepening and breaking as lines that roll towards the shore
            float shallow = (1.0 - smoothstep(0.25, 2.4, vdepth)) * (stream ? 0.0 : 1.0);
            float ph = vdepth * 3.0 + t * 1.25 + noise2s(wp.xz * 0.45) * 4.0;
            float crest = pow(sat(sin(ph) * 0.5 + 0.5), 10.0) * shallow;
            float band = crest * smoothstep(0.3, 0.62, broken + 0.1);
            // the foam a breaker leaves behind lingers and fades
            float trail = pow(sat(sin(ph - 0.9) * 0.5 + 0.5), 3.0) * shallow * smoothstep(0.45, 0.75, broken) * 0.45;
            foam = max(foam, sat(contact * 0.95 + band * 0.9 + trail * 0.5));
            // steeper water at the crest catches more light
            n = normalize(n + vec3(br1 - 0.5, 0.0, br2 - 0.5) * (crest + contact) * 0.6);

            // ---- the player touching the water: rings around the body and a wake behind when moving
            vec3 feet = cameraPosition - vec3(0.0, 1.62, 0.0);
            if (feet.y < wp.y + 0.35 && cameraPosition.y > wp.y - 0.6) {
                vec2 rel = wp.xz - cameraPosition.xz;
                float r = length(rel);
                vec2 vel = (cameraPosition.xz - previousCameraPosition.xz) / max(frameTime, 1e-3);
                float spd = min(length(vel), 6.0);
                float act = 0.35 + spd * 0.3;
                float ring = sin(r * 10.0 - t * 8.0) * exp(-r * (1.2 - min(spd, 4.0) * 0.15)) * smoothstep(0.25, 0.45, r);
                n = normalize(n + vec3(rel.x, 0.0, rel.y) / max(r, 1e-3) * ring * 0.35 * act);
                float body = (1.0 - smoothstep(0.3, 0.75, r)) * smoothstep(0.3, 0.7, broken + 0.2);
                foam = max(foam, body * sat(0.25 + spd * 0.18));
                if (spd > 0.4) {
                    vec2 v = vel / max(length(vel), 1e-3);
                    float along = dot(rel, -v);
                    float lat = abs(dot(rel, vec2(-v.y, v.x)));
                    // Kelvin wake: a 19.5 degree wedge of foam and ripples trailing behind
                    float wedge = smoothstep(0.42, 0.25, lat / max(along, 0.01)) * step(0.0, along) * exp(-along * 0.35);
                    float arms = smoothstep(0.08, 0.0, abs(lat - along * 0.354)) * step(0.0, along) * exp(-along * 0.3);
                    foam = max(foam, sat((wedge * 0.5 + arms) * smoothstep(0.35, 0.7, broken) * spd * 0.3));
                    n = normalize(n + vec3(-v.y, 0.0, v.x) * sin(along * 8.0 - t * 6.0) * wedge * 0.3);
                }
            }
        }
        vec3 nv = mat3(gbufferModelView) * n;
        // refraction
        vec2 ruv = uv;
        float wdist = length(vpos1 - vpos0);
#ifdef WATER_REFRACTION
        if (isEyeInWater == 0) {
            vec2 off = nv.xy * (sheet ? 0.05 : 0.06 * sat(wdist * 0.4)) / max(-vpos0.z * 0.12, 1.0);
            vec2 tuv = uv + off;
            float td1 = texture(depthtex1, tuv).r;
            if (td1 > d0) { ruv = tuv; vpos1 = viewFromDepth(tuv, td1); wdist = length(vpos1 - vpos0); }
        }
#endif
        vec3 behind = texture(colortex0, ruv).rgb;
        if (texture(depthtex1, ruv).r >= 1.0) wdist = 200.0;
        if (sheet) wdist = 0.6; // a falling sheet is thin
        // absorption + in-scattering of the water body
        vec3 absorb = waterAbsorption() * mix(vec3(1.0), vec3(1.4, 0.9, 1.0) - tint * 0.4, 0.35);
        vec3 transm = exp(-absorb * wdist);
        vec3 scatterCol = vec3(0.020, 0.075, 0.085) * (directCol * sat(L.y) * cloudSh * 0.9 + ambientCol * 0.6) * (skyLm * 0.9 + 0.1);
        vec3 refr = behind * transm + scatterCol * (1.0 - transm);
        if (isEyeInWater == 1) refr = behind;

        // reflection
        vec3 R = reflect(V, n);
        vec3 Rv = reflect(Vv, nv);
        vec3 refl = reflectedSky(R, Rv, skyLm);
#ifdef WATER_REFLECTIONS
        // rays bouncing back towards the camera cannot hit anything visible on screen
        if (Rv.z < -0.05) {
            vec4 s = ssr(vpos0, Rv, dither);
            refl = mix(refl, s.rgb, s.a * smoothstep(-0.05, -0.25, Rv.z));
        }
#endif
        float NdotV = sat(dot(-V, n));
        float F = 0.02 + 0.98 * pow(1.0 - NdotV, 5.0);
        if (isEyeInWater == 1) { F = NdotV < 0.66 ? 1.0 : F; refl = refl * 0.4; }
        // sun glint
        float wd;
        vec3 sh = shadowLight(ppos0, vec3(0.0, 1.0, 0.0), dither, 0.0, wd).rgb * cloudSh;
        vec3 H = normalize(L - V);
        float NdotH = sat(dot(n, H));
        float rough = 0.045;
        float a2 = rough * rough;
        float D = a2 / (PI * sq(NdotH * NdotH * (a2 - 1.0) + 1.0));
        vec3 spec = directCol * sh * D * F * 0.25 * sat(dot(n, L) * 4.0);
        vec3 wcol = mix(refr, refl, F) + spec;
        // foam / white water
        if (foam > 0.0) {
            // foam is a volume of bubbles: lit from all sides, light leaks through it
            vec3 foamLight = directCol * (sh * 0.75 + 0.25) * (0.55 + 0.45 * sat(dot(n, L) * 0.5 + 0.5))
                           + ambientCol * (skyLm * skyLm * 0.95 + 0.15);
            vec3 foamCol = vec3(0.88, 0.92, 0.94) * foamLight;
            foamCol += spec * 0.3;
            wcol = mix(wcol, foamCol, foam);
        }
        // spray / mist hanging around the impact zone
        wcol += (ambientCol * 0.6 + directCol * cloudSh * 0.3) * impact * impact * (sheet ? 0.55 : 0.4);
        col = wcol;
    }
#ifdef RAIN_PUDDLES
    else if (d0 < 1.0 && wetness > 0.01 && biome_precipitation == 1 && d0 > 0.56) {
        // wet ground: darken and reflect, puddles on flat surfaces
        vec3 wp = ppos0 + cameraPosition;
        float exposed = smoothstep(0.88, 0.97, skyLm);
        float flat_ = smoothstep(0.85, 0.98, n.y);
        float puddle = sat((noise2s(wp.xz * 0.35) * 0.7 + noise2s(wp.xz * 1.3) * 0.3 - 0.55) * 4.0) * flat_;
        float w = wetness * exposed;
        float refl = w * mix(0.15, 1.0, puddle);
        if (refl > 0.02) {
            vec3 nn = n;
            vec2 rip = rainRipples(wp.xz) * rainStrength * puddle;
            nn = normalize(nn + vec3(rip.x, 0.0, rip.y));
            vec3 nv = mat3(gbufferModelView) * nn;
            vec3 R = reflect(V, nn);
            vec3 Rv = reflect(Vv, nv);
            vec3 rc = reflectedSky(R, Rv, skyLm);
            vec4 s = ssr(vpos0, Rv, dither);
            rc = mix(rc, s.rgb, s.a);
            float F = 0.02 + 0.98 * pow(1.0 - sat(dot(-V, nn)), 5.0);
            col *= 1.0 - w * 0.35 * (1.0 - puddle * 0.5);
            col = mix(col, rc, F * refl);
        }
    }
#endif
#endif

    // ---------------------------------------------------------------- fog
    float dist = length(ppos0);
#if WORLD == 0
    if (isEyeInWater == 1) {
        // under water: absorb along the view ray
        float wd = d0 >= 1.0 ? 64.0 : dist;
        vec3 transm = exp(-waterAbsorption() * 1.1 * wd);
        float eye = float(eyeBrightnessSmooth.y) / 240.0;
        vec3 scat = vec3(0.03, 0.11, 0.12) * (directCol * cloudSh * 0.8 + ambientCol * 0.7) * (eye * 0.9 + 0.1);
        col = col * transm + scat * (1.0 - transm);
    } else if (d0 < 1.0) {
        vec3 wp = ppos0 + cameraPosition;
        float rain = rainStrength;
        float density = (0.0018 + rain * 0.010) * FOG_DENSITY;
        // height fog: thicker in valleys and over water
        float hf = exp(-max(wp.y - 62.0, 0.0) / 48.0) * 0.8 + 0.2;
        float eyeH = exp(-max(cameraPosition.y - 62.0, 0.0) / 48.0) * 0.8 + 0.2;
        float fogAmt = 1.0 - exp(-dist * density * (hf + eyeH) * 0.5);
        // sunrise / sunset haze
        vec3 sd = sunDirection();
        float lowSun = sat(1.0 - abs(sd.y) * 3.0);
        fogAmt = sat(fogAmt * (1.0 + lowSun * 0.6));
        vec3 fogCol = ambientCol * 0.55 + directCol * henyeyGreenstein(dot(V, L), 0.65) * 1.4 * (1.0 - rain);
        fogCol *= sat(float(eyeBrightnessSmooth.y) / 240.0 * 1.4 + 0.1);
        col = mix(col, fogCol, fogAmt);
        // blend the edge of the render distance into the sky
        float border = smoothstep(far * 0.72, far * 0.98, length(ppos0.xz));
        if (border > 0.0) {
            vec3 sky = skyRadiance(vec3(V.x, max(V.y, 0.0), V.z), 10);
            col = mix(col, sky, border);
        }
    }
#elif WORLD == -1
    if (d0 < 1.0) {
        float fa = 1.0 - exp(-dist * 0.010);
        col = mix(col, toLinear(fogColor) * 1.2 + vec3(0.05, 0.015, 0.01), fa);
    }
#elif WORLD == 1
    if (d0 < 1.0) {
        float fa = 1.0 - exp(-dist * 0.006);
        col = mix(col, vec3(0.012, 0.008, 0.02), fa);
    }
#endif

    // ---------------------------------------------------------------- god rays
#if WORLD == 0
#ifdef GODRAYS
    {
        float rayLen = min(d0 >= 1.0 ? shadowDistance : dist, shadowDistance);
        float VdotL = dot(V, L);
        float steps = float(GODRAY_STEPS);
        float lit = 0.0;
        for (int i = 0; i < GODRAY_STEPS; i++) {
            float t = (float(i) + dither) / steps;
            t = t * t;
            vec3 p = V * rayLen * t;
            vec3 sp = (shadowProjection * (shadowModelView * vec4(p, 1.0))).xyz;
            vec3 s = distortShadow(sp) * 0.5 + 0.5;
            float sh = 1.0;
            if (s.x > 0.0 && s.x < 1.0 && s.y > 0.0 && s.y < 1.0) sh = texture(shadowtex1, vec3(s.xy, s.z - 0.0002));
            lit += sh * cloudShadow(p + cameraPosition, L);
        }
        lit /= steps;
        float eye = sat(float(eyeBrightnessSmooth.y) / 240.0 * 1.5);
        vec3 sd = sunDirection();
        float lowSun = 1.0 + sat(1.0 - abs(sd.y) * 2.5) * 2.0;
        float density = (isEyeInWater == 1 ? 0.03 : 0.0022) * (1.0 + rainStrength * 2.0);
        float amount = (1.0 - exp(-rayLen * density));
        float phase = mix(henyeyGreenstein(VdotL, 0.78), henyeyGreenstein(VdotL, 0.2), 0.35) * 4.0 * PI;
        vec3 vl = directCol * lit * phase * amount * 0.28 * lowSun * GODRAY_STRENGTH * eye;
        if (isEyeInWater == 1) vl *= vec3(0.2, 0.7, 0.8);
        col += vl * (1.0 - rainStrength * 0.7);
    }
#endif
#endif

    // particles and rain, lit in their own pass, go on top of the finished water and fog
    vec4 parts = texture(colortex5, uv);
    col = col * (1.0 - parts.a) + parts.rgb;

    col = max(col, vec3(0.0));
    col *= 1.0 - blindness * 0.9;
    outColor = vec4(col, 1.0);
}
#endif

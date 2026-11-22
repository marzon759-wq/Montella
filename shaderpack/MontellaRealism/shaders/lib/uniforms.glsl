// every uniform the pack uses, declared once
uniform mat4 gbufferModelView;
uniform mat4 gbufferModelViewInverse;
uniform mat4 gbufferProjection;
uniform mat4 gbufferProjectionInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowModelViewInverse;
uniform mat4 shadowProjection;
uniform mat4 shadowProjectionInverse;

uniform vec3 cameraPosition;
uniform vec3 sunPosition;
uniform vec3 moonPosition;
uniform vec3 shadowLightPosition;

uniform float frameTimeCounter;
uniform float rainStrength;
uniform float wetness;
uniform float far;
uniform float near;
uniform float viewWidth;
uniform float viewHeight;
uniform float aspectRatio;
uniform float sunAngle;
uniform float nightVision;
uniform float blindness;
uniform float darknessFactor;
uniform float eyeAltitude;
uniform float screenBrightness;

uniform int isEyeInWater;
uniform int worldTime;
uniform int frameCounter;
uniform int biome_precipitation;
uniform ivec2 eyeBrightnessSmooth;
uniform vec3 fogColor;
uniform vec3 skyColor;

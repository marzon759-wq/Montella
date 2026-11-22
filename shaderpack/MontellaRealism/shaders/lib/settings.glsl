// ---------------------------------------------------------------------------------------------
//  Montella Realism - user settings (shown in the Oculus / Iris shader options menu)
// ---------------------------------------------------------------------------------------------

// Shadows
const int   shadowMapResolution = 3072;   // [1024 2048 3072 4096 6144 8192]
const float shadowDistance      = 160.0;  // [64.0 96.0 128.0 160.0 192.0 256.0]
const float sunPathRotation     = -28.0;  // [-45.0 -40.0 -35.0 -30.0 -28.0 -25.0 -20.0 -15.0 -10.0 0.0 10.0 20.0 30.0]
const bool  shadowHardwareFiltering1 = true;
const float shadowDistanceRenderMul  = 1.0;
const float drynessHalflife = 40.0;
const float wetnessHalflife = 120.0;
const float eyeBrightnessHalflife = 4.0;
const float ambientOcclusionLevel = 1.0;

#define SHADOW_FILTER 12      // [4 8 12 16 24]
#define SHADOW_SOFTNESS 1.0   // [0.5 0.75 1.0 1.5 2.0 3.0]
#define COLORED_SHADOWS

// Sky & clouds
#define VOLUMETRIC_CLOUDS
#define CLOUD_STEPS 28        // [12 16 20 28 36 48]
#define CLOUD_LIGHT_STEPS 5   // [3 4 5 6 8]
#define CLOUD_COVERAGE 0.42   // [0.30 0.35 0.40 0.42 0.45 0.50 0.55 0.60 0.65 0.70]
#define CLOUD_HEIGHT 1100.0   // [600.0 800.0 1100.0 1500.0 2000.0]
#define CLOUD_THICKNESS 1400.0 // [600.0 900.0 1400.0 2000.0]
#define CLOUD_SPEED 1.0       // [0.0 0.5 1.0 2.0 4.0]
#define CIRRUS_CLOUDS
#define CLOUD_SHADOWS
#define STARS
#define STAR_AMOUNT 1.0       // [0.5 1.0 1.5 2.0]

// Light
#define SUN_BRIGHTNESS 1.0    // [0.6 0.8 1.0 1.2 1.4]
#define SKY_LIGHT 1.0         // [0.5 0.75 1.0 1.25 1.5]
#define BLOCK_LIGHT 1.0       // [0.5 0.75 1.0 1.25 1.5 2.0]
#define NIGHT_BRIGHTNESS 1.0  // [0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define MIN_LIGHT 0.004       // [0.0 0.002 0.004 0.008 0.016]
#define SSAO
#define SUBSURFACE

// Atmosphere
#define FOG_DENSITY 1.0       // [0.0 0.25 0.5 0.75 1.0 1.5 2.0 3.0]
#define GODRAYS
#define GODRAY_STEPS 12       // [6 8 12 16 24]
#define GODRAY_STRENGTH 1.0   // [0.25 0.5 0.75 1.0 1.5 2.0]

// Water
#define WATER_WAVES
#define WAVE_HEIGHT 1.0       // [0.25 0.5 0.75 1.0 1.5 2.0]
#define WATER_REFLECTIONS
#define WATER_REFRACTION
#define WATER_CAUSTICS
#define WATERFALL_FOAM
#define RAIN_PUDDLES

// Plants
#define WAVING_PLANTS
#define WAVING_LEAVES
#define WIND_STRENGTH 1.0     // [0.0 0.5 0.75 1.0 1.5 2.0]

// Camera
#define EXPOSURE 0.0          // [-2.0 -1.5 -1.0 -0.5 0.0 0.5 1.0 1.5 2.0]
#define BLOOM
#define BLOOM_STRENGTH 1.0    // [0.0 0.25 0.5 0.75 1.0 1.5 2.0]
#define SATURATION 1.18       // [0.80 0.90 1.00 1.05 1.10 1.18 1.25 1.30 1.40]
#define CONTRAST 1.05         // [0.90 1.00 1.05 1.10 1.15 1.20]
#define VIGNETTE 0.35         // [0.0 0.15 0.25 0.35 0.5 0.7]
#define SHARPEN 0.35          // [0.0 0.2 0.35 0.5 0.75]
#define LABPBR_NORMALS

// Internal (not user options)
#define SHADOW_DISTORT 0.85

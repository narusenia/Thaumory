#version 330
#extension GL_ARB_separate_shader_objects : require

// GUI text with a Thaumory effect. SHIMMER: colours drift and sparkle inside the glyphs.
// STREAK: a band of light sweeps across the screen. Both only scale the glyph's own colour,
// so shadows stay dark.

#include <minecraft:globals.glsl>
#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;

layout(location = 0) out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    #ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
    #else
    vec4 texColor = texture(Sampler0, texCoord0);
    #endif

    vec4 color = texColor * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }

    // GameTime runs 0..1 over a day of 24000 ticks, so this is in seconds.
    float t = GameTime * 1200.0;
    float bright = max(vertexColor.r, max(vertexColor.g, vertexColor.b));

    #ifdef SHIMMER
    vec2 p = gl_FragCoord.xy / 3.0;
    float drift = noise(p * 0.45 + vec2(t * 0.9, -t * 0.6));
    color.rgb *= 0.55 + 0.9 * drift;
    float sparkle = step(0.985, hash(floor(p) + floor(t * 6.0)));
    color.rgb += sparkle * 0.7 * bright * bright;
    #endif

    #ifdef STREAK
    float period = ScreenSize.x + 600.0;
    float head = mod(t * 700.0, period) - 300.0;
    float along = gl_FragCoord.x + gl_FragCoord.y * 0.6;
    float band = 1.0 - smoothstep(0.0, 70.0, abs(along - head));
    color.rgb = color.rgb * (1.0 + 1.5 * band) + band * 0.6 * bright * bright;
    #endif

    fragColor = vec4(min(color.rgb, vec3(1.0)), color.a);
}

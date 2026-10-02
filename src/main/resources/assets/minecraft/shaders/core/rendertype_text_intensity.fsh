#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// dofis addons: vanilla uses texture(...).rrrr, so a TTF glyph's coverage scales the colour *and*
// the alpha; with normal alpha blending its anti-aliased edges come out darker than the background
// (a dark fringe around every letter). Use the coverage as alpha only, and keep faint edge pixels.
void main() {
    float coverage = texture(Sampler0, texCoord0).r;
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
    if (color.a < 0.01) {
        discard;
    }
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}

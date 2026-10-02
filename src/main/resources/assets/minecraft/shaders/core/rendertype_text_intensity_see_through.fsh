#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

// dofis addons: same fix as rendertype_text_intensity.fsh (coverage as alpha only, no dark fringe).
void main() {
    float coverage = texture(Sampler0, texCoord0).r;
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * coverage);
    if (color.a < 0.01) {
        discard;
    }
    fragColor = color * ColorModulator;
}

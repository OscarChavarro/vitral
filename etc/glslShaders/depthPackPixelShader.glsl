#version 410 core

// Packs the window depth of a 24 bit depth texture, texel by texel, into the
// red (high byte), green and blue (low byte) channels of an 8 bit color
// target, so it can be read back without loss with readPixels.

uniform highp sampler2D depthTexture;
uniform ivec2 viewportOrigin;

layout(location = 0) out vec4 fragColor;

void main()
{
    float depth = texelFetch(depthTexture, ivec2(gl_FragCoord.xy) + viewportOrigin, 0).r;
    uint value = uint(clamp(depth, 0.0, 1.0) * 16777215.0 + 0.5);

    fragColor = vec4(
        float((value >> 16u) & 255u),
        float((value >> 8u) & 255u),
        float(value & 255u),
        255.0) / 255.0;
}

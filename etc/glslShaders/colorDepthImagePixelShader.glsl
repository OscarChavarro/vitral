#version 410 core

// Writes an image computed outside OpenGL (i.e. by a raytracer) into both
// the color and the depth buffers, so rasterized geometry drawn afterwards is
// depth tested against it. The depth texture holds window space depth values
// (as OpenGL stores them, in the glDepthRange); texels are fetched without
// filtering, so each fragment gets exactly the depth of its pixel.

in vec2 uvTextureCoordinate;

// highp: GLSL ES (WebGL) samplers default to lowp in fragment shaders; the
// qualifier has no effect in desktop GLSL
uniform highp sampler2D colorTexture;
uniform highp sampler2D depthTexture;

layout(location = 0) out vec4 fragColor;

void main()
{
    ivec2 size = textureSize(colorTexture, 0);
    ivec2 texel = clamp(ivec2(uvTextureCoordinate * vec2(size)),
                        ivec2(0), size - ivec2(1));

    fragColor = vec4(texelFetch(colorTexture, texel, 0).rgb, 1.0);
    gl_FragDepth = texelFetch(depthTexture, texel, 0).r;
}

#version 410 core

// Screen aligned quad for colorDepthImagePixelShader.glsl: positions come
// in normalized device coordinates, so no transformation is applied.

layout(location = 0) in vec3 PObject;
layout(location = 2) in vec2 uvVertexTextureCoordinate;

out vec2 uvTextureCoordinate;

void main()
{
    gl_Position = vec4(PObject, 1.0);
    uvTextureCoordinate = uvVertexTextureCoordinate;
}

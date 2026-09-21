#version 410 core

layout(location = 0) in vec3 PObject;
layout(location = 1) in vec4 vertexColorIn;

uniform mat4 modelViewProjectionLocal;
uniform float depthBiasNdc;

out vec4 vertexColor;

void main()
{
    vec4 clipPosition = modelViewProjectionLocal * vec4(PObject, 1.0);
    clipPosition.z += depthBiasNdc * clipPosition.w;
    gl_Position = clipPosition;
    vertexColor = vertexColorIn;
}

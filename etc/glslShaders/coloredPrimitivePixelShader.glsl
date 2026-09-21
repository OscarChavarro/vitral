#version 410 core

in vec4 vertexColor;
layout(location = 0) out vec4 fragColor;

void main()
{
    fragColor = vertexColor;
}

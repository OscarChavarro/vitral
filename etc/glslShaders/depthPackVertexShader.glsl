#version 410 core

// Covers the whole viewport with one triangle, without vertex buffers.
// Used by the WebGL port to read back a depth texture (WebGL can not read a
// depth buffer with readPixels): see depthPackPixelShader.glsl.

void main()
{
    vec2 corners[3] = vec2[3](vec2(-1.0, -1.0), vec2(3.0, -1.0), vec2(-1.0, 3.0));
    gl_Position = vec4(corners[gl_VertexID], 0.0, 1.0);
}

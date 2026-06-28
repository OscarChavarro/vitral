#version 410 core

uniform mat4 modelViewProjectionLocal;
uniform mat4 modelViewLocal;
uniform mat4 modelViewITLocal;
uniform vec3 cameraPositionGlobal;
uniform vec3 boundingBoxMinObject;
uniform vec3 boundingBoxMaxObject;
uniform int clippingPlaneEnabled;
uniform vec4 clippingPlaneGlobal;

layout(location = 0) in vec4 PObject;
layout(location = 1) in vec3 NObject;

out vec3 PGlobal;
out vec3 N;
out vec3 V;
out vec3 textureCoordinate3D;

void main()
{
    vec4 transformed = modelViewLocal * PObject;
    vec3 extent = max(boundingBoxMaxObject - boundingBoxMinObject, vec3(0.000001));
    PGlobal = transformed.xyz;
    N = normalize((modelViewITLocal * vec4(NObject, 0.0)).xyz);
    V = normalize(cameraPositionGlobal - PGlobal);
    textureCoordinate3D = clamp((PObject.xyz - boundingBoxMinObject) / extent, 0.0, 1.0);
    gl_ClipDistance[0] = clippingPlaneEnabled != 0
        ? dot(clippingPlaneGlobal, vec4(PGlobal, 1.0))
        : 1.0;
    gl_Position = modelViewProjectionLocal * PObject;
}

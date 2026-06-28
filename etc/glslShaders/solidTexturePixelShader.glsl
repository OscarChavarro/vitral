#version 410 core

uniform sampler3D sSolidTexture;
uniform int numberOfLights;
uniform vec3 lightColorsGlobal[8];
uniform vec3 lightPositionsGlobal[8];
uniform float phongExponent;

in vec3 PGlobal;
in vec3 N;
in vec3 V;
in vec3 textureCoordinate3D;
layout(location = 0) out vec4 fragColor;

void main()
{
    vec3 normal = normalize(N);
    vec3 viewDir = normalize(V);
    vec3 textureElement = texture(sSolidTexture, textureCoordinate3D).rgb;
    vec3 diffuseTerm = vec3(0.0);
    vec3 specularTerm = vec3(0.0);

    for ( int i = 0; i < numberOfLights; i++ ) {
        vec3 L = normalize(lightPositionsGlobal[i] - PGlobal);
        vec3 R = reflect(-L, normal);
        diffuseTerm += lightColorsGlobal[i] * max(dot(normal, L), 0.0);
        specularTerm += lightColorsGlobal[i] * vec3(0.18) *
            pow(max(dot(R, viewDir), 0.0), phongExponent);
    }

    vec3 color = textureElement * (vec3(0.25) + diffuseTerm) + specularTerm;
    fragColor = vec4(color, 1.0);
}

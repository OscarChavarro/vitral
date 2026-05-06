#version 410 core

uniform sampler2D sTexture;
uniform sampler2D sNormalMap;
uniform int withTexture;
uniform int numberOfLights;
uniform vec3 lightColorsGlobal[8];
uniform vec3 lightPositionsGlobal[8];
uniform vec3 ambientColor;
uniform vec3 diffuseColor;
uniform vec3 specularColor;
uniform float phongExponent;
uniform vec3 bumpScale;

in vec3 PGlobal;
in vec3 N;
in vec3 V;
in vec2 uvTextureCoordinate;
in vec3 T;
in vec3 B;
layout(location = 0) out vec4 fragColor;

void main()
{
    // [BLIN1978b] "Simulation of Wrinkled Surfaces", Section 2.
    // We evaluate:
    //   N' = N + D
    //   D = (Fu (N x Pv) - Fv (N x Pu)) / |N|
    // using Fu/Fv reconstructed from the normal-map field exactly as in the
    // Java CPU path (NormalMap.importBumpMap + LightingShader).
    vec3 Nn = normalize(N);
    vec3 Pu = normalize(T);
    vec3 Pv = normalize(cross(Nn, Pu));

    vec3 normalVariation = texture(sNormalMap, uvTextureCoordinate).xyz * 2.0 - 1.0;
    float nz = normalVariation.z;
    if ( abs(nz) < 1e-6 ) {
        nz = (nz < 0.0) ? -1e-6 : 1e-6;
    }

    float Fu = -2.0 * (bumpScale.z / bumpScale.x) * (normalVariation.x / nz);
    float Fv = -2.0 * (bumpScale.z / bumpScale.y) * (normalVariation.y / nz);
    vec3 D = (Fu * cross(Nn, Pv) - Fv * cross(Nn, Pu));
    vec3 perturbedNormal = normalize(Nn + D);

    vec3 viewDir = normalize(V);
    vec3 ambientTerm = ambientColor;
    vec3 diffuseTerm = vec3(0.0);
    vec3 specularTerm = vec3(0.0);

    for ( int i = 0; i < numberOfLights; i++ ) {
        vec3 L = normalize(lightPositionsGlobal[i] - PGlobal);
        vec3 R = reflect(-L, perturbedNormal);
        diffuseTerm += lightColorsGlobal[i] * diffuseColor * max(dot(perturbedNormal, L), 0.0);
        specularTerm += lightColorsGlobal[i] * specularColor *
                        pow(max(dot(R, viewDir), 0.0), phongExponent);
    }

    // Match CPU raytracer: texture modulates diffuse term only.
    vec3 texturedDiffuseTerm = diffuseTerm;
    if ( withTexture > 0 ) {
        texturedDiffuseTerm *= texture(sTexture, uvTextureCoordinate).xyz;
    }
    vec3 color = ambientTerm + texturedDiffuseTerm + specularTerm;

    fragColor = vec4(color, 1.0);
}

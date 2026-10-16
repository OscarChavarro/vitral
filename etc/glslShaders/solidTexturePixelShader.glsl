#version 410 core

// Values of RendererConfiguration.SHADING_TYPE_*
const int SHADING_TYPE_NOLIGHT = 0;
const int SHADING_TYPE_FLAT = 1;
const int SHADING_TYPE_GOURAUD = 2;
const int SHADING_TYPE_PHONG = 3;
const int SHADING_TYPE_COOK_TERRANCE = 4;

const vec3 AMBIENT_TERM = vec3(0.25);
const vec3 UNTEXTURED_COLOR = vec3(0.8);
const float COOK_ROUGHNESS = 0.3;
const vec3 COOK_F0 = vec3(0.04);

// Unset uniforms read as 0, which keeps the textured Phong shading for the
// renderers that do not select a RendererConfiguration
uniform sampler3D sSolidTexture;
uniform int solidTextureDisabled;
uniform int shadingTypeSelected;
uniform int shadingType;
// When set (wires and points), fragments get constantColor with no lighting
uniform int constantColorEnabled;
uniform vec3 constantColor;
uniform int numberOfLights;
uniform vec3 lightColorsGlobal[8];
uniform vec3 lightPositionsGlobal[8];
uniform float phongExponent;

in vec3 PGlobal;
in vec3 N;
in vec3 V;
in vec3 textureCoordinate3D;
in vec3 vertexDiffuseTerm;
in vec3 vertexSpecularTerm;
layout(location = 0) out vec4 fragColor;

vec3 phongShading(vec3 baseColor, vec3 normal, vec3 viewDir)
{
    vec3 diffuseTerm = vec3(0.0);
    vec3 specularTerm = vec3(0.0);

    for ( int i = 0; i < numberOfLights; i++ ) {
        vec3 L = normalize(lightPositionsGlobal[i] - PGlobal);
        vec3 R = reflect(-L, normal);
        diffuseTerm += lightColorsGlobal[i] * max(dot(normal, L), 0.0);
        specularTerm += lightColorsGlobal[i] * vec3(0.18) *
            pow(max(dot(R, viewDir), 0.0), phongExponent);
    }

    return baseColor * (AMBIENT_TERM + diffuseTerm) + specularTerm;
}

vec3 cookTorranceShading(vec3 baseColor, vec3 normal, vec3 viewDir)
{
    vec3 diffuseTerm = vec3(0.0);
    vec3 specularTerm = vec3(0.0);
    float mSquared = COOK_ROUGHNESS * COOK_ROUGHNESS;
    float k = mSquared * 0.5;
    float ndotV = max(dot(normal, viewDir), 0.0);

    for ( int i = 0; i < numberOfLights; i++ ) {
        vec3 L = normalize(lightPositionsGlobal[i] - PGlobal);
        float ndotL = max(dot(normal, L), 0.0);
        if ( ndotL <= 0.0 || ndotV <= 0.0 ) {
            continue;
        }
        vec3 H = normalize(L + viewDir);
        float ndotH = max(dot(normal, H), 1e-4);
        float vdotH = max(dot(viewDir, H), 0.0);

        // Beckmann distribution, Smith-Schlick geometry, Schlick Fresnel
        float ndotHSquared = ndotH * ndotH;
        float tanSquaredTheta = (1.0 - ndotHSquared) / ndotHSquared;
        float D = exp(-tanSquaredTheta / mSquared) /
            (3.14159265 * mSquared * ndotHSquared * ndotHSquared);
        float G = (ndotV / (ndotV * (1.0 - k) + k)) *
            (ndotL / (ndotL * (1.0 - k) + k));
        vec3 F = COOK_F0 + (vec3(1.0) - COOK_F0) * pow(1.0 - vdotH, 5.0);

        diffuseTerm += lightColorsGlobal[i] * ndotL;
        specularTerm += lightColorsGlobal[i] * (D * G / (4.0 * ndotL * ndotV)) * F;
    }

    return baseColor * (AMBIENT_TERM + diffuseTerm) + specularTerm;
}

void main()
{
    if ( constantColorEnabled != 0 ) {
        fragColor = vec4(constantColor, 1.0);
        return;
    }

    vec3 baseColor = solidTextureDisabled != 0
        ? UNTEXTURED_COLOR
        : texture(sSolidTexture, textureCoordinate3D).rgb;
    vec3 viewDir = normalize(V);
    int selectedShading = shadingTypeSelected != 0 ? shadingType : SHADING_TYPE_PHONG;
    vec3 color;

    if ( selectedShading == SHADING_TYPE_NOLIGHT ) {
        color = baseColor;
    }
    else if ( selectedShading == SHADING_TYPE_FLAT ) {
        // Face normal from the screen space derivatives of the position
        vec3 faceNormal = normalize(cross(dFdx(PGlobal), dFdy(PGlobal)));
        if ( dot(faceNormal, viewDir) < 0.0 ) {
            faceNormal = -faceNormal;
        }
        color = phongShading(baseColor, faceNormal, viewDir);
    }
    else if ( selectedShading == SHADING_TYPE_GOURAUD ) {
        color = baseColor * (AMBIENT_TERM + vertexDiffuseTerm) + vertexSpecularTerm;
    }
    else if ( selectedShading == SHADING_TYPE_COOK_TERRANCE ) {
        color = cookTorranceShading(baseColor, normalize(N), viewDir);
    }
    else {
        color = phongShading(baseColor, normalize(N), viewDir);
    }

    fragColor = vec4(color, 1.0);
}

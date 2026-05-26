#include "vsdk/toolkit/render/opengl4/OpenGL4SphereRenderer.h"

#include <GL/glew.h>
#include <GL/gl.h>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/MicroFacetedMaterial.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"

#include "java/util/ArrayList.txx"
#include <algorithm>
#include <cmath>
#include <cstdio>
#include <fstream>
#include <string>

namespace vsdk { namespace toolkit { namespace render { namespace opengl4 {

unsigned int OpenGL4SphereRenderer::vao_ = 0;
unsigned int OpenGL4SphereRenderer::vboPositions_ = 0;
unsigned int OpenGL4SphereRenderer::vboNormals_ = 0;
unsigned int OpenGL4SphereRenderer::vboUvs_ = 0;
unsigned int OpenGL4SphereRenderer::vboTangents_ = 0;
unsigned int OpenGL4SphereRenderer::vboBinormals_ = 0;
unsigned int OpenGL4SphereRenderer::ebo_ = 0;
unsigned int OpenGL4SphereRenderer::constantProgram_ = 0;
unsigned int OpenGL4SphereRenderer::texturedProgram_ = 0;
unsigned int OpenGL4SphereRenderer::flatProgram_ = 0;
unsigned int OpenGL4SphereRenderer::flatTexturedProgram_ = 0;
unsigned int OpenGL4SphereRenderer::gouraudProgram_ = 0;
unsigned int OpenGL4SphereRenderer::phongProgram_ = 0;
unsigned int OpenGL4SphereRenderer::phongBumpProgram_ = 0;
unsigned int OpenGL4SphereRenderer::cookProgram_ = 0;
unsigned int OpenGL4SphereRenderer::cookBumpProgram_ = 0;

int OpenGL4SphereRenderer::cachedMeridians_ = -1;
int OpenGL4SphereRenderer::cachedParallels_ = -1;
unsigned int OpenGL4SphereRenderer::indexCount_ = 0;

static const Vector3Dd DEFAULT_BUMP_SCALE(1.0, 1.0, 1.0);

static std::string readTextFile(const std::string& path)
{
    std::ifstream f(path.c_str(), std::ios::in | std::ios::binary);
    if (!f.good()) return std::string();
    std::string s;
    f.seekg(0, std::ios::end);
    s.resize((size_t)f.tellg());
    f.seekg(0, std::ios::beg);
    if (!s.empty()) f.read(&s[0], (std::streamsize)s.size());
    return s;
}

static std::string findShaderSource(const std::string& shaderFileName)
{
    const char* candidates[] = {
        "../../../../etc/glslShaders/", // from testsuite/OpenGL4Examples/ShadersExample/build
        "../../../etc/glslShaders/",
        "../../etc/glslShaders/",
        "../etc/glslShaders/",
        "etc/glslShaders/"
    };
    for (size_t i = 0; i < sizeof(candidates)/sizeof(candidates[0]); i++) {
        std::string path = std::string(candidates[i]) + shaderFileName;
        std::string src = readTextFile(path);
        if (!src.empty()) return src;
    }
    return std::string();
}

unsigned int OpenGL4SphereRenderer::compileShader(unsigned int type, const char* source)
{
    unsigned int shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    int ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
        std::fprintf(stderr, "OpenGL4SphereRenderer shader compile error: %s\n", log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

static unsigned int buildProgram(const char* vsFile, const char* fsFile)
{
    std::string vsSource = findShaderSource(vsFile);
    std::string fsSource = findShaderSource(fsFile);
    if (vsSource.empty() || fsSource.empty()) {
        std::fprintf(stderr, "OpenGL4SphereRenderer shader not found: %s / %s\n", vsFile, fsFile);
        return 0;
    }

    auto compileLocal = [](unsigned int type, const char* source) -> unsigned int {
        unsigned int shader = glCreateShader(type);
        glShaderSource(shader, 1, &source, nullptr);
        glCompileShader(shader);
        int ok = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
        if (!ok) {
            char log[4096];
            glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
            std::fprintf(stderr, "OpenGL4SphereRenderer shader compile error: %s\n", log);
            glDeleteShader(shader);
            return 0;
        }
        return shader;
    };

    unsigned int vs = compileLocal(GL_VERTEX_SHADER, vsSource.c_str());
    unsigned int fs = compileLocal(GL_FRAGMENT_SHADER, fsSource.c_str());
    if (vs == 0 || fs == 0) {
        if (vs) glDeleteShader(vs);
        if (fs) glDeleteShader(fs);
        return 0;
    }

    unsigned int program = glCreateProgram();
    glAttachShader(program, vs);
    glAttachShader(program, fs);
    glLinkProgram(program);
    glDeleteShader(vs);
    glDeleteShader(fs);

    int ok = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &ok);
    if (!ok) {
        char log[4096];
        glGetProgramInfoLog(program, sizeof(log), nullptr, log);
        std::fprintf(stderr, "OpenGL4SphereRenderer link error: %s\n", log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

bool OpenGL4SphereRenderer::initProgramIfNeeded()
{
    if (constantProgram_ != 0) return true;

    constantProgram_ = buildProgram("constantVertexShader.glsl", "constantPixelShader.glsl");
    texturedProgram_ = buildProgram("constantTextureVertexShader.glsl", "constantTexturePixelShader.glsl");
    flatProgram_ = buildProgram("flatVertexShader.glsl", "flatPixelShader.glsl");
    flatTexturedProgram_ = buildProgram("flatTexturedVertexShader.glsl", "flatTexturedPixelShader.glsl");
    gouraudProgram_ = buildProgram("gouraudTextureVertexShader.glsl", "gouraudTexturePixelShader.glsl");
    phongProgram_ = buildProgram("phongTextureVertexShader.glsl", "phongTexturePixelShader.glsl");
    phongBumpProgram_ = buildProgram("phongTextureBumpVertexShader.glsl", "phongTextureBumpPixelShader.glsl");
    cookProgram_ = buildProgram("phongTextureVertexShader.glsl", "cookTexturePixelShader.glsl");
    cookBumpProgram_ = buildProgram("phongTextureBumpVertexShader.glsl", "cookTextureBumpPixelShader.glsl");

    if (!constantProgram_ || !texturedProgram_ || !flatProgram_ || !flatTexturedProgram_ ||
        !gouraudProgram_ || !phongProgram_ || !phongBumpProgram_ || !cookProgram_ || !cookBumpProgram_) {
        return false;
    }

    glGenVertexArrays(1, &vao_);
    glGenBuffers(1, &vboPositions_);
    glGenBuffers(1, &vboNormals_);
    glGenBuffers(1, &vboUvs_);
    glGenBuffers(1, &vboTangents_);
    glGenBuffers(1, &vboBinormals_);
    glGenBuffers(1, &ebo_);
    return true;
}

unsigned int OpenGL4SphereRenderer::selectProgram(const RendererConfiguration* quality, bool hasTexture, bool hasNormalMap)
{
    if (quality == nullptr) {
        return hasTexture ? OpenGL4SphereRenderer::texturedProgram_ : OpenGL4SphereRenderer::constantProgram_;
    }

    int shadingType = quality->getShadingType();
    if (shadingType == RendererConfiguration::SHADING_TYPE_NOLIGHT) {
        return (quality->isTextureSet() && hasTexture)
            ? OpenGL4SphereRenderer::texturedProgram_ : OpenGL4SphereRenderer::constantProgram_;
    }
    if (shadingType == RendererConfiguration::SHADING_TYPE_FLAT) {
        return (quality->isTextureSet() && hasTexture)
            ? OpenGL4SphereRenderer::flatTexturedProgram_ : OpenGL4SphereRenderer::flatProgram_;
    }
    if (shadingType == RendererConfiguration::SHADING_TYPE_PHONG) {
        if (quality->isBumpMapSet() && hasNormalMap) return OpenGL4SphereRenderer::phongBumpProgram_;
        return OpenGL4SphereRenderer::phongProgram_;
    }
    if (shadingType == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE) {
        if (quality->isBumpMapSet() && hasNormalMap) return OpenGL4SphereRenderer::cookBumpProgram_;
        return OpenGL4SphereRenderer::cookProgram_;
    }
    return OpenGL4SphereRenderer::gouraudProgram_;
}

bool OpenGL4SphereRenderer::buildSphereMeshIfNeeded(int meridians, int parallels)
{
    meridians = std::max(12, meridians);
    parallels = std::max(8, parallels);
    if (cachedMeridians_ == meridians && cachedParallels_ == parallels && indexCount_ > 0) {
        return true;
    }

    Sphere unitSphere(1.0);
    long int vertexCount = (long int)(parallels + 1) * (long int)(meridians + 1);
    java::ArrayList<float> positions;
    java::ArrayList<float> normals;
    java::ArrayList<float> uvs;
    java::ArrayList<float> tangents;
    java::ArrayList<float> binormals;
    java::ArrayList<unsigned int> indices;
    positions.reserve(vertexCount * 3);
    normals.reserve(vertexCount * 3);
    tangents.reserve(vertexCount * 3);
    binormals.reserve(vertexCount * 3);
    uvs.reserve(vertexCount * 2);
    indices.reserve((long int)parallels * (long int)meridians * 6L);

    for (int p = 0; p <= parallels; ++p) {
        double t = static_cast<double>(p) / static_cast<double>(parallels);
        double phi = M_PI * t - M_PI / 2.0;
        for (int m = 0; m <= meridians; ++m) {
            double s = static_cast<double>(m) / static_cast<double>(meridians);
            double theta = 2.0 * M_PI * s;

            Vector3Dd pos = unitSphere.spherePosition(theta, phi);
            Vector3Dd nrm = unitSphere.sphereNormal(theta, phi);
            Vector3Dd tan = unitSphere.sphereTangent(theta, phi);
            Vector3Dd bin = unitSphere.sphereBinormal(theta, phi);

            positions.add((float)pos.x());
            positions.add((float)pos.y());
            positions.add((float)pos.z());
            normals.add((float)nrm.x());
            normals.add((float)nrm.y());
            normals.add((float)nrm.z());
            tangents.add((float)tan.x());
            tangents.add((float)tan.y());
            tangents.add((float)tan.z());
            binormals.add((float)bin.x());
            binormals.add((float)bin.y());
            binormals.add((float)bin.z());
            uvs.add((float)(1.0 - s)); // Java parity
            uvs.add((float)t);
        }
    }

    int row = meridians + 1;
    for (int p = 0; p < parallels; ++p) {
        for (int m = 0; m < meridians; ++m) {
            unsigned int i0 = static_cast<unsigned int>(p * row + m);
            unsigned int i1 = i0 + 1;
            unsigned int i2 = i0 + row;
            unsigned int i3 = i2 + 1;
            indices.add(i0); indices.add(i2); indices.add(i1);
            indices.add(i1); indices.add(i2); indices.add(i3);
        }
    }

    glBindVertexArray(vao_);

    glBindBuffer(GL_ARRAY_BUFFER, vboPositions_);
    glBufferData(GL_ARRAY_BUFFER, positions.size() * sizeof(float), positions.data(), GL_STATIC_DRAW);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, nullptr);
    glEnableVertexAttribArray(0);

    glBindBuffer(GL_ARRAY_BUFFER, vboNormals_);
    glBufferData(GL_ARRAY_BUFFER, normals.size() * sizeof(float), normals.data(), GL_STATIC_DRAW);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, nullptr);
    glEnableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, vboUvs_);
    glBufferData(GL_ARRAY_BUFFER, uvs.size() * sizeof(float), uvs.data(), GL_STATIC_DRAW);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, nullptr);
    glEnableVertexAttribArray(2);

    glBindBuffer(GL_ARRAY_BUFFER, vboTangents_);
    glBufferData(GL_ARRAY_BUFFER, tangents.size() * sizeof(float), tangents.data(), GL_STATIC_DRAW);
    glVertexAttribPointer(3, 3, GL_FLOAT, GL_FALSE, 0, nullptr);
    glEnableVertexAttribArray(3);

    glBindBuffer(GL_ARRAY_BUFFER, vboBinormals_);
    glBufferData(GL_ARRAY_BUFFER, binormals.size() * sizeof(float), binormals.data(), GL_STATIC_DRAW);
    glVertexAttribPointer(4, 3, GL_FLOAT, GL_FALSE, 0, nullptr);
    glEnableVertexAttribArray(4);

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo_);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.size() * sizeof(unsigned int), indices.data(), GL_STATIC_DRAW);

    glBindVertexArray(0);

    cachedMeridians_ = meridians;
    cachedParallels_ = parallels;
    indexCount_ = (unsigned int)indices.size();
    return true;
}

static void setUniform3f(unsigned int program, const char* name, const Vector3Dd& v)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform3f(loc, (float)v.x(), (float)v.y(), (float)v.z());
}

static void setUniform3f(unsigned int program, const char* name, const ColorRgb& c)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform3f(loc, (float)c.r(), (float)c.g(), (float)c.b());
}

static void setUniform1i(unsigned int program, const char* name, int v)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform1i(loc, v);
}

static void setUniform1f(unsigned int program, const char* name, float v)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform1f(loc, v);
}

static void configureMicrofacetUniforms(unsigned int programId, const SimpleMaterial* material)
{
    float roughness = 0.35f;
    float alpha = roughness * roughness;
    ColorRgb fresnelF0 = material->getSpecular();
    float kd = 1.0f;
    float ks = 1.0f;
    int fresnelModel = MicroFacetedMaterial::FRESNEL_MODEL_SCHLICK;
    int ndfModel = MicroFacetedMaterial::NDF_MODEL_BECKMANN;
    int geometryModel = MicroFacetedMaterial::GEOMETRY_MODEL_SMITH;
    ColorRgb eta(1.5, 1.5, 1.5);
    ColorRgb kappa(0.0, 0.0, 0.0);

    const MicroFacetedMaterial* mf = dynamic_cast<const MicroFacetedMaterial*>(material);
    if (mf) {
        roughness = (float)mf->getRoughness();
        alpha = (float)mf->getAlpha();
        fresnelF0 = mf->getFresnelF0();
        kd = (float)mf->getKd();
        ks = (float)mf->getKs();
        fresnelModel = mf->getFresnelModel();
        ndfModel = mf->getNdfModel();
        geometryModel = mf->getGeometryModel();
        eta = mf->getEta();
        kappa = mf->getKappa();
    }

    setUniform1f(programId, "cookRoughness", roughness);
    setUniform1f(programId, "cookAlpha", alpha);
    setUniform1f(programId, "cookKd", kd);
    setUniform1f(programId, "cookKs", ks);
    setUniform3f(programId, "cookF0", fresnelF0);
    setUniform3f(programId, "cookEta", eta);
    setUniform3f(programId, "cookKappa", kappa);
    setUniform1i(programId, "cookFresnelModel", fresnelModel);
    setUniform1i(programId, "cookNdfModel", ndfModel);
    setUniform1i(programId, "cookGeometryModel", geometryModel);
}

void OpenGL4SphereRenderer::draw(
    const Sphere* sphere,
    const Camera* camera,
    const Light* light,
    const SimpleMaterial* material,
    const RendererConfiguration* quality,
    RGBImageUncompressed* textureMap,
    RGBImageUncompressed* bumpMapHeightRgb,
    const Matrix4x4d& modelRotation,
    int meridians,
    int parallels)
{
    if (sphere == nullptr || camera == nullptr || light == nullptr || material == nullptr || quality == nullptr) {
        return;
    }
    if (!initProgramIfNeeded() || !buildSphereMeshIfNeeded(meridians, parallels)) {
        return;
    }

    bool hasTexture = (textureMap != nullptr);
    bool hasNormalMap = (bumpMapHeightRgb != nullptr);
    int textureId = hasTexture ? OpenGL4ImageRenderer::activate(textureMap) : 0;
    int normalMapId = (hasNormalMap && quality->isBumpMapSet()) ? OpenGL4ImageRenderer::activate(bumpMapHeightRgb) : 0;

    unsigned int programId = OpenGL4SphereRenderer::selectProgram(quality, hasTexture, normalMapId > 0);
    if (programId == 0) return;

    Matrix4x4d localTransform = modelRotation.multiply(
        Matrix4x4d().scale(sphere->getRadius(), sphere->getRadius(), sphere->getRadius()));
    Matrix4x4d modelViewProjection = camera->calculateProjectionMatrix().multiply(localTransform);
    Matrix4x4d modelViewITLocal = localTransform.invert().transpose();

    float* mvpFloat = modelViewProjection.exportToFloatArrayColumnOrder();
    float* modelFloat = localTransform.exportToFloatArrayColumnOrder();
    float* modelItFloat = modelViewITLocal.exportToFloatArrayColumnOrder();

    glUseProgram(programId);

    GLint mvpLoc = glGetUniformLocation(programId, "modelViewProjectionLocal");
    if (mvpLoc >= 0) glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, mvpFloat);
    GLint modelLoc = glGetUniformLocation(programId, "modelViewLocal");
    if (modelLoc >= 0) glUniformMatrix4fv(modelLoc, 1, GL_FALSE, modelFloat);
    GLint modelItLoc = glGetUniformLocation(programId, "modelViewITLocal");
    if (modelItLoc >= 0) glUniformMatrix4fv(modelItLoc, 1, GL_FALSE, modelItFloat);

    setUniform3f(programId, "cameraPositionGlobal", camera->getPosition());
    setUniform3f(programId, "lightPositionsGlobal[0]", light->getPosition());
    setUniform3f(programId, "lightColorsGlobal[0]", light->getSpecular());
    setUniform1i(programId, "numberOfLights", 1);

    setUniform3f(programId, "ambientColor", material->getAmbient());
    setUniform3f(programId, "diffuseColor", material->getDiffuse());
    setUniform3f(programId, "specularColor", material->getSpecular());
    setUniform3f(programId, "bumpScale", DEFAULT_BUMP_SCALE);
    setUniform1f(programId, "phongExponent", (float)material->getPhongExponent());
    configureMicrofacetUniforms(programId, material);

    setUniform1i(programId, "withTexture", (quality->isTextureSet() && textureId > 0) ? 1 : 0);
    setUniform1i(programId, "withBumpMap", (quality->isBumpMapSet() && normalMapId > 0) ? 1 : 0);
    setUniform1i(programId, "withVertexColors", 0);
    setUniform1i(programId, "sTexture", 0);
    setUniform1i(programId, "sNormalMap", 1);

    if (textureId > 0) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, (unsigned int)textureId);
    }
    if (normalMapId > 0) {
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, (unsigned int)normalMapId);
        glActiveTexture(GL_TEXTURE0);
    }

    glBindVertexArray(vao_);

    if (quality->isSurfacesSet()) {
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_TRUE);
        glDepthFunc(GL_LESS);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDrawElements(GL_TRIANGLES, (GLsizei)indexCount_, GL_UNSIGNED_INT, nullptr);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    if (quality->isWiresSet()) {
        unsigned int wireProgram = OpenGL4SphereRenderer::selectProgram(nullptr, false, false);
        glUseProgram(wireProgram);
        GLint wireMvp = glGetUniformLocation(wireProgram, "modelViewProjectionLocal");
        if (wireMvp >= 0) glUniformMatrix4fv(wireMvp, 1, GL_FALSE, mvpFloat);
        GLint wireDiffuse = glGetUniformLocation(wireProgram, "diffuseColor");
        if (wireDiffuse >= 0) glUniform3f(wireDiffuse, 1.0f, 1.0f, 1.0f);
        setUniform1i(wireProgram, "withTexture", 0);
        setUniform1i(wireProgram, "withVertexColors", 0);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-1.0f, -1.0f);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(1.0f);
        glDrawElements(GL_TRIANGLES, (GLsizei)indexCount_, GL_UNSIGNED_INT, nullptr);
        glDisable(GL_POLYGON_OFFSET_LINE);
    }

    if (quality->isPointsSet()) {
        unsigned int pointsProgram = OpenGL4SphereRenderer::selectProgram(nullptr, false, false);
        glUseProgram(pointsProgram);
        GLint pointsMvp = glGetUniformLocation(pointsProgram, "modelViewProjectionLocal");
        if (pointsMvp >= 0) glUniformMatrix4fv(pointsMvp, 1, GL_FALSE, mvpFloat);
        GLint pointsDiffuse = glGetUniformLocation(pointsProgram, "diffuseColor");
        if (pointsDiffuse >= 0) glUniform3f(pointsDiffuse, 1.0f, 0.0f, 0.0f);
        setUniform1i(pointsProgram, "withTexture", 0);
        setUniform1i(pointsProgram, "withVertexColors", 0);

        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDepthFunc(GL_LEQUAL);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
        glPointSize(4.0f);
        glDrawElements(GL_TRIANGLES, (GLsizei)indexCount_, GL_UNSIGNED_INT, nullptr);
    }

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);

    delete[] mvpFloat;
    delete[] modelFloat;
    delete[] modelItFloat;
}

void OpenGL4SphereRenderer::dispose()
{
    if (vao_ != 0) { glDeleteVertexArrays(1, &vao_); vao_ = 0; }
    if (vboPositions_ != 0) { glDeleteBuffers(1, &vboPositions_); vboPositions_ = 0; }
    if (vboNormals_ != 0) { glDeleteBuffers(1, &vboNormals_); vboNormals_ = 0; }
    if (vboUvs_ != 0) { glDeleteBuffers(1, &vboUvs_); vboUvs_ = 0; }
    if (vboTangents_ != 0) { glDeleteBuffers(1, &vboTangents_); vboTangents_ = 0; }
    if (vboBinormals_ != 0) { glDeleteBuffers(1, &vboBinormals_); vboBinormals_ = 0; }
    if (ebo_ != 0) { glDeleteBuffers(1, &ebo_); ebo_ = 0; }

    unsigned int programs[] = {
        constantProgram_, texturedProgram_, flatProgram_, flatTexturedProgram_, gouraudProgram_,
        phongProgram_, phongBumpProgram_, cookProgram_, cookBumpProgram_ };
    for (size_t i = 0; i < sizeof(programs)/sizeof(programs[0]); i++) {
        if (programs[i] != 0) glDeleteProgram(programs[i]);
    }
    constantProgram_ = texturedProgram_ = flatProgram_ = flatTexturedProgram_ = gouraudProgram_ = 0;
    phongProgram_ = phongBumpProgram_ = cookProgram_ = cookBumpProgram_ = 0;

    cachedMeridians_ = -1;
    cachedParallels_ = -1;
    indexCount_ = 0;
}

}}}}

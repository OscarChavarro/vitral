#include "OpenGL4SphereRenderer.h"

#include <GL/glew.h>
#include <GL/gl.h>

#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "OpenGL4ImageRenderer.h"

#include <vector>
#include <cmath>
#include <cstdio>

namespace vsdk { namespace toolkit { namespace render { namespace opengl4 {

unsigned int OpenGL4SphereRenderer::vao_ = 0;
unsigned int OpenGL4SphereRenderer::vboPositions_ = 0;
unsigned int OpenGL4SphereRenderer::vboNormals_ = 0;
unsigned int OpenGL4SphereRenderer::vboUvs_ = 0;
unsigned int OpenGL4SphereRenderer::ebo_ = 0;
unsigned int OpenGL4SphereRenderer::program_ = 0;
int OpenGL4SphereRenderer::cachedMeridians_ = -1;
int OpenGL4SphereRenderer::cachedParallels_ = -1;
unsigned int OpenGL4SphereRenderer::indexCount_ = 0;

static const char* VERTEX_SHADER =
    "#version 410 core\n"
    "layout(location=0) in vec3 aPos;\n"
    "layout(location=1) in vec3 aNormal;\n"
    "layout(location=2) in vec2 aUv;\n"
    "uniform mat4 uMVP;\n"
    "uniform mat4 uModel;\n"
    "out vec3 vPos;\n"
    "out vec3 vNormal;\n"
    "out vec2 vUv;\n"
    "void main(){\n"
    "  vec4 wp = uModel * vec4(aPos, 1.0);\n"
    "  vPos = wp.xyz;\n"
    "  vNormal = normalize(mat3(uModel) * aNormal);\n"
    "  vUv = aUv;\n"
    "  gl_Position = uMVP * vec4(aPos, 1.0);\n"
    "}\n";

static const char* FRAGMENT_SHADER =
    "#version 410 core\n"
    "in vec3 vPos;\n"
    "in vec3 vNormal;\n"
    "in vec2 vUv;\n"
    "uniform vec3 uLightPos;\n"
    "uniform vec3 uLightColor;\n"
    "uniform vec3 uViewPos;\n"
    "uniform vec3 uAmbient;\n"
    "uniform vec3 uDiffuse;\n"
    "uniform vec3 uSpecular;\n"
    "uniform float uShininess;\n"
    "uniform bool uUseTexture;\n"
    "uniform bool uUseBump;\n"
    "uniform sampler2D uTexture;\n"
    "uniform sampler2D uBump;\n"
    "uniform int uShadingType;\n"
    "out vec4 FragColor;\n"
    "void main(){\n"
    "  vec3 n = normalize(vNormal);\n"
    "  if (uUseBump) {\n"
    "    vec3 bn = texture(uBump, vUv).rgb * 2.0 - 1.0;\n"
    "    n = normalize(mix(n, bn, 0.4));\n"
    "  }\n"
    "  vec3 baseColor = uDiffuse;\n"
    "  if (uUseTexture) baseColor *= texture(uTexture, vUv).rgb;\n"
    "  vec3 ambient = uAmbient * baseColor;\n"
    "  if (uShadingType == 0) { FragColor = vec4(ambient, 1.0); return; }\n"
    "  vec3 l = normalize(uLightPos - vPos);\n"
    "  float diff = max(dot(n, l), 0.0);\n"
    "  vec3 diffuse = diff * baseColor * uLightColor;\n"
    "  vec3 v = normalize(uViewPos - vPos);\n"
    "  vec3 r = reflect(-l, n);\n"
    "  float specF = pow(max(dot(v, r), 0.0), uShininess);\n"
    "  vec3 spec = specF * uSpecular * uLightColor;\n"
    "  FragColor = vec4(ambient + diffuse + spec, 1.0);\n"
    "}\n";

unsigned int OpenGL4SphereRenderer::compileShader(unsigned int type, const char* source)
{
    unsigned int shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    int ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[2048];
        glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
        fprintf(stderr, "OpenGL4SphereRenderer shader compile error: %s\n", log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

bool OpenGL4SphereRenderer::initProgramIfNeeded()
{
    if (program_ != 0) return true;
    unsigned int vs = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER);
    unsigned int fs = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
    if (vs == 0 || fs == 0) return false;

    program_ = glCreateProgram();
    glAttachShader(program_, vs);
    glAttachShader(program_, fs);
    glLinkProgram(program_);
    glDeleteShader(vs);
    glDeleteShader(fs);

    int ok = 0;
    glGetProgramiv(program_, GL_LINK_STATUS, &ok);
    if (!ok) {
        char log[2048];
        glGetProgramInfoLog(program_, sizeof(log), nullptr, log);
        fprintf(stderr, "OpenGL4SphereRenderer link error: %s\n", log);
        glDeleteProgram(program_);
        program_ = 0;
        return false;
    }

    glGenVertexArrays(1, &vao_);
    glGenBuffers(1, &vboPositions_);
    glGenBuffers(1, &vboNormals_);
    glGenBuffers(1, &vboUvs_);
    glGenBuffers(1, &ebo_);
    return true;
}

bool OpenGL4SphereRenderer::buildSphereMeshIfNeeded(int meridians, int parallels)
{
    meridians = (meridians < 3) ? 3 : meridians;
    parallels = (parallels < 2) ? 2 : parallels;
    if (cachedMeridians_ == meridians && cachedParallels_ == parallels && indexCount_ > 0) {
        return true;
    }

    std::vector<float> positions;
    std::vector<float> normals;
    std::vector<float> uvs;
    std::vector<unsigned int> indices;

    for (int p = 0; p <= parallels; ++p) {
        double t = static_cast<double>(p) / static_cast<double>(parallels);
        double phi = M_PI * t - M_PI / 2.0;
        for (int m = 0; m <= meridians; ++m) {
            double s = static_cast<double>(m) / static_cast<double>(meridians);
            double theta = 2.0 * M_PI * s;
            double cosPhi = std::cos(phi);
            double sinPhi = std::sin(phi);
            double cosTheta = std::cos(theta);
            double sinTheta = std::sin(theta);
            Vector3Dd pos(cosPhi * cosTheta, cosPhi * sinTheta, sinPhi);
            Vector3Dd nrm = pos.normalized();
            positions.push_back(static_cast<float>(pos.x()));
            positions.push_back(static_cast<float>(pos.y()));
            positions.push_back(static_cast<float>(pos.z()));
            normals.push_back(static_cast<float>(nrm.x()));
            normals.push_back(static_cast<float>(nrm.y()));
            normals.push_back(static_cast<float>(nrm.z()));
            uvs.push_back(static_cast<float>(s));
            uvs.push_back(static_cast<float>(t));
        }
    }

    int row = meridians + 1;
    for (int p = 0; p < parallels; ++p) {
        for (int m = 0; m < meridians; ++m) {
            unsigned int i0 = static_cast<unsigned int>(p * row + m);
            unsigned int i1 = i0 + 1;
            unsigned int i2 = i0 + row;
            unsigned int i3 = i2 + 1;
            indices.push_back(i0); indices.push_back(i2); indices.push_back(i1);
            indices.push_back(i1); indices.push_back(i2); indices.push_back(i3);
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

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo_);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.size() * sizeof(unsigned int), indices.data(), GL_STATIC_DRAW);

    glBindVertexArray(0);

    cachedMeridians_ = meridians;
    cachedParallels_ = parallels;
    indexCount_ = static_cast<unsigned int>(indices.size());
    return true;
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

    Matrix4x4d model = Matrix4x4d::identityMatrix().multiply(modelRotation).multiply(
        Matrix4x4d().scale(sphere->getRadius(), sphere->getRadius(), sphere->getRadius()));
    Matrix4x4d mvp = camera->calculateProjectionMatrix().multiply(model);

    float* mvpFloat = mvp.exportToFloatArrayColumnOrder();
    float* modelFloat = model.exportToFloatArrayColumnOrder();

    glUseProgram(program_);
    glUniformMatrix4fv(glGetUniformLocation(program_, "uMVP"), 1, GL_FALSE, mvpFloat);
    glUniformMatrix4fv(glGetUniformLocation(program_, "uModel"), 1, GL_FALSE, modelFloat);

    Vector3Dd lp = light->getPosition();
    Vector3Dd cp = camera->getPosition();
    ColorRgb lc = light->getSpecular();
    ColorRgb amb = material->getAmbient();
    ColorRgb dif = material->getDiffuse();
    ColorRgb spe = material->getSpecular();

    glUniform3f(glGetUniformLocation(program_, "uLightPos"), (float)lp.x(), (float)lp.y(), (float)lp.z());
    glUniform3f(glGetUniformLocation(program_, "uLightColor"), (float)lc.r(), (float)lc.g(), (float)lc.b());
    glUniform3f(glGetUniformLocation(program_, "uViewPos"), (float)cp.x(), (float)cp.y(), (float)cp.z());
    glUniform3f(glGetUniformLocation(program_, "uAmbient"), (float)amb.r(), (float)amb.g(), (float)amb.b());
    glUniform3f(glGetUniformLocation(program_, "uDiffuse"), (float)dif.r(), (float)dif.g(), (float)dif.b());
    glUniform3f(glGetUniformLocation(program_, "uSpecular"), (float)spe.r(), (float)spe.g(), (float)spe.b());
    glUniform1f(glGetUniformLocation(program_, "uShininess"), (float)material->getPhongExponent());
    glUniform1i(glGetUniformLocation(program_, "uShadingType"), quality->getShadingType());

    int texId = -1;
    int bumpId = -1;
    bool useTexture = quality->isTextureSet() && textureMap != nullptr;
    bool useBump = quality->isBumpMapSet() && bumpMapHeightRgb != nullptr;

    if (useTexture) {
        texId = OpenGL4ImageRenderer::activate(textureMap);
        if (texId > 0) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, (unsigned int)texId);
            glUniform1i(glGetUniformLocation(program_, "uTexture"), 0);
        }
    }
    if (useBump) {
        bumpId = OpenGL4ImageRenderer::activate(bumpMapHeightRgb);
        if (bumpId > 0) {
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, (unsigned int)bumpId);
            glUniform1i(glGetUniformLocation(program_, "uBump"), 1);
        }
    }

    glBindVertexArray(vao_);

    const GLint locUseTexture = glGetUniformLocation(program_, "uUseTexture");
    const GLint locUseBump = glGetUniformLocation(program_, "uUseBump");
    const GLint locAmbient = glGetUniformLocation(program_, "uAmbient");
    const GLint locDiffuse = glGetUniformLocation(program_, "uDiffuse");
    const GLint locShadingType = glGetUniformLocation(program_, "uShadingType");

    if (quality->isSurfacesSet()) {
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glUniform1i(locShadingType, quality->getShadingType());
        glUniform1i(locUseTexture, useTexture ? 1 : 0);
        glUniform1i(locUseBump, useBump ? 1 : 0);
        glDrawElements(GL_TRIANGLES, (GLsizei)indexCount_, GL_UNSIGNED_INT, nullptr);
    }

    if (quality->isWiresSet()) {
        ColorRgb wire(1.0, 1.0, 1.0);
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-1.0f, -1.0f);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glUniform1i(locShadingType, RendererConfiguration::SHADING_TYPE_NOLIGHT);
        glUniform3f(locAmbient, (float)wire.r(), (float)wire.g(), (float)wire.b());
        glUniform3f(locDiffuse, (float)wire.r(), (float)wire.g(), (float)wire.b());
        glUniform1i(locUseTexture, 0);
        glUniform1i(locUseBump, 0);
        glDrawElements(GL_TRIANGLES, (GLsizei)indexCount_, GL_UNSIGNED_INT, nullptr);
        glDisable(GL_POLYGON_OFFSET_LINE);
        glUniform3f(locAmbient, (float)amb.r(), (float)amb.g(), (float)amb.b());
        glUniform3f(locDiffuse, (float)dif.r(), (float)dif.g(), (float)dif.b());
        glUniform1i(locShadingType, quality->getShadingType());
    }

    if (quality->isPointsSet()) {
        glEnable(GL_PROGRAM_POINT_SIZE);
        glPointSize(3.0f);
        glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
        glUniform1i(locShadingType, RendererConfiguration::SHADING_TYPE_NOLIGHT);
        glUniform3f(locAmbient, 1.0f, 0.0f, 0.0f);
        glUniform3f(locDiffuse, 1.0f, 0.0f, 0.0f);
        glUniform1i(locUseTexture, 0);
        glUniform1i(locUseBump, 0);
        glDrawElements(GL_TRIANGLES, (GLsizei)indexCount_, GL_UNSIGNED_INT, nullptr);
        glUniform3f(locAmbient, (float)amb.r(), (float)amb.g(), (float)amb.b());
        glUniform3f(locDiffuse, (float)dif.r(), (float)dif.g(), (float)dif.b());
        glUniform1i(locShadingType, quality->getShadingType());
    }

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glBindVertexArray(0);

    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);

    delete[] mvpFloat;
    delete[] modelFloat;
}

void OpenGL4SphereRenderer::dispose()
{
    if (vao_ != 0) {
        glDeleteVertexArrays(1, &vao_);
        vao_ = 0;
    }
    if (vboPositions_ != 0) {
        glDeleteBuffers(1, &vboPositions_);
        vboPositions_ = 0;
    }
    if (vboNormals_ != 0) {
        glDeleteBuffers(1, &vboNormals_);
        vboNormals_ = 0;
    }
    if (vboUvs_ != 0) {
        glDeleteBuffers(1, &vboUvs_);
        vboUvs_ = 0;
    }
    if (ebo_ != 0) {
        glDeleteBuffers(1, &ebo_);
        ebo_ = 0;
    }
    if (program_ != 0) {
        glDeleteProgram(program_);
        program_ = 0;
    }
    cachedMeridians_ = -1;
    cachedParallels_ = -1;
    indexCount_ = 0;
}

}}}}

#include "vsdk/toolkit/io/geometry/ReaderObj.h"

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/elements/Triangle.h"
#include "vsdk/toolkit/environment/geometry/elements/Vertex.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"

#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <fstream>
#include <map>
#include <sstream>
#include <string>
#include <vector>

struct ReaderObjVertex {
    int vertexPositionIndex;
    int vertexNormalIndex;
    int vertexTextureCoordinateIndex;

    ReaderObjVertex() : vertexPositionIndex(-1), vertexNormalIndex(-1), vertexTextureCoordinateIndex(-1) {}
    ReaderObjVertex(const ReaderObjVertex& o) = default;

    bool operator<(const ReaderObjVertex& o) const
    {
        if (vertexPositionIndex != o.vertexPositionIndex) return vertexPositionIndex < o.vertexPositionIndex;
        if (vertexNormalIndex != o.vertexNormalIndex) return vertexNormalIndex < o.vertexNormalIndex;
        return vertexTextureCoordinateIndex < o.vertexTextureCoordinateIndex;
    }
};

static std::string dirnameOf(const std::string& path)
{
    size_t p = path.find_last_of("/\\");
    if (p == std::string::npos) return std::string(".");
    return path.substr(0, p);
}

static std::string joinPath(const std::string& a, const std::string& b)
{
    if (a.empty() || a == ".") return b;
    return a + "/" + b;
}

static int readIndexInteger(const std::string& token)
{
    return std::atoi(token.c_str());
}

static int resolveObjIndex(int rawIndex, int count)
{
    if (rawIndex > 0) {
        return rawIndex - 1;
    }
    if (rawIndex < 0) {
        return count + rawIndex;
    }
    return -1;
}

static ReaderObjVertex readFaceVertex(const std::string& text)
{
    ReaderObjVertex r;
    std::vector<std::string> parts;
    std::stringstream ss(text);
    std::string item;
    while (std::getline(ss, item, '/')) parts.push_back(item);

    if (parts.size() >= 1 && !parts[0].empty()) r.vertexPositionIndex = readIndexInteger(parts[0]);
    if (parts.size() >= 2 && !parts[1].empty()) r.vertexTextureCoordinateIndex = readIndexInteger(parts[1]);
    if (parts.size() >= 3 && !parts[2].empty()) r.vertexNormalIndex = readIndexInteger(parts[2]);
    if (parts.size() == 2 && text.size() > 0 && text[text.size() - 1] != '/') {
        r.vertexNormalIndex = r.vertexTextureCoordinateIndex;
        r.vertexTextureCoordinateIndex = -1;
    }
    return r;
}

static std::vector< std::vector<ReaderObjVertex> > readPolygonAsTriangleFan(const std::string& line)
{
    std::vector< std::vector<ReaderObjVertex> > ret;
    std::istringstream ss(line);
    std::string tag;
    ss >> tag;

    std::vector<ReaderObjVertex> poly;
    std::string tok;
    while (ss >> tok) poly.push_back(readFaceVertex(tok));
    if (poly.size() < 3) return ret;

    for (size_t i = 2; i < poly.size(); i++) {
        std::vector<ReaderObjVertex> tri(3);
        tri[0] = poly[0];
        tri[1] = poly[i - 1];
        tri[2] = poly[i];
        ret.push_back(tri);
    }
    return ret;
}

static Vector3Dd readVertex(const std::string& line)
{
    std::istringstream ss(line);
    std::string tag;
    double x = 0, y = 0, z = 0;
    ss >> tag >> x >> y >> z;
    return Vector3Dd(x, y, z);
}

static Vector3Dd readVertexTexture(const std::string& line)
{
    std::istringstream ss(line);
    std::string tag;
    double x = 0, y = 0, z = 0;
    ss >> tag >> x >> y;
    if (!(ss >> z)) z = 0;
    return Vector3Dd(x, y, z);
}

static SimpleMaterial defaultMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(ColorRgb(0.5, 0.9, 0.5));
    m = m.withSpecular(ColorRgb(1, 1, 1));
    m = m.withDoubleSided(false);
    return m;
}

static std::map<std::string, SimpleMaterial> readMaterials(const std::string& mtllibLine, const std::string& objFile)
{
    std::map<std::string, SimpleMaterial> ret;
    std::istringstream ss(mtllibLine);
    std::string tag, mtlFile;
    ss >> tag >> mtlFile;
    if (mtlFile.empty()) return ret;

    std::ifstream in(joinPath(dirnameOf(objFile), mtlFile).c_str());
    if (!in.is_open()) return ret;

    SimpleMaterial active;
    active = active.withDoubleSided(false).withName("default");

    std::string line;
    while (std::getline(in, line)) {
        std::istringstream ls(line);
        std::string cmd;
        ls >> cmd;
        if (cmd == "Ns") {
            double v; if (ls >> v) active = active.withPhongExponent(v);
        }
        else if (cmd == "d") {
            double v; if (ls >> v) active = active.withOpacity(v);
        }
        else if (cmd == "Tr") {
            double tr; if (ls >> tr) active = active.withOpacity(1.0 - tr);
        }
        else if (cmd == "Kd") {
            double r,g,b; if (ls >> r >> g >> b) active = active.withDiffuse(ColorRgb(r,g,b));
        }
        else if (cmd == "Ka") {
            double r,g,b; if (ls >> r >> g >> b) active = active.withAmbient(ColorRgb(r,g,b));
        }
        else if (cmd == "Ks") {
            double r,g,b; if (ls >> r >> g >> b) active = active.withSpecular(ColorRgb(r,g,b));
        }
        else if (cmd == "Ke") {
            double r,g,b; if (ls >> r >> g >> b) active = active.withEmission(ColorRgb(r,g,b));
        }
        else if (cmd == "Kt" || cmd == "Tf") {
            double r,g,b; if (ls >> r >> g >> b) active = active.withTransmittance(ColorRgb(r,g,b));
        }
        else if (cmd == "Ni") {
            double ior; if (ls >> ior) active = active.withIndexOfRefraction(ior);
        }
        else if (cmd == "illum") {
            // Ignored by Vitral material model.
        }
        else if (cmd == "newmtl") {
            std::string name; ls >> name;
            ret[active.getName()] = active;
            active = SimpleMaterial().withDoubleSided(false).withName(name);
        }
    }
    ret[active.getName()] = active;
    return ret;
}

static void addMeshToGroup(
    std::vector<TriangleMesh>& meshGroup,
    const std::string& nextGeometricObjectName,
    const std::vector<Vector3Dd>& vertexPositionsArray,
    const std::vector<Vector3Dd>& vertexNormalsArray,
    const std::vector<Vector3Dd>& vertexTextureCoordinatesArray,
    std::vector< std::vector<ReaderObjVertex> >& triangleDatasetsArray,
    const std::vector<Image*>& nextTexturesArray,
    std::vector< std::vector< std::vector<int> > >& textureSpanTriangleRangeTable,
    std::vector<SimpleMaterial>& nextMaterialsArray,
    std::vector< std::vector<int> >& materialTriangleRangeTable)
{
    TriangleMesh mesh;

    if (nextMaterialsArray.empty()) {
        SimpleMaterial m;
        m = m.withName("default obj material").withDoubleSided(false);
        nextMaterialsArray.push_back(m);
    }

    std::map<ReaderObjVertex, int> usedCombinedVertexes;
    std::vector<ReaderObjVertex> finalVertexes;

    for (size_t i = 0; i < triangleDatasetsArray.size(); i++) {
        for (int k = 0; k < 3; k++) {
            ReaderObjVertex p = triangleDatasetsArray[i][k];
            if (usedCombinedVertexes.find(p) == usedCombinedVertexes.end()) {
                usedCombinedVertexes[p] = (int)finalVertexes.size();
                finalVertexes.push_back(p);
            }
            triangleDatasetsArray[i][k].vertexPositionIndex = usedCombinedVertexes[p];
        }
    }

    std::vector<Vertex> newVertexArray;
    newVertexArray.reserve(finalVertexes.size());
    const double pi = std::acos(-1.0);
    Matrix4x4d R;
    R = R.axisRotation(90.0 * pi / 180.0, Vector3Dd(1, 0, 0));

    for (size_t i = 0; i < finalVertexes.size(); i++) {
        int srcPos = resolveObjIndex(
            finalVertexes[i].vertexPositionIndex, (int)vertexPositionsArray.size());
        if (srcPos < 0 || srcPos >= (int)vertexPositionsArray.size()) srcPos = 0;
        Vertex vtx(R.multiply(vertexPositionsArray[(size_t)srcPos]));

        int ti = resolveObjIndex(finalVertexes[i].vertexTextureCoordinateIndex,
            (int)vertexTextureCoordinatesArray.size());
        if (ti >= 0 && ti < (int)vertexTextureCoordinatesArray.size()) {
            vtx.u = vertexTextureCoordinatesArray[(size_t)ti].x();
            vtx.v = vertexTextureCoordinatesArray[(size_t)ti].y();
        }

        int ni =
            resolveObjIndex(finalVertexes[i].vertexNormalIndex, (int)vertexNormalsArray.size());
        if (ni >= 0 && ni < (int)vertexNormalsArray.size()) {
            vtx.normal = R.multiply(vertexNormalsArray[(size_t)ni]);
        }
        else {
            vtx.normal = Vector3Dd(0, 0, 0);
        }
        newVertexArray.push_back(vtx);
    }

    std::vector<Triangle> newTriangleArray(triangleDatasetsArray.size());
    for (size_t i = 0; i < triangleDatasetsArray.size(); i++) {
        newTriangleArray[i].p0 = triangleDatasetsArray[i][0].vertexPositionIndex;
        newTriangleArray[i].p1 = triangleDatasetsArray[i][1].vertexPositionIndex;
        newTriangleArray[i].p2 = triangleDatasetsArray[i][2].vertexPositionIndex;
    }

    mesh.setVertexes(newVertexArray, true, false, false, true);
    mesh.setTriangles(newTriangleArray);

    std::vector<SimpleMaterial*> materials;
    for (size_t i = 0; i < nextMaterialsArray.size(); i++) {
        materials.push_back(new SimpleMaterial(nextMaterialsArray[i]));
    }
    mesh.setMaterials(materials);

    std::vector<int> auxMaterialRange(2, 0);
    auxMaterialRange[0] = (int)triangleDatasetsArray.size();
    auxMaterialRange[1] = (int)nextMaterialsArray.size() - 1;
    materialTriangleRangeTable.push_back(auxMaterialRange);

    std::vector< std::vector<int> > materialRanges(materialTriangleRangeTable.size(), std::vector<int>(2, 0));
    for (size_t i = 1; i < materialTriangleRangeTable.size(); i++) {
        materialRanges[i][0] = materialTriangleRangeTable[i][0];
        materialRanges[i][1] = materialTriangleRangeTable[i - 1][1];
    }
    mesh.setMaterialRanges(materialRanges);

    mesh.setTextures(nextTexturesArray);

    int numTextureSpans = 0;
    for (size_t textureIndex = 0; textureIndex < textureSpanTriangleRangeTable.size(); textureIndex++) {
        numTextureSpans += (int)textureSpanTriangleRangeTable[textureIndex].size();
    }

    std::vector< std::vector<int> > textureRanges((size_t)std::max(numTextureSpans, 0), std::vector<int>(2, 0));
    int ri = 0;
    for (size_t textureIndex = 0; textureIndex < textureSpanTriangleRangeTable.size(); textureIndex++) {
        for (size_t j = 0; j < textureSpanTriangleRangeTable[textureIndex].size(); j++) {
            textureRanges[(size_t)ri][0] = textureSpanTriangleRangeTable[textureIndex][j][1];
            textureRanges[(size_t)ri][1] = (int)textureIndex;
            ri++;
        }
    }
    std::sort(textureRanges.begin(), textureRanges.end(), [](const std::vector<int>& a, const std::vector<int>& b){ return a[0] < b[0]; });
    mesh.setTextureRanges(textureRanges);

    mesh.setName(nextGeometricObjectName);
    meshGroup.push_back(mesh);
}

static TriangleMeshGroup* readObj(const java::File& sceneFile)
{
    const std::string fileName = sceneFile.getPath().toCString();
    std::ifstream in(fileName.c_str());
    if (!in.is_open()) return new TriangleMeshGroup();

    std::vector<Vector3Dd> vertexPositionsArray;
    std::vector<Vector3Dd> vertexNormalsArray;
    std::vector<Vector3Dd> vertexTextureCoordinatesArray;
    std::vector< std::vector<ReaderObjVertex> > triangleDatasetsArray;

    std::string nextGeometricObjectName = "OBJ_default_material";
    std::vector<Image*> nextTexturesArray;
    std::vector<SimpleMaterial> nextMaterialsArray;

    std::vector<TriangleMesh> meshGroup;
    std::vector< std::vector< std::vector<int> > > textureSpanTriangleRangeTable;
    textureSpanTriangleRangeTable.push_back(std::vector< std::vector<int> >(1, std::vector<int>(2, 0)));
    std::vector< std::vector<int> > materialTriangleRangeTable;

    std::map<std::string, Image*> texturesHashMap;
    std::map<std::string, SimpleMaterial> materialsHashMap;
    int textureIndex = 0;
    auto ensureMaterialSelection = [&]() {
        if (!nextMaterialsArray.empty() || materialsHashMap.empty()) {
            return;
        }
        // Fallback for OBJ files that declare mtllib but omit explicit usemtl:
        // use the first material from the MTL table for the whole mesh.
        auto it = materialsHashMap.begin();
        nextMaterialsArray.push_back(it->second);
        std::vector<int> r(2, 0);
        r[0] = 0;
        r[1] = 0;
        materialTriangleRangeTable.push_back(r);
    };

    std::string line;
    while (std::getline(in, line)) {
        if (line.rfind("mtllib ", 0) == 0) {
            materialsHashMap = readMaterials(line, fileName);
        }
        if (line.rfind("usemtl ", 0) == 0) {
            std::istringstream ss(line);
            std::string t, matName;
            ss >> t >> matName;
            if (materialsHashMap.find(matName) != materialsHashMap.end()) nextMaterialsArray.push_back(materialsHashMap[matName]);
            else nextMaterialsArray.push_back(SimpleMaterial());
            std::vector<int> r(2, 0);
            r[0] = (int)triangleDatasetsArray.size();
            r[1] = (int)nextMaterialsArray.size() - 1;
            materialTriangleRangeTable.push_back(r);
        }
        if (line.rfind("v ", 0) == 0) vertexPositionsArray.push_back(readVertex(line));
        if (line.rfind("vn ", 0) == 0) vertexNormalsArray.push_back(readVertex(line));
        if (line.rfind("vt ", 0) == 0) vertexTextureCoordinatesArray.push_back(readVertexTexture(line));
        if (line.rfind("f ", 0) == 0) {
            std::vector< std::vector<ReaderObjVertex> > fan = readPolygonAsTriangleFan(line);
            for (size_t i = 0; i < fan.size(); i++) triangleDatasetsArray.push_back(fan[i]);
        }
        if (line.rfind("usemap ", 0) == 0) {
            std::istringstream ss(line);
            std::string t, texName;
            ss >> t >> texName;

            if (texturesHashMap.find(texName) == texturesHashMap.end()) {
                Image* tex = 0;
                if (texName != "(null)") {
                    std::string texPath = joinPath(dirnameOf(fileName), texName);
                    tex = ImagePersistence::importRGBA(java::File(texPath.c_str()));
                }
                texturesHashMap[texName] = tex;
                if (tex != 0) {
                    nextTexturesArray.push_back(tex);
                    textureSpanTriangleRangeTable.push_back(std::vector< std::vector<int> >());
                    textureIndex = (int)nextTexturesArray.size();
                }
                else {
                    textureIndex = 0;
                }
            }
            else {
                Image* tex = texturesHashMap[texName];
                if (tex == 0) {
                    textureIndex = 0;
                }
                else {
                    textureIndex = 0;
                    for (size_t i = 0; i < nextTexturesArray.size(); i++) {
                        if (nextTexturesArray[i] == tex) { textureIndex = (int)i + 1; break; }
                    }
                    if (textureIndex == 0) {
                        nextTexturesArray.push_back(tex);
                        textureSpanTriangleRangeTable.push_back(std::vector< std::vector<int> >());
                        textureIndex = (int)nextTexturesArray.size();
                    }
                }
            }

            if ((size_t)textureIndex >= textureSpanTriangleRangeTable.size()) {
                textureSpanTriangleRangeTable.resize((size_t)textureIndex + 1);
            }
            std::vector<int> newRange(2, 0);
            newRange[0] = (int)triangleDatasetsArray.size();
            textureSpanTriangleRangeTable[(size_t)textureIndex].push_back(newRange);
        }
        if (line.rfind("o ", 0) == 0 || line.rfind("g ", 0) == 0) {
            if (!vertexPositionsArray.empty()) {
                ensureMaterialSelection();
                addMeshToGroup(meshGroup, nextGeometricObjectName,
                               vertexPositionsArray, vertexNormalsArray, vertexTextureCoordinatesArray,
                               triangleDatasetsArray, nextTexturesArray, textureSpanTriangleRangeTable,
                               nextMaterialsArray, materialTriangleRangeTable);
            }

            std::istringstream ss(line);
            std::string t;
            ss >> t >> nextGeometricObjectName;

            if (!vertexPositionsArray.empty()) {
                nextTexturesArray.clear();
                nextMaterialsArray.clear();
                triangleDatasetsArray.clear();
                materialTriangleRangeTable.clear();
                textureSpanTriangleRangeTable.clear();
                textureSpanTriangleRangeTable.push_back(std::vector< std::vector<int> >(1, std::vector<int>(2, 0)));
                textureIndex = 0;
            }
        }
    }

    if (!vertexPositionsArray.empty()) {
        ensureMaterialSelection();
        addMeshToGroup(meshGroup, nextGeometricObjectName,
                       vertexPositionsArray, vertexNormalsArray, vertexTextureCoordinatesArray,
                       triangleDatasetsArray, nextTexturesArray, textureSpanTriangleRangeTable,
                       nextMaterialsArray, materialTriangleRangeTable);
    }

    TriangleMeshGroup* finalGroup = new TriangleMeshGroup();
    for (size_t i = 0; i < meshGroup.size(); i++) {
        if (meshGroup[i].getNumVertices() > 0) finalGroup->addMesh(meshGroup[i]);
    }
    return finalGroup;
}

static void addThing(TriangleMeshGroup* g, std::vector<SimpleBody*>& bodies)
{
    if (g == 0) return;
    SimpleBody* thing = new SimpleBody();
    thing->setGeometry(g);
    thing->setPosition(Vector3Dd());
    thing->setRotation(Matrix4x4d());
    thing->setRotationInverse(Matrix4x4d());
    thing->setMaterial(new SimpleMaterial(defaultMaterial()));
    bodies.push_back(thing);
}

void ReaderObj::importEnvironment(const java::File& sceneFile, SimpleScene* scene)
{
    if (scene == 0) return;
    TriangleMeshGroup* mg = readObj(sceneFile);
    addThing(mg, scene->getSimpleBodies());
}

#include "vsdk/toolkit/io/geometry/ReaderObj.h"
#include "java/lang/String.h"
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
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"

#include <algorithm>
#include "java/lang/String.h"
#include <cmath>
#include "java/lang/String.h"
#include <cstdlib>
#include "java/lang/String.h"
#include <fstream>
#include "java/lang/String.h"
#include <map>
#include "java/lang/String.h"
#include <sstream>
#include "java/lang/String.h"
#include "java/lang/String.h"
#include "java/lang/String.h"

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

    bool operator==(const ReaderObjVertex& o) const
    {
        return vertexPositionIndex == o.vertexPositionIndex &&
               vertexNormalIndex == o.vertexNormalIndex &&
               vertexTextureCoordinateIndex == o.vertexTextureCoordinateIndex;
    }
};

static java::String dirnameOf(const java::String& path)
{
    size_t p = path.find_last_of("/\\");
    if (p == java::String::npos) return java::String(".");
    return path.substr(0, p);
}

static java::String joinPath(const java::String& a, const java::String& b)
{
    if (a.empty() || a == ".") return b;
    return a + "/" + b;
}

static int readIndexInteger(const java::String& token)
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

static ReaderObjVertex readFaceVertex(const java::String& text)
{
    ReaderObjVertex r;
    java::ArrayList<java::String> parts;
    std::basic_stringstream<char> ss(text.toCString());
    std::string item;
    while (std::getline(ss, item, '/')) parts.add(java::String(item.c_str()));

    if (parts.size() >= 1 && !parts[0].empty()) r.vertexPositionIndex = readIndexInteger(parts[0]);
    if (parts.size() >= 2 && !parts[1].empty()) r.vertexTextureCoordinateIndex = readIndexInteger(parts[1]);
    if (parts.size() >= 3 && !parts[2].empty()) r.vertexNormalIndex = readIndexInteger(parts[2]);
    if (parts.size() == 2 && text.size() > 0 && text[text.size() - 1] != '/') {
        r.vertexNormalIndex = r.vertexTextureCoordinateIndex;
        r.vertexTextureCoordinateIndex = -1;
    }
    return r;
}

static java::ArrayList< java::ArrayList<ReaderObjVertex> > readPolygonAsTriangleFan(const java::String& line)
{
    java::ArrayList< java::ArrayList<ReaderObjVertex> > ret;
    std::istringstream ss(line.toCString());
    std::string tagStr;
    ss >> tagStr;
    java::String tag(tagStr.c_str());

    java::ArrayList<ReaderObjVertex> poly;
    std::string tokStr;
    while (ss >> tokStr) poly.add(readFaceVertex(java::String(tokStr.c_str())));
    if (poly.size() < 3) return ret;

    for (size_t i = 2; i < poly.size(); i++) {
        java::ArrayList<ReaderObjVertex> tri;
        tri.add(poly[0]);
        tri.add(poly[(long int)i - 1]);
        tri.add(poly[(long int)i]);
        ret.add(tri);
    }
    return ret;
}

static Vector3Dd readVertex(const java::String& line)
{
    std::istringstream ss(line.toCString());
    std::string tagStr;
    double x = 0, y = 0, z = 0;
    ss >> tagStr >> x >> y >> z;
    return Vector3Dd(x, y, z);
}

static Vector3Dd readVertexTexture(const java::String& line)
{
    std::istringstream ss(line.toCString());
    std::string tagStr;
    double x = 0, y = 0, z = 0;
    ss >> tagStr >> x >> y;
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

static void readMaterials(const java::String& mtllibLine, const java::String& objFile, java::HashMap<java::String, SimpleMaterial>& ret)
{
    ret.clear();
    std::istringstream ss(mtllibLine.toCString());
    std::string tagStr, mtlFileStr;
    ss >> tagStr >> mtlFileStr;
    java::String mtlFile(mtlFileStr.c_str());
    if (mtlFile.empty()) return;

    std::ifstream in(joinPath(dirnameOf(objFile), mtlFile).c_str());
    if (!in.is_open()) return;

    SimpleMaterial active;
    active = active.withDoubleSided(false).withName("default");

    std::string line;
    while (std::getline(in, line)) {
        std::istringstream ls(line);
        std::string cmdStr;
        ls >> cmdStr;
        java::String cmd(cmdStr.c_str());
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
            std::string nameStr; ls >> nameStr;
            java::String name(nameStr.c_str());
            ret.put(active.getName(), active);
            active = SimpleMaterial().withDoubleSided(false).withName(name);
        }
    }
    ret.put(active.getName(), active);
}

static void addMeshToGroup(
    java::ArrayList<TriangleMesh>& meshGroup,
    const java::String& nextGeometricObjectName,
    const java::ArrayList<Vector3Dd>& vertexPositionsArray,
    const java::ArrayList<Vector3Dd>& vertexNormalsArray,
    const java::ArrayList<Vector3Dd>& vertexTextureCoordinatesArray,
    java::ArrayList< java::ArrayList<ReaderObjVertex> >& triangleDatasetsArray,
    const java::ArrayList<Image*>& nextTexturesArray,
    java::ArrayList< java::ArrayList< java::ArrayList<int> > >& textureSpanTriangleRangeTable,
    java::ArrayList<SimpleMaterial>& nextMaterialsArray,
    java::ArrayList< java::ArrayList<int> >& materialTriangleRangeTable)
{
    TriangleMesh mesh;

    if (nextMaterialsArray.size() == 0) {
        SimpleMaterial m;
        m = m.withName("default obj material").withDoubleSided(false);
        nextMaterialsArray.add(m);
    }

    java::HashMap<ReaderObjVertex, int> usedCombinedVertexes;
    java::ArrayList<ReaderObjVertex> finalVertexes;

    for (size_t i = 0; i < triangleDatasetsArray.size(); i++) {
        for (int k = 0; k < 3; k++) {
            ReaderObjVertex p = triangleDatasetsArray[i][k];
            if (!usedCombinedVertexes.containsKey(p)) {
                usedCombinedVertexes.put(p, (int)finalVertexes.size());
                finalVertexes.add(p);
            }
            int vertexIdx = usedCombinedVertexes.getOrDefault(p, -1);
            triangleDatasetsArray[i][k].vertexPositionIndex = vertexIdx;
        }
    }

    java::ArrayList<Vertex> newVertexArray;
    newVertexArray.reserve((long int)finalVertexes.size());
    const double pi = std::acos(-1.0);
    Matrix4x4d R;
    R = R.axisRotation(90.0 * pi / 180.0, Vector3Dd(1, 0, 0));

    for (size_t i = 0; i < finalVertexes.size(); i++) {
        int srcPos = resolveObjIndex(
            finalVertexes[i].vertexPositionIndex, (int)vertexPositionsArray.size());
        if (srcPos < 0 || srcPos >= (int)vertexPositionsArray.size()) srcPos = 0;
        Vertex vtx(R.multiply(vertexPositionsArray.get((long int)srcPos)));

        int ti = resolveObjIndex(finalVertexes[i].vertexTextureCoordinateIndex,
            (int)vertexTextureCoordinatesArray.size());
        if (ti >= 0 && ti < (int)vertexTextureCoordinatesArray.size()) {
            vtx.u = vertexTextureCoordinatesArray.get((long int)ti).x();
            vtx.v = vertexTextureCoordinatesArray.get((long int)ti).y();
        }

        int ni =
            resolveObjIndex(finalVertexes[i].vertexNormalIndex, (int)vertexNormalsArray.size());
        if (ni >= 0 && ni < (int)vertexNormalsArray.size()) {
            vtx.normal = R.multiply(vertexNormalsArray.get((long int)ni));
        }
        else {
            vtx.normal = Vector3Dd(0, 0, 0);
        }
        newVertexArray.add(vtx);
    }

    java::ArrayList<Triangle> newTriangleArray;
    newTriangleArray.reserve((long int)triangleDatasetsArray.size());
    for (size_t i = 0; i < triangleDatasetsArray.size(); i++) newTriangleArray.add(Triangle());
    for (size_t i = 0; i < triangleDatasetsArray.size(); i++) {
        newTriangleArray[(long int)i].p0 = triangleDatasetsArray[i][0].vertexPositionIndex;
        newTriangleArray[(long int)i].p1 = triangleDatasetsArray[i][1].vertexPositionIndex;
        newTriangleArray[(long int)i].p2 = triangleDatasetsArray[i][2].vertexPositionIndex;
    }

    mesh.setVertexes(newVertexArray, true, false, false, true);
    mesh.setTriangles(newTriangleArray);

    java::ArrayList<SimpleMaterial*> materials;
    for (size_t i = 0; i < nextMaterialsArray.size(); i++) {
        materials.add(new SimpleMaterial(nextMaterialsArray[i]));
    }
    mesh.setMaterials(materials);

    java::ArrayList<int> auxMaterialRange;
    auxMaterialRange.add((int)triangleDatasetsArray.size());
    auxMaterialRange.add((int)nextMaterialsArray.size() - 1);
    materialTriangleRangeTable.add(auxMaterialRange);

    java::ArrayList< java::ArrayList<int> > materialRanges;
    for (size_t i = 0; i < materialTriangleRangeTable.size(); i++) {
        java::ArrayList<int> row;
        row.add(0); row.add(0);
        materialRanges.add(row);
    }
    for (size_t i = 1; i < materialTriangleRangeTable.size(); i++) {
        materialRanges[(long int)i][0] = materialTriangleRangeTable[i][0];
        materialRanges[(long int)i][1] = materialTriangleRangeTable[i - 1][1];
    }
    mesh.setMaterialRanges(materialRanges);

    mesh.setTextures(nextTexturesArray);

    int numTextureSpans = 0;
    for (size_t textureIndex = 0; textureIndex < textureSpanTriangleRangeTable.size(); textureIndex++) {
        numTextureSpans += (int)textureSpanTriangleRangeTable[textureIndex].size();
    }

    java::ArrayList< java::ArrayList<int> > textureRangesTmp;
    for (size_t textureIndex = 0; textureIndex < textureSpanTriangleRangeTable.size(); textureIndex++) {
        for (size_t j = 0; j < textureSpanTriangleRangeTable[textureIndex].size(); j++) {
            java::ArrayList<int> row;
            row.add(textureSpanTriangleRangeTable[textureIndex][j][1]);
            row.add((int)textureIndex);
            textureRangesTmp.add(row);
        }
    }
    for (long int i = 1; i < textureRangesTmp.size(); i++) {
        java::ArrayList<int> key = textureRangesTmp[i];
        long int j = i - 1;
        while (j >= 0 && textureRangesTmp[j][0] > key[0]) {
            textureRangesTmp[j + 1] = textureRangesTmp[j];
            j--;
        }
        textureRangesTmp[j + 1] = key;
    }
    java::ArrayList< java::ArrayList<int> > textureRanges;
    for (size_t i = 0; i < textureRangesTmp.size(); i++) {
        java::ArrayList<int> row;
        row.add(textureRangesTmp[i][0]);
        row.add(textureRangesTmp[i][1]);
        textureRanges.add(row);
    }
    mesh.setTextureRanges(textureRanges);

    mesh.setName(nextGeometricObjectName);
    meshGroup.add(mesh);
}

static TriangleMeshGroup* readObj(const java::File& sceneFile)
{
    const java::String fileName = sceneFile.getPath().toCString();
    std::ifstream in(fileName.c_str());
    if (!in.is_open()) return new TriangleMeshGroup();

    java::ArrayList<Vector3Dd> vertexPositionsArray;
    java::ArrayList<Vector3Dd> vertexNormalsArray;
    java::ArrayList<Vector3Dd> vertexTextureCoordinatesArray;
    java::ArrayList< java::ArrayList<ReaderObjVertex> > triangleDatasetsArray;

    java::String nextGeometricObjectName = "OBJ_default_material";
    java::ArrayList<Image*> nextTexturesArray;
    java::ArrayList<SimpleMaterial> nextMaterialsArray;

    java::ArrayList<TriangleMesh> meshGroup;
    java::ArrayList< java::ArrayList< java::ArrayList<int> > > textureSpanTriangleRangeTable;
    java::ArrayList< java::ArrayList<int> > initialTextureSpans;
    java::ArrayList<int> initialTextureRange;
    initialTextureRange.add(0);
    initialTextureRange.add(0);
    initialTextureSpans.add(initialTextureRange);
    textureSpanTriangleRangeTable.add(initialTextureSpans);
    java::ArrayList< java::ArrayList<int> > materialTriangleRangeTable;

    java::HashMap<java::String, Image*> texturesHashMap;
    java::HashMap<java::String, SimpleMaterial> materialsHashMap;
    int textureIndex = 0;
    auto ensureMaterialSelection = [&]() {
        if (nextMaterialsArray.size() > 0 || materialsHashMap.isEmpty()) {
            return;
        }
        // Fallback for OBJ files that declare mtllib but omit explicit usemtl:
        // use the default material for the whole mesh
        nextMaterialsArray.add(SimpleMaterial());
        java::ArrayList<int> r;
        r.add(0);
        r.add(0);
        materialTriangleRangeTable.add(r);
    };

    std::string line;
    while (std::getline(in, line)) {
        java::String javaLine(line.c_str());
        if (javaLine.rfind("mtllib ", 0) == 0) {
            readMaterials(javaLine, fileName, materialsHashMap);
        }
        if (javaLine.rfind("usemtl ", 0) == 0) {
            std::istringstream ss(line);
            std::string tStr, matNameStr;
            ss >> tStr >> matNameStr;
            java::String matName(matNameStr.c_str());
            SimpleMaterial matValue;
            if (materialsHashMap.tryGet(matName, &matValue)) nextMaterialsArray.add(matValue);
            else nextMaterialsArray.add(SimpleMaterial());
            java::ArrayList<int> r;
            r.add((int)triangleDatasetsArray.size());
            r.add((int)nextMaterialsArray.size() - 1);
            materialTriangleRangeTable.add(r);
        }
        if (javaLine.rfind("v ", 0) == 0) vertexPositionsArray.add(readVertex(javaLine));
        if (javaLine.rfind("vn ", 0) == 0) vertexNormalsArray.add(readVertex(javaLine));
        if (javaLine.rfind("vt ", 0) == 0) vertexTextureCoordinatesArray.add(readVertexTexture(javaLine));
        if (javaLine.rfind("f ", 0) == 0) {
            java::ArrayList< java::ArrayList<ReaderObjVertex> > fan = readPolygonAsTriangleFan(javaLine);
            for (size_t i = 0; i < fan.size(); i++) triangleDatasetsArray.add(fan[i]);
        }
        if (javaLine.rfind("usemap ", 0) == 0) {
            std::istringstream ss(line);
            std::string tStr, texNameStr;
            ss >> tStr >> texNameStr;
            java::String texName(texNameStr.c_str());

            Image* texValue = nullptr;
            if (!texturesHashMap.tryGet(texName, &texValue)) {
                Image* tex = 0;
                if (texName != "(null)") {
                    java::String texPath = joinPath(dirnameOf(fileName), texName);
                    tex = ImagePersistence::importRGBA(java::File(texPath.c_str()));
                }
                texturesHashMap.put(texName, tex);
                if (tex != 0) {
                    nextTexturesArray.add(tex);
                    textureSpanTriangleRangeTable.add(java::ArrayList< java::ArrayList<int> >());
                    textureIndex = (int)nextTexturesArray.size();
                }
                else {
                    textureIndex = 0;
                }
            }
            else {
                Image* tex = texValue;
                if (tex == 0) {
                    textureIndex = 0;
                }
                else {
                    textureIndex = 0;
                    for (long int i = 0; i < nextTexturesArray.size(); i++) {
                        if (nextTexturesArray[i] == tex) { textureIndex = (int)i + 1; break; }
                    }
                    if (textureIndex == 0) {
                        nextTexturesArray.add(tex);
                        textureSpanTriangleRangeTable.add(java::ArrayList< java::ArrayList<int> >());
                        textureIndex = (int)nextTexturesArray.size();
                    }
                }
            }

            while ((size_t)textureIndex >= textureSpanTriangleRangeTable.size()) {
                textureSpanTriangleRangeTable.add(java::ArrayList< java::ArrayList<int> >());
            }
            java::ArrayList<int> newRange;
            newRange.add((int)triangleDatasetsArray.size());
            newRange.add(0);
            textureSpanTriangleRangeTable[(size_t)textureIndex].add(newRange);
        }
        if (javaLine.rfind("o ", 0) == 0 || javaLine.rfind("g ", 0) == 0) {
            if (vertexPositionsArray.size() > 0) {
                ensureMaterialSelection();
                addMeshToGroup(meshGroup, nextGeometricObjectName,
                               vertexPositionsArray, vertexNormalsArray, vertexTextureCoordinatesArray,
                               triangleDatasetsArray, nextTexturesArray, textureSpanTriangleRangeTable,
                               nextMaterialsArray, materialTriangleRangeTable);
            }

            std::istringstream ss(line);
            std::string tStr, nameStr;
            ss >> tStr >> nameStr;
            nextGeometricObjectName = java::String(nameStr.c_str());

            if (vertexPositionsArray.size() > 0) {
                nextTexturesArray.clear();
                nextMaterialsArray.clear();
                triangleDatasetsArray.clear();
                materialTriangleRangeTable.clear();
                textureSpanTriangleRangeTable.clear();
                java::ArrayList< java::ArrayList<int> > defaultTextureSpans;
                java::ArrayList<int> defaultTextureRange;
                defaultTextureRange.add(0);
                defaultTextureRange.add(0);
                defaultTextureSpans.add(defaultTextureRange);
                textureSpanTriangleRangeTable.add(defaultTextureSpans);
                textureIndex = 0;
            }
        }
    }

    if (vertexPositionsArray.size() > 0) {
        ensureMaterialSelection();
        addMeshToGroup(meshGroup, nextGeometricObjectName,
                       vertexPositionsArray, vertexNormalsArray, vertexTextureCoordinatesArray,
                       triangleDatasetsArray, nextTexturesArray, textureSpanTriangleRangeTable,
                       nextMaterialsArray, materialTriangleRangeTable);
    }

    TriangleMeshGroup* finalGroup = new TriangleMeshGroup();
    for (long int i = 0; i < meshGroup.size(); i++) {
        if (meshGroup[i].getNumVertices() > 0) finalGroup->addMesh(meshGroup[i]);
    }
    return finalGroup;
}

static void addThing(TriangleMeshGroup* g, java::ArrayList<SimpleBody*>& bodies)
{
    if (g == 0) return;
    SimpleBody* thing = new SimpleBody();
    thing->setGeometry(g);
    thing->setPosition(Vector3Dd());
    thing->setRotation(Matrix4x4d());
    thing->setRotationInverse(Matrix4x4d());
    thing->setMaterial(new SimpleMaterial(defaultMaterial()));
    bodies.add(thing);
}

void ReaderObj::importEnvironment(const java::File& sceneFile, SimpleScene* scene)
{
    if (scene == 0) return;
    TriangleMeshGroup* mg = readObj(sceneFile);
    addThing(mg, scene->getSimpleBodies());
}

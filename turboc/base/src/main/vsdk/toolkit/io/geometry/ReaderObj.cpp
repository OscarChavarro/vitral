#include <ctype.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/element/Triangle.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/io/image/ImagePersistence.h"
#include "vsdk/toolkit/io/geometry/ReaderObj.h"
struct ReaderObjVertex {
    int vertexPositionIndex;
    int vertexNormalIndex;
    int vertexTextureCoordinateIndex;

    ReaderObjVertex() : vertexPositionIndex(-1), vertexNormalIndex(-1), vertexTextureCoordinateIndex(-1) {}

    BOOLEAN operator<(const ReaderObjVertex& o) const
    {
        if (vertexPositionIndex != o.vertexPositionIndex) return vertexPositionIndex < o.vertexPositionIndex;
        if (vertexNormalIndex != o.vertexNormalIndex) return vertexNormalIndex < o.vertexNormalIndex;
        return vertexTextureCoordinateIndex < o.vertexTextureCoordinateIndex;
    }

    BOOLEAN operator==(const ReaderObjVertex& o) const
    {
        return vertexPositionIndex == o.vertexPositionIndex &&
               vertexNormalIndex == o.vertexNormalIndex &&
               vertexTextureCoordinateIndex == o.vertexTextureCoordinateIndex;
    }
};

static String dirnameOf(const String& path)
{
    size_t p = path.find_last_of("/\\");
    if (p == (size_t)String::npos) return String(".");
    return path.substr(0, p);
}

static String joinPath(const String& a, const String& b)
{
    if (a.empty() || a == ".") return b;
    return a + "/" + b;
}

static int readIndexInteger(const String& token)
{
    return atoi(token.c_str());
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

static ReaderObjVertex readFaceVertex(const String& text)
{
    ReaderObjVertex r;
    ArrayList<String> parts;
    // Split by '/', preserving empty tokens
    int prev = 0;
    for (int i = 0; i <= (int)text.size(); i++) {
        if (i == (int)text.size() || text[i] == '/') {
            parts.add(text.substr(prev, i - prev));
            prev = i + 1;
        }
    }

    if (parts.size() >= 1 && !parts[0].empty()) r.vertexPositionIndex = readIndexInteger(parts[0]);
    if (parts.size() >= 2 && !parts[1].empty()) r.vertexTextureCoordinateIndex = readIndexInteger(parts[1]);
    if (parts.size() >= 3 && !parts[2].empty()) r.vertexNormalIndex = readIndexInteger(parts[2]);
    if (parts.size() == 2 && text.size() > 0 && text[text.size() - 1] != '/') {
        r.vertexNormalIndex = r.vertexTextureCoordinateIndex;
        r.vertexTextureCoordinateIndex = -1;
    }
    return r;
}

static ArrayList< ArrayList<ReaderObjVertex> > readPolygonAsTriangleFan(const String& line)
{
    ArrayList< ArrayList<ReaderObjVertex> > ret;

    // Skip the 'f' tag and get the rest of the line
    const char* lineStr = line.toCString();
    int offset = 0;
    while (offset < (int)line.size() && !isspace((unsigned char)lineStr[offset])) offset++;
    while (offset < (int)line.size() && isspace((unsigned char)lineStr[offset])) offset++;

    ArrayList<ReaderObjVertex> poly;
    // Parse remaining tokens (space-separated vertex specs)
    while (offset < (int)line.size()) {
        // Skip leading spaces
        while (offset < (int)line.size() && isspace((unsigned char)lineStr[offset])) offset++;
        if (offset >= (int)line.size()) break;

        // Find end of token
        int tokenStart = offset;
        while (offset < (int)line.size() && !isspace((unsigned char)lineStr[offset])) offset++;

        // Add vertex
        String token = line.substr(tokenStart, offset - tokenStart);
        poly.add(readFaceVertex(token));
    }

    if (poly.size() < 3) return ret;

    for (size_t i = 2; i < (size_t)poly.size(); i++) {
        ArrayList<ReaderObjVertex> tri;
        tri.add(poly[0]);
        tri.add(poly[(long int)i - 1]);
        tri.add(poly[(long int)i]);
        ret.add(tri);
    }
    return ret;
}

static Vector3Dd readVertex(const String& line)
{
    double x = 0, y = 0, z = 0;
    sscanf(line.toCString(), "%*s %lf %lf %lf", &x, &y, &z);
    return Vector3Dd(x, y, z);
}

static Vector3Dd readVertexTexture(const String& line)
{
    double x = 0, y = 0, z = 0;
    int readCount = sscanf(line.toCString(), "%*s %lf %lf %lf", &x, &y, &z);
    if (readCount < 2) {
        sscanf(line.toCString(), "%*s %lf %lf", &x, &y);
        z = 0;
    }
    return Vector3Dd(x, y, z);
}

static SimpleMaterial defaultMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(ColorRgb(0.5, 0.9, 0.5));
    m = m.withSpecular(ColorRgb(1, 1, 1));
    m = m.withDoubleSided(FALSE);
    return m;
}

static void readMaterials(
    const String& mtllibLine,
    const String& objFile,
    ArrayList<String>& materialNames,
    ArrayList<SimpleMaterial>& materialValues)
{
    materialNames.clear();
    materialValues.clear();
    char tagStr[256] = {0}, mtlFileStr[256] = {0};
    sscanf(mtllibLine.toCString(), "%255s %255s", tagStr, mtlFileStr);
    String mtlFile(mtlFileStr);
    if (mtlFile.empty()) return;

    String mtlPath = joinPath(dirnameOf(objFile), mtlFile);
    FILE* in = fopen(mtlPath.c_str(), "r");
    if (!in) return;

    SimpleMaterial active;
    active = active.withDoubleSided(FALSE).withName("default");
    BOOLEAN hasRealMaterial = FALSE;

    char line[1024];
    while (fgets(line, sizeof(line), in)) {
        char cmdStr[256] = {0};
        sscanf(line, "%255s", cmdStr);
        String cmd(cmdStr);
        if (cmd == "Ns") {
            double v;
            if (sscanf(line, "%*s %lf", &v) == 1) active = active.withPhongExponent(v);
        }
        else if (cmd == "d") {
            double v;
            if (sscanf(line, "%*s %lf", &v) == 1) active = active.withOpacity(v);
        }
        else if (cmd == "Tr") {
            double tr;
            if (sscanf(line, "%*s %lf", &tr) == 1) active = active.withOpacity(1.0 - tr);
        }
        else if (cmd == "Kd") {
            double r, g, b;
            if (sscanf(line, "%*s %lf %lf %lf", &r, &g, &b) == 3) active = active.withDiffuse(ColorRgb(r, g, b));
        }
        else if (cmd == "Ka") {
            double r, g, b;
            if (sscanf(line, "%*s %lf %lf %lf", &r, &g, &b) == 3) active = active.withAmbient(ColorRgb(r, g, b));
        }
        else if (cmd == "Ks") {
            double r, g, b;
            if (sscanf(line, "%*s %lf %lf %lf", &r, &g, &b) == 3) active = active.withSpecular(ColorRgb(r, g, b));
        }
        else if (cmd == "Ke") {
            double r, g, b;
            if (sscanf(line, "%*s %lf %lf %lf", &r, &g, &b) == 3) active = active.withEmission(ColorRgb(r, g, b));
        }
        else if (cmd == "Kt" || cmd == "Tf") {
            double r, g, b;
            if (sscanf(line, "%*s %lf %lf %lf", &r, &g, &b) == 3) active = active.withTransmittance(ColorRgb(r, g, b));
        }
        else if (cmd == "Ni") {
            double ior;
            if (sscanf(line, "%*s %lf", &ior) == 1) active = active.withIndexOfRefraction(ior);
        }
        else if (cmd == "illum") {
            // Ignored by Vitral material model.
        }
        else if (cmd == "newmtl") {
            char nameStr[256] = {0};
            sscanf(line, "%*s %255s", nameStr);
            String name(nameStr);
            if (hasRealMaterial) {
                materialNames.add(active.getName());
                materialValues.add(active);
            }
            active = SimpleMaterial().withDoubleSided(FALSE).withName(name);
            hasRealMaterial = TRUE;
        }
    }
    fclose(in);
    if (hasRealMaterial) {
        materialNames.add(active.getName());
        materialValues.add(active);
    }
}

static void addMeshToGroup(
    ArrayList<TriangleMesh>& meshGroup,
    const String& nextGeometricObjectName,
    const ArrayList<Vector3Dd>& vertexPositionsArray,
    const ArrayList<Vector3Dd>& vertexNormalsArray,
    const ArrayList<Vector3Dd>& vertexTextureCoordinatesArray,
    ArrayList< ArrayList<ReaderObjVertex> >& triangleDatasetsArray,
    const ArrayList<Image*>& nextTexturesArray,
    ArrayList< ArrayList< ArrayList<int> > >& textureSpanTriangleRangeTable,
    ArrayList<SimpleMaterial*>& nextMaterialsArray,
    ArrayList< ArrayList<int> >& materialTriangleRangeTable)
{
    TriangleMesh mesh;

    if (nextMaterialsArray.size() == 0) {
        SimpleMaterial defaultMat;
        defaultMat = defaultMat.withName("default obj material").withDoubleSided(FALSE);
        nextMaterialsArray.add(new SimpleMaterial(defaultMat));
    }

    ArrayList<ReaderObjVertex> finalVertexes;

    long i;
    for (i = 0; i < triangleDatasetsArray.size(); i++) {
        for (int k = 0; k < 3; k++) {
            ReaderObjVertex p = triangleDatasetsArray[i][k];
            int vertexIdx = -1;
            for (long int vi = 0; vi < finalVertexes.size(); vi++) {
                if (finalVertexes[vi] == p) {
                    vertexIdx = (int)vi;
                    break;
                }
            }
            if (vertexIdx < 0) {
                vertexIdx = (int)finalVertexes.size();
                finalVertexes.add(p);
            }
            triangleDatasetsArray[i][k].vertexPositionIndex = vertexIdx;
        }
    }

    ArrayList<Vertex> newVertexArray;
    newVertexArray.reserve((long int)finalVertexes.size());
    const double pi = acos(-1.0);
    Matrix4x4d R;
    R = R.axisRotation(90.0 * pi / 180.0, Vector3Dd(1, 0, 0));

    for (i = 0; i < finalVertexes.size(); i++) {
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

    ArrayList<Triangle> newTriangleArray;
    newTriangleArray.reserve((long int)triangleDatasetsArray.size());
    for (i = 0; i < triangleDatasetsArray.size(); i++) newTriangleArray.add(Triangle());
    for (i = 0; i < triangleDatasetsArray.size(); i++) {
        newTriangleArray[(long int)i].setPoint0(triangleDatasetsArray[i][0].vertexPositionIndex);
        newTriangleArray[(long int)i].setPoint1(triangleDatasetsArray[i][1].vertexPositionIndex);
        newTriangleArray[(long int)i].setPoint2(triangleDatasetsArray[i][2].vertexPositionIndex);
    }

    mesh.setVertexes(newVertexArray, TRUE, FALSE, FALSE, TRUE);
    mesh.setTriangles(newTriangleArray);

    mesh.setMaterials(nextMaterialsArray);

    ArrayList<int> auxMaterialRange;
    auxMaterialRange.add((int)triangleDatasetsArray.size());
    auxMaterialRange.add((int)nextMaterialsArray.size() - 1);
    materialTriangleRangeTable.add(auxMaterialRange);

    ArrayList< ArrayList<int> > materialRanges;
    for (i = 0; i < materialTriangleRangeTable.size(); i++) {
        ArrayList<int> row;
        row.add(0); row.add(0);
        materialRanges.add(row);
    }
    for (i = 1; i < materialTriangleRangeTable.size(); i++) {
        materialRanges[(long int)i][0] = materialTriangleRangeTable[i][0];
        materialRanges[(long int)i][1] = materialTriangleRangeTable[i - 1][1];
    }
    mesh.setMaterialRanges(materialRanges);

    mesh.setTextures(nextTexturesArray);

    int numTextureSpans = 0;
    size_t textureIndex;
    for (textureIndex = 0; textureIndex < (size_t)textureSpanTriangleRangeTable.size(); textureIndex++) {
        numTextureSpans += (int)textureSpanTriangleRangeTable[textureIndex].size();
    }

    ArrayList< ArrayList<int> > textureRangesTmp;
    for (textureIndex = 0; textureIndex < (size_t)textureSpanTriangleRangeTable.size(); textureIndex++) {
        for (size_t j = 0; j < (size_t)textureSpanTriangleRangeTable[textureIndex].size(); j++) {
            ArrayList<int> row;
            row.add(textureSpanTriangleRangeTable[textureIndex][j][1]);
            row.add((int)textureIndex);
            textureRangesTmp.add(row);
        }
    }
    for (i = 1; i < textureRangesTmp.size(); i++) {
        ArrayList<int> key = textureRangesTmp[i];
        long int j = i - 1;
        while (j >= 0 && textureRangesTmp[j][0] > key[0]) {
            textureRangesTmp[j + 1] = textureRangesTmp[j];
            j--;
        }
        textureRangesTmp[j + 1] = key;
    }
    ArrayList< ArrayList<int> > textureRanges;
    for (i = 0; i < textureRangesTmp.size(); i++) {
        ArrayList<int> row;
        row.add(textureRangesTmp[i][0]);
        row.add(textureRangesTmp[i][1]);
        textureRanges.add(row);
    }
    mesh.setTextureRanges(textureRanges);

    mesh.setName(nextGeometricObjectName);
    meshGroup.add(mesh);
}

/**
Fallback for OBJ files that declare mtllib but omit explicit usemtl: use the
first material from the MTL table for the whole mesh.
*/
static void ensureMaterialSelection(
    ArrayList<SimpleMaterial*>& nextMaterialsArray,
    const ArrayList<SimpleMaterial>& materialValues,
    ArrayList< ArrayList<int> >& materialTriangleRangeTable)
{
    if (nextMaterialsArray.size() > 0 || materialValues.size() == 0) {
        return;
    }
    nextMaterialsArray.add(new SimpleMaterial(materialValues[0]));
    ArrayList<int> r;
    r.add(0);
    r.add(0);
    materialTriangleRangeTable.add(r);
}

static TriangleMeshGroup* readObj(const File& sceneFile)
{
    const String fileName = sceneFile.getPath().toCString();
    FILE* in = fopen(fileName.c_str(), "r");
    if (!in) return new TriangleMeshGroup();

    ArrayList<Vector3Dd> vertexPositionsArray;
    ArrayList<Vector3Dd> vertexNormalsArray;
    ArrayList<Vector3Dd> vertexTextureCoordinatesArray;
    ArrayList< ArrayList<ReaderObjVertex> > triangleDatasetsArray;

    String nextGeometricObjectName = "OBJ_default_material";
    ArrayList<Image*> nextTexturesArray;
    ArrayList<SimpleMaterial*> nextMaterialsArray;

    ArrayList<TriangleMesh> meshGroup;
    ArrayList< ArrayList< ArrayList<int> > > textureSpanTriangleRangeTable;
    ArrayList< ArrayList<int> > initialTextureSpans;
    ArrayList<int> initialTextureRange;
    initialTextureRange.add(0);
    initialTextureRange.add(0);
    initialTextureSpans.add(initialTextureRange);
    textureSpanTriangleRangeTable.add(initialTextureSpans);
    ArrayList< ArrayList<int> > materialTriangleRangeTable;

    HashMap<String, Image*> texturesHashMap;
    ArrayList<String> materialNames;
    ArrayList<SimpleMaterial> materialValues;
    int textureIndex = 0;

    char lineBuf[1024];
    while (fgets(lineBuf, sizeof(lineBuf), in)) {
        String javaLine(lineBuf);
        if (javaLine.rfind("mtllib ", 0) == 0) {
            readMaterials(javaLine, fileName, materialNames, materialValues);
        }
        if (javaLine.rfind("usemtl ", 0) == 0) {
            char tStr[256] = {0}, matNameStr[256] = {0};
            sscanf(lineBuf, "%255s %255s", tStr, matNameStr);
            String matName(matNameStr);
            BOOLEAN found = FALSE;
            for (long int mi = 0; mi < materialNames.size(); mi++) {
                if (materialNames[mi] == matName) {
                    nextMaterialsArray.add(new SimpleMaterial(materialValues[mi]));
                    found = TRUE;
                    break;
                }
            }
            if (!found) nextMaterialsArray.add(new SimpleMaterial());
            ArrayList<int> r;
            r.add((int)triangleDatasetsArray.size());
            r.add((int)nextMaterialsArray.size() - 1);
            materialTriangleRangeTable.add(r);
        }
        if (javaLine.rfind("v ", 0) == 0) vertexPositionsArray.add(readVertex(javaLine));
        if (javaLine.rfind("vn ", 0) == 0) vertexNormalsArray.add(readVertex(javaLine));
        if (javaLine.rfind("vt ", 0) == 0) vertexTextureCoordinatesArray.add(readVertexTexture(javaLine));
        if (javaLine.rfind("f ", 0) == 0) {
            ArrayList< ArrayList<ReaderObjVertex> > fan = readPolygonAsTriangleFan(javaLine);
            for (size_t i = 0; i < (size_t)fan.size(); i++) triangleDatasetsArray.add(fan[i]);
        }
        if (javaLine.rfind("usemap ", 0) == 0) {
            char tStr[256] = {0}, texNameStr[256] = {0};
            sscanf(lineBuf, "%255s %255s", tStr, texNameStr);
            String texName(texNameStr);

            Image* texValue = NULL;
            if (!texturesHashMap.tryGet(texName, &texValue)) {
                Image* tex = 0;
                if (texName != "(null)") {
                    String texPath = joinPath(dirnameOf(fileName), texName);
                    tex = ImagePersistence::importRGBA(File(texPath.c_str()));
                }
                texturesHashMap.put(texName, tex);
                if (tex != 0) {
                    nextTexturesArray.add(tex);
                    textureSpanTriangleRangeTable.add(ArrayList< ArrayList<int> >());
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
                        textureSpanTriangleRangeTable.add(ArrayList< ArrayList<int> >());
                        textureIndex = (int)nextTexturesArray.size();
                    }
                }
            }

            while ((size_t)textureIndex >= (size_t)textureSpanTriangleRangeTable.size()) {
                textureSpanTriangleRangeTable.add(ArrayList< ArrayList<int> >());
            }
            ArrayList<int> newRange;
            newRange.add((int)triangleDatasetsArray.size());
            newRange.add(0);
            textureSpanTriangleRangeTable[(size_t)textureIndex].add(newRange);
        }
        if (javaLine.rfind("o ", 0) == 0 || javaLine.rfind("g ", 0) == 0) {
            if (vertexPositionsArray.size() > 0) {
                ensureMaterialSelection(nextMaterialsArray, materialValues,
                    materialTriangleRangeTable);
                addMeshToGroup(meshGroup, nextGeometricObjectName,
                               vertexPositionsArray, vertexNormalsArray, vertexTextureCoordinatesArray,
                               triangleDatasetsArray, nextTexturesArray, textureSpanTriangleRangeTable,
                               nextMaterialsArray, materialTriangleRangeTable);
            }

            char tStr[256] = {0}, nameStr[256] = {0};
            sscanf(lineBuf, "%255s %255s", tStr, nameStr);
            nextGeometricObjectName = String(nameStr);

            if (vertexPositionsArray.size() > 0) {
                nextTexturesArray.clear();
                nextMaterialsArray.clear();
                triangleDatasetsArray.clear();
                materialTriangleRangeTable.clear();
                textureSpanTriangleRangeTable.clear();
                ArrayList< ArrayList<int> > defaultTextureSpans;
                ArrayList<int> defaultTextureRange;
                defaultTextureRange.add(0);
                defaultTextureRange.add(0);
                defaultTextureSpans.add(defaultTextureRange);
                textureSpanTriangleRangeTable.add(defaultTextureSpans);
                textureIndex = 0;
            }
        }
    }

    if (vertexPositionsArray.size() > 0) {
        ensureMaterialSelection(nextMaterialsArray, materialValues,
            materialTriangleRangeTable);
        addMeshToGroup(meshGroup, nextGeometricObjectName,
                       vertexPositionsArray, vertexNormalsArray, vertexTextureCoordinatesArray,
                       triangleDatasetsArray, nextTexturesArray, textureSpanTriangleRangeTable,
                       nextMaterialsArray, materialTriangleRangeTable);
    }

    fclose(in);

    TriangleMeshGroup* finalGroup = new TriangleMeshGroup();
    for (long int i = 0; i < meshGroup.size(); i++) {
        if (meshGroup[i].getNumVertices() > 0) finalGroup->addMesh(meshGroup[i]);
    }

    nextMaterialsArray.clear();
    return finalGroup;
}

static void addThing(TriangleMeshGroup* g, ArrayList<SimpleBody*>& bodies)
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

void ReaderObj::importEnvironment(const File& sceneFile, SimpleScene* scene)
{
    if (scene == 0) return;
    TriangleMeshGroup* mg = readObj(sceneFile);
    addThing(mg, scene->getSimpleBodies());
}

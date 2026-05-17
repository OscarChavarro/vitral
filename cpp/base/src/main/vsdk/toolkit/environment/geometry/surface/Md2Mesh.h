#ifndef __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_MD2MESH_H__
#define __VSDK_TOOLKIT_ENVIRONMENT_GEOMETRY_SURFACE_MD2MESH_H__

#include <string>
#include <vector>

class Image;

class _AnimationInfo {
public:
    std::string name;
    short start;
    short end;

    _AnimationInfo();
    _AnimationInfo(const _AnimationInfo& ai);
};

class Md2Mesh {
public:
    int ident;
    int version;
    int skinWidth;
    int skinHeight;
    int frameSize;
    int numSkins;
    int numVertices;
    int numTexCoords;
    int numTriangles;
    int numGlCommands;
    int numFrames;
    int offsetSkins;
    int offsetTexCoords;
    int offsetTriangles;
    int offsetFrames;
    int offsetGlCommands;
    int offsetEnd;

    std::vector<std::string> skinNames;
    std::vector<Image*> skins;
    std::vector<std::string> frameNames;
    std::vector< std::vector<float> > frameVertices;
    std::vector< std::vector<short> > frameNormalIndices;
    std::vector< std::vector<float> > glCmdTexCoordsStrip;
    std::vector< std::vector<int> > glCmdVertIndexStrip;
    std::vector< std::vector<float> > glCmdTexCoordsFan;
    std::vector< std::vector<int> > glCmdVertIndexFan;
    std::vector<float> texCoords;
    std::vector< std::vector<int> > triangles;

private:
    std::vector<_AnimationInfo> aniInfos;
    float frameTimeSeg;
    float elapsedTimeSeg;
    short currentAnimationInd;
    short maxAnimationInd;

    _AnimationInfo* getAniInfo(const std::string& nameAnim);
    void fillAniInfo();

public:
    Md2Mesh();

    short getMaxAnimationInd() const;
    void setMaxAnimationInd(short maxAnimationInd);
    short getCurrentAnimationInd() const;
    void setCurrentAnimationInd(short currentAnimationInd);
    float getFrameTimeSeg() const;
    void setFrameTimeSeg(float frameTimeSeg);
    float getElapsedTimeSeg() const;
    void setElapsedTimeSeg(float elapsedTimeSeg);

    void returnStartEndAnim(short inIndex, short outStartEnd[2]);
    void returnStartEndAnim(const std::string& inNameAnim, short outStartEnd[2]);

    static const float anorms[162][3];
};

// Compressed vertex
class _md2Vertex {
public:
    unsigned char v[3];
    unsigned char normalIndex;
};

// Texture coords
class _md2TexCoord {
public:
    short s;
    short t;
};

// Triangle info
class _md2Triangle {
public:
    short vertex[3];
    short st[3];
};

// Model frame
class _md2Frame {
public:
    float scale[3];
    float translate[3];
    unsigned char name[16];
    std::vector<_md2Vertex> verts;
};

#endif

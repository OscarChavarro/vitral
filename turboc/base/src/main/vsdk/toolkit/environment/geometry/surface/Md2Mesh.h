#ifndef __MD_2_MESH__
#define __MD_2_MESH__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
class Image;

class _AnimationInfo {
public:
    String name;
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

    ArrayList<String> skinNames;
    ArrayList<Image*> skins;
    ArrayList<String> frameNames;
    ArrayList< ArrayList<float> > frameVertices;
    ArrayList< ArrayList<short> > frameNormalIndices;
    ArrayList< ArrayList<float> > glCmdTexCoordsStrip;
    ArrayList< ArrayList<int> > glCmdVertIndexStrip;
    ArrayList< ArrayList<float> > glCmdTexCoordsFan;
    ArrayList< ArrayList<int> > glCmdVertIndexFan;
    ArrayList<float> texCoords;
    ArrayList< ArrayList<int> > triangles;

private:
    ArrayList<_AnimationInfo> aniInfos;
    float frameTimeSeg;
    float elapsedTimeSeg;
    short currentAnimationInd;
    short maxAnimationInd;

    _AnimationInfo* getAniInfo(const String& nameAnim);
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
    void returnStartEndAnim(const String& inNameAnim, short outStartEnd[2]);

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
    ArrayList<_md2Vertex> verts;
};

#endif

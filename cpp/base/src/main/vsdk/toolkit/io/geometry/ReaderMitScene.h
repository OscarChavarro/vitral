#ifndef __VSDK_TOOLKIT_IO_GEOMETRY_READERMITSCENE_H__
#define __VSDK_TOOLKIT_IO_GEOMETRY_READERMITSCENE_H__

class SimpleScene;

class ReaderMitScene {
public:
    ReaderMitScene();
    virtual ~ReaderMitScene() {}

    void importEnvironment(const char* fileName, SimpleScene* outScene);
};

#endif

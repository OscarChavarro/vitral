#ifndef __VSDK_TOOLKIT_IO_GEOMETRY_READERMITSCENE_H__
#define __VSDK_TOOLKIT_IO_GEOMETRY_READERMITSCENE_H__

#include <istream>

class SimpleScene;

class ReaderMitScene {
public:
    ReaderMitScene();
    virtual ~ReaderMitScene() {}

    void importEnvironment(std::istream& is, SimpleScene* outScene);
};

#endif

#ifndef __READERMITSCENE__
#define __READERMITSCENE__

class SimpleScene;

class ReaderMitScene {
public:
    ReaderMitScene();
    virtual ~ReaderMitScene() {}

    void importEnvironment(const char* fileName, SimpleScene* outScene);
};

#endif

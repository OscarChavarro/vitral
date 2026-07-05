#ifndef __READEROBJ__
#define __READEROBJ__

#include "java/io/File.h"
#include "vsdk/toolkit/io/PersistenceElement.h"
class SimpleScene;

class ReaderObj : public PersistenceElement {
public:
    static void importEnvironment(const java::File& sceneFile, SimpleScene* scene);
};

#endif

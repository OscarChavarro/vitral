#ifndef __COMMAND_OPTIONS_PROCESSOR__
#define __COMMAND_OPTIONS_PROCESSOR__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/geometry/boundingVolumeHierarchy/TriangleMeshAccelerationMethod.h"

class CommandOptionsProcessor {
private:
    static const char* DEFAULT_SCENE_FILE;
    static const char* DEFAULT_OUTPUT_FILE_NAME;

    java::String sceneFile;
    java::String outputFile;
    bool save;
    bool parallel;
    bool showHelp;
    TriangleMeshAccelerationMethod meshAccelerationMethod;

public:
    CommandOptionsProcessor();

    static CommandOptionsProcessor process(const java::ArrayList<java::String>& args);
    static void printUsage();

    const java::String& getSceneFile() const;
    const java::String& getOutputFile() const;
    bool shouldSave() const;
    bool shouldUseParallelExecutor() const;
    bool shouldShowHelp() const;
    TriangleMeshAccelerationMethod getMeshAccelerationMethod() const;
};

#endif
#include "java/lang/String.h"

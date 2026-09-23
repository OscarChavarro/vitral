#ifndef __COMMAND_LINE_OPTIONS__
#define __COMMAND_LINE_OPTIONS__

#include "java/lang/String.h"

class CommandLineOptions {
public:
    enum Action {
        RUN,
        SHOW_HELP,
        LIST_CAMERAS
    };

    CommandLineOptions(int argc, char** argv);

    Action getAction() const { return action; }

    int getPort() const { return port; }
    int getCameraIndex() const { return cameraIndex; }
    double getMarkerSize() const { return markerSize; }
    const java::String& getCalibFile() const { return calibFile; }
    double getDecisionMarginThreshold() const { return decisionMarginThreshold; }
    double getViewAngleCosThreshold() const { return viewAngleCosThreshold; }
    int getStreamHz() const { return streamHz; }
    bool isDebugMode() const { return debugMode; }
    const java::String& getDebugDir() const { return debugDir; }
    bool isPreviewMode() const { return previewMode; }
    bool isMappingValid() const { return mappingValid; }
    const char* getProgramName() const { return programName; }

private:
    void parse(int argc, char** argv);
    static void showHelp(const char* progName);
    static void listCameras();

    Action action;
    int port;
    int cameraIndex;
    double markerSize;
    java::String calibFile;
    double decisionMarginThreshold;
    double viewAngleCosThreshold;
    int streamHz;
    bool debugMode;
    java::String debugDir;
    bool previewMode;
    bool mappingValid;
    const char* programName;
};

#endif

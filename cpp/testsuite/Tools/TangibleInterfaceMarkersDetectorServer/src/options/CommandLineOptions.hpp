#ifndef COMMAND_LINE_OPTIONS_HPP
#define COMMAND_LINE_OPTIONS_HPP

#include "java/lang/String.h"

class CommandLineOptions {
public:
    enum Action {
        RUN,
        SHOW_HELP,
        LIST_CAMERAS
    };

    CommandLineOptions(int argc, char** argv);

    Action getAction() const { return action_; }

    int getPort() const { return port_; }
    int getCameraIndex() const { return cameraIndex_; }
    double getMarkerSize() const { return markerSize_; }
    const java::String& getCalibFile() const { return calibFile_; }
    double getDecisionMarginThreshold() const { return decisionMarginThreshold_; }
    double getViewAngleCosThreshold() const { return viewAngleCosThreshold_; }
    int getStreamHz() const { return streamHz_; }
    bool isDebugMode() const { return debugMode_; }
    const java::String& getDebugDir() const { return debugDir_; }
    bool isPreviewMode() const { return previewMode_; }
    bool isMappingValid() const { return mappingValid_; }
    const char* getProgramName() const { return programName_; }

private:
    void parse(int argc, char** argv);
    static void showHelp(const char* progName);
    static void listCameras();

    Action action_;
    int port_;
    int cameraIndex_;
    double markerSize_;
    java::String calibFile_;
    double decisionMarginThreshold_;
    double viewAngleCosThreshold_;
    int streamHz_;
    bool debugMode_;
    java::String debugDir_;
    bool previewMode_;
    bool mappingValid_;
    const char* programName_;
};

#endif

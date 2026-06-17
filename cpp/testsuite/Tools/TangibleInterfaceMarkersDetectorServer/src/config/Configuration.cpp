#include <cstdio>

#include "java/util/ArrayList.txx"
#include "config/Configuration.hpp"
#include "io/MarkerGroupPersistence.hpp"
#include "model/MarkersModel.hpp"
#include <dirent.h>
#include "options/CommandLineOptions.hpp"
Configuration::Configuration(const CommandLineOptions& opts) : opts_(opts) {}

MarkerTrackerConfig Configuration::getMarkerTrackerConfig() const {
    MarkerTrackerConfig cfg;
    cfg.cameraIndex = opts_.getCameraIndex();
    cfg.markerSize = opts_.getMarkerSize();
    cfg.calibFile = opts_.getCalibFile();
    cfg.decisionMarginThreshold = opts_.getDecisionMarginThreshold();
    cfg.viewAngleCosThreshold = opts_.getViewAngleCosThreshold();
    cfg.debugMode = opts_.isDebugMode();
    cfg.debugDir = opts_.getDebugDir();
    return cfg;
}

WebServiceConfig Configuration::getWebServiceConfig() const {
    WebServiceConfig cfg;
    cfg.port = opts_.getPort();
    cfg.streamHz = opts_.getStreamHz();
    cfg.path = "/v1/values";
    return cfg;
}

bool Configuration::loadMarkerGroups(MarkersModel* model) const {
    if (model == nullptr) return false;
    DIR* dir = opendir("../../../../etc/markerGroups");
    if (!dir) {
        std::fprintf(stderr, "[config] marker groups dir not found: ../../../../etc/markerGroups\n");
        return false;
    }

    MarkerGroupPersistence persistence;
    bool anyLoaded = false;

    for (;;) {
        dirent* entry = readdir(dir);
        if (!entry) break;
        java::String fileName(entry->d_name);
        if (fileName == "." || fileName == "..") continue;
        if (fileName.length() < 6 || fileName.rfind(".json") != fileName.length() - 5) continue;

        java::String path = java::String("../../../../etc/markerGroups/") + fileName;
        MarkerGroup group;
        if (!persistence.readFromJsonFile(path, &group)) {
            std::fprintf(stderr, "[config] could not read marker group: %s\n", path.c_str());
            continue;
        }
        model->addMarkerGroup(group);
        anyLoaded = true;
    }

    closedir(dir);
    if (!anyLoaded) {
        std::fprintf(stderr, "[config] no marker groups loaded from ../../../../etc/markerGroups\n");
    }
    return anyLoaded;
}

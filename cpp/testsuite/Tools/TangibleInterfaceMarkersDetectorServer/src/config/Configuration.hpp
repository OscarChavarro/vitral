#ifndef CONFIGURATION_HPP
#define CONFIGURATION_HPP

#include "vision/MarkerTracker.hpp"
#include "webservice/WebServiceServer.hpp"

class CommandLineOptions;
class MarkersModel;

class Configuration {
public:
    explicit Configuration(const CommandLineOptions& opts);

    MarkerTrackerConfig getMarkerTrackerConfig() const;
    WebServiceConfig getWebServiceConfig() const;
    bool loadMarkerGroups(MarkersModel* model) const;

private:
    const CommandLineOptions& opts_;
};

#endif

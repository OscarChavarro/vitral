#include <cstdio>
#include <cstdlib>
#include <pthread.h>
#include <unistd.h>

#include "model/MarkersModel.hpp"
#include "model/MarkerEventBus.hpp"
#include "model/CleanerConsumer.hpp"
#include "vision/MarkerTracker.hpp"
#include "webservice/WebServiceServer.hpp"
#include "options/CommandLineOptions.hpp"
#include "config/Configuration.hpp"
#include "InteractiveMarkers.hpp"
#include "java/util/concurrent/Executors.h"
#include "java/util/ArrayList.txx"

void*
webServiceThreadEntry(void* arg) {
    WebServiceServer* webService = static_cast<WebServiceServer*>(arg);
    webService->start();
    return NULL;
}

pthread_t
startWebServiceThread(WebServiceServer& webService) {
    pthread_t thread;
    if (pthread_create(&thread, NULL, webServiceThreadEntry, &webService) != 0) {
        std::fprintf(stderr, "[main] failed to create web service thread\n");
        return 0;
    }
    return thread;
}

void
stopWebServiceThread(pthread_t thread, WebServiceServer& webService) {
    webService.requestStop();
    pthread_join(thread, NULL);
}

int
runPreviewMode(MarkersModel& model, MarkerTracker& markerTracker, WebServiceServer& webService) {
    std::printf("[main] preview mode enabled\n");

    pthread_t webServiceThread = startWebServiceThread(webService);
    if (webServiceThread == 0) {
        return 1;
    }

    InteractiveMarkers interactive(&model, &markerTracker);
    int result = interactive.run();

    model.setRunning(false);
    usleep(200000);
    stopWebServiceThread(webServiceThread, webService);
    return result;
}

int
runNormalMode(WebServiceServer& webService) {
    return webService.start() ? 0 : 1;
}

int
main(int argc, char** argv) {
    CommandLineOptions opts(argc, argv);

    if (opts.getAction() == CommandLineOptions::SHOW_HELP ||
        opts.getAction() == CommandLineOptions::LIST_CAMERAS) {
        return 0;
    }

    Configuration config(opts);

    MarkerTrackerConfig markerCfg = config.getMarkerTrackerConfig();
    WebServiceConfig webCfg = config.getWebServiceConfig();

    MarkersModel* model = new MarkersModel();
    model->setRunning(true);
    config.loadMarkerGroups(model);
    model->initializeTestsFromFirstMarkerGroup();

    MarkerEventBus bus;
    MarkerTracker markerTracker(markerCfg, model, &bus);
    WebServiceServer webService(webCfg, &bus);

    java::ExecutorService* executorService = java::Executors::newFixedThreadPool(1);
    CleanerConsumer cleaner(&bus);
    java::Future<java::Void> cleanerFuture = executorService->submit(&cleaner);

    model->setMarkerTracker(&markerTracker);

    int result = 0;
    if (opts.isPreviewMode()) {
        result = runPreviewMode(*model, markerTracker, webService);
    } else {
        if (!markerTracker.start()) {
            std::fprintf(stderr, "[main] failed to start marker tracker\n");
            result = 1;
        } else {
            result = runNormalMode(webService);
            markerTracker.stop();
        }
    }

    cleaner.stop();
    executorService->shutdownNow();
    delete executorService;
    delete model;
    return result;
}

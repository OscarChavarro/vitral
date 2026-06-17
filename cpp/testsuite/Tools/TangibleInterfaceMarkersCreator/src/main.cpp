#include "TangibleInterfaceMarkersCreator.h"
int
main(int argc, char** argv) {
    TangibleInterfaceMarkersCreator application(argc, argv);

    if (!application.init()) {
        return 1;
    }

    application.process();
    application.exportPdf();

    return 0;
}

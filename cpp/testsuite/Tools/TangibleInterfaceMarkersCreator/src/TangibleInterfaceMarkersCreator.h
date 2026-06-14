#ifndef __TANGIBLEINTERFACEMARKERSCREATOR_H__
#define __TANGIBLEINTERFACEMARKERSCREATOR_H__

#include "model/MarkersModel.h"
#include "options/CommandLineOptions.h"
#include "render/CairoPdfPageRenderer.h"

class TangibleInterfaceMarkersCreator {
  public:
    TangibleInterfaceMarkersCreator(int argc, char** argv);
    ~TangibleInterfaceMarkersCreator();

    bool init();
    void process();
    void exportPdf();

  private:
    CommandLineOptions options_;
    MarkersModel model_;
    CairoPdfPageRenderer* pageRenderer_;
};

#endif

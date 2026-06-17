#ifndef __TANGIBLEINTERFACELABELSCREATOR_H__
#define __TANGIBLEINTERFACELABELSCREATOR_H__

#include "model/LabelsModel.h"
#include "render/CairoPdfPageRenderer.h"
#include "options/CommandLineOptions.h"
class TangibleInterfaceLabelsCreator {
  public:
    TangibleInterfaceLabelsCreator(int argc, char** argv);
    ~TangibleInterfaceLabelsCreator();

    bool init();
    void process();
    void exportPdf();

  private:
    CommandLineOptions options_;
    LabelsModel model_;
    CairoPdfPageRenderer* pageRenderer_;
};

#endif

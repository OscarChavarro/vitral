#include <vector>
#include <unistd.h>

#include "java/io/File.h"
#include "application/commands/GuiEventExecutor.h"
#include "application/XtOpenGL1GuiEventExecutor.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtUiFactory.h"
#include "io/FileSuffixFilter.h"
#include "model/GuiState.h"
#include "render/opengl1/XtOpenGL1SceneBridge.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"


static std::string parentFolderOf(const std::string& path)
{
    size_t separator = path.rfind('/');
    if ( separator == std::string::npos ) {
        return ".";
    }
    return separator == 0 ? "/" : path.substr(0, separator);
}

static std::string currentFolder()
{
    char buffer[4096];
    return getcwd(buffer, sizeof(buffer)) != NULL ? buffer : ".";
}


XtOpenGL1GuiEventExecutor::XtOpenGL1GuiEventExecutor(XtApplicationHost* parent)
    : parent(parent)
{
}

bool XtOpenGL1GuiEventExecutor::executeCommand(const String& command)
{
    std::string label(command.c_str());
    CommandResult result =
        parent->getSceneBridge()->executeCommand(label);

    if ( result == CommandResult::FAILED ) {
        return false;
    }
    if ( result == CommandResult::NOT_HANDLED &&
         !executeXtCommand(label) ) {
        return false;
    }

    //-----------------------------------------------------------------
    parent->repaintDrawingArea();
    return true;
}

/**
Executes the commands that need Xt.
@return false if the command failed
*/
bool XtOpenGL1GuiEventExecutor::executeXtCommand(const std::string& label)
{
    XtOpenGL1SceneBridge* bridge = parent->getSceneBridge();
    GuiEventExecutor* commands = bridge->getCommands();
    GuiState* guiState = bridge->getGuiState();
    std::string path;

    //- FILE ----------------------------------------------------------
    if ( label == "IDC_TOOLS_TANGIBLE_INTERFACE_CLIENT" ) {
        parent->showTangibleInterfaceDialog();
    }
    else if ( label == "IDC_FILE_QUIT" ) {
        parent->closeApplication();
    }
    else if ( label == "IDC_IMPORT_OBJECTS_FROM_FILE" ) {
        // The filters of the scene.import dialog of etc/gui/dialogs.json
        std::vector<FileSuffixFilter> filters =
            FileSuffixFilter::fromDialog(bridge->getDialogById("scene.import"));

        if ( parent->getUiFactory()->showFileDialog(parent, "Open",
                 guiState->getReadFolder().c_str(), filters, path) ) {
            if ( !commands->importObjects(File(path.c_str())) ) {
                Logger::reportMessage("XtOpenGL1GuiEventExecutor",
                    Logger::WARNING, "executeCommand",
                    String("Failed to read file...\n") + path.c_str());
                return false;
            }
            guiState->setReadFolder(parentFolderOf(path).c_str());
        }
    }
    else if ( label == "IDC_EXPORT_OBJECTS_TO_OBJ" ||
              label == "IDC_EXPORT_OBJECTS_TO_GTS" ||
              label == "IDC_EXPORT_OBJECTS_TO_VTK" ) {
        ExportFormat format =
            label == "IDC_EXPORT_OBJECTS_TO_OBJ" ? ExportFormat::OBJ :
            label == "IDC_EXPORT_OBJECTS_TO_GTS" ? ExportFormat::GTS :
            ExportFormat::VTK;

        if ( parent->getUiFactory()->showFileDialog(parent, "Save",
                 guiState->getWriteFolder().c_str(),
                 std::vector<FileSuffixFilter>(), path) ) {
            if ( !commands->exportObjects(File(path.c_str()), format) ) {
                Logger::reportMessage("XtOpenGL1GuiEventExecutor",
                    Logger::WARNING, "execute",
                    String("Failed to write file...\n") + path.c_str());
                return false;
            }
            guiState->setWriteFolder(parentFolderOf(path).c_str());
        }
    }
    //- RENDERING -----------------------------------------------------
    else if ( label == "Select palette for depthmap display" ||
              label == "IDC_RENDERING_SELECTPALETTEDEPTH" ) {
        std::vector<FileSuffixFilter> filters;
        filters.push_back(FileSuffixFilter("gpl", "gpl Gimp Palettes"));
        if ( parent->getUiFactory()->showFileDialog(parent, "Open",
                 currentFolder() + "/../../../../etc/palettes", filters,
                 path) ) {
            if ( !commands->loadPalette(File(path.c_str())) ) {
                Logger::reportMessage("XtOpenGL1GuiEventExecutor",
                    Logger::WARNING, "execute", "Failed to read file");
                return false;
            }
        }
    }
    else if ( label == "IDC_RENDERING_RAYTRACING" ) {
        parent->showStatusMessage(parent->getMessage("IDM_COMPUTING_RAYTRACING"));
        parent->doRaytracingImage();
        parent->showImage(bridge->getRaytracedImage());
    }
    //- CUSTOMIZE -----------------------------------------------------
    else if ( label == "IDC_CUSTOMIZE_LAF_MOTIF" ||
              label == "IDC_CUSTOMIZE_LAF_JAVA" ||
              label == "IDC_CUSTOMIZE_LAF_GTK" ||
              label == "IDC_CUSTOMIZE_LAF_WINDOWS" ) {
        // Swing look and feels: the widget set of the Xt GUI (and so its
        // look) is chosen when building the application
        parent->showStatusMessage(
            std::string("Look and feel changes are not available: this Xt GUI "
                        "is built with ") +
            parent->getUiFactory()->getWidgetSet()->getName());
    }
    else if ( label == "IDC_CUSTOMIZE_LANGUAGE_ENGLISH" ) {
        parent->setGuiLanguage("english");
    }
    else if ( label == "IDC_CUSTOMIZE_LANGUAGE_SPANISH" ) {
        parent->setGuiLanguage("spanish");
    }
    return true;
}

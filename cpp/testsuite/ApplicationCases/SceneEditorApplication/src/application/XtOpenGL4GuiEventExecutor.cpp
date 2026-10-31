#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include <unistd.h>

#include "java/io/File.h"
#include "application/commands/GuiEventExecutor.h"
#include "application/XtOpenGL4GuiEventExecutor.h"
#include "gui/xt/XtApplicationHost.h"
#include "gui/xt/XtUiFactory.h"
#include "io/FileSuffixFilter.h"
#include "model/GuiState.h"
#include "render/opengl4/XtOpenGL4SceneBridge.h"
#include "vsdk/toolkit/common/logging/Logger.h"
#include "vsdk/toolkit/gui/XtWidgetSet.h"

namespace {

java::String parentFolderOf(const java::String& path)
{
    size_t separator = path.rfind('/');
    if ( separator == -1 ) {
        return ".";
    }
    return separator == 0 ? "/" : path.substr(0, separator);
}

java::String currentFolder()
{
    char buffer[4096];
    return getcwd(buffer, sizeof(buffer)) != nullptr ? buffer : ".";
}

}

XtOpenGL4GuiEventExecutor::XtOpenGL4GuiEventExecutor(XtApplicationHost* parent)
    : parent(parent)
{
}

bool XtOpenGL4GuiEventExecutor::executeCommand(const java::String& command)
{
    java::String label(command.c_str());
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
bool XtOpenGL4GuiEventExecutor::executeXtCommand(const java::String& label)
{
    XtOpenGL4SceneBridge* bridge = parent->getSceneBridge();
    GuiEventExecutor* commands = bridge->getCommands();
    GuiState* guiState = bridge->getGuiState();
    java::String path;

    //- FILE ----------------------------------------------------------
    if ( label == "IDC_TOOLS_TANGIBLE_INTERFACE_CLIENT" ) {
        parent->showTangibleInterfaceDialog();
    }
    else if ( label == "IDC_FILE_QUIT" ) {
        parent->closeApplication();
    }
    else if ( label == "IDC_IMPORT_OBJECTS_FROM_FILE" ) {
        // The filters of the scene.import dialog of etc/gui/dialogs.json
        java::ArrayList<FileSuffixFilter> filters =
            FileSuffixFilter::fromDialog(bridge->getDialogById("scene.import"));

        if ( parent->getUiFactory()->showFileDialog(parent, "Open",
                 guiState->getReadFolder().c_str(), filters, path) ) {
            if ( !commands->importObjects(java::File(path.c_str())) ) {
                Logger::reportMessage("XtOpenGL4GuiEventExecutor",
                    Logger::WARNING, "executeCommand",
                    java::String("Failed to read file...\n") + path.c_str());
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
                 java::ArrayList<FileSuffixFilter>(), path) ) {
            if ( !commands->exportObjects(java::File(path.c_str()), format) ) {
                Logger::reportMessage("XtOpenGL4GuiEventExecutor",
                    Logger::WARNING, "execute",
                    java::String("Failed to write file...\n") + path.c_str());
                return false;
            }
            guiState->setWriteFolder(parentFolderOf(path).c_str());
        }
    }
    //- RENDERING -----------------------------------------------------
    else if ( label == "Select palette for depthmap display" ||
              label == "IDC_RENDERING_SELECTPALETTEDEPTH" ) {
        java::ArrayList<FileSuffixFilter> filters;
        filters.add(FileSuffixFilter("gpl", "gpl Gimp Palettes"));
        if ( parent->getUiFactory()->showFileDialog(parent, "Open",
                 currentFolder() + "/../../../../etc/palettes", filters,
                 path) ) {
            if ( !commands->loadPalette(java::File(path.c_str())) ) {
                Logger::reportMessage("XtOpenGL4GuiEventExecutor",
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
            java::String("Look and feel changes are not available: this Xt GUI "
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

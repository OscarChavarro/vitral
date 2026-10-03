#ifndef __GUI_EVENT_EXECUTOR__
#define __GUI_EVENT_EXECUTOR__

#include "java/io/File.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/gui/CommandListener.h"
#include "application/commands/CommandResult.h"
#include "application/commands/ExportFormat.h"
#include "application/commands/GeometryCreationCommandsExecutor.h"
#include "application/commands/Presenter.h"

class ApplicationModel;
class Scene;

/**
Executes the commands of the GUI of the editor (identified by the `IDC_*`
names of the I18N GUI definition) that only work over the application model:
creation of objects and lights, capture requests, interaction modes,
viewport and debugging toggles. It also offers the persistence operations
whose files are chosen by the GUI. It does not depend on any GUI or
rendering technology: each GUI technology executes here what it does not
present itself (file dialogs, windows, look and feel...), and presents the
status messages requested through its `Presenter`. The creation of
geometries is delegated to `GeometryCreationCommandsExecutor`.

What the commands change in the scene (i.e. objects and lights created,
objects imported) is recorded in the scene history of the model, so it can
be undone.
*/
class GuiEventExecutor : public CommandListener {
private:
    ApplicationModel* model;
    Presenter* presenter;
    GeometryCreationCommandsExecutor geometryCreationCommands;

    Scene* scene() const;
    CommandResult executeModelCommand(const java::String& label);
    java::String message(const char* id) const;

public:
    /**
    @param model application model the commands work over
    @param presenter presents the status messages of the commands
    */
    GuiEventExecutor(ApplicationModel* model, Presenter* presenter);
    virtual ~GuiEventExecutor() {}

    /**
    @param label identifier of the command (`IDC_*`)
    @return true if the command was executed
    */
    virtual bool executeCommand(const java::String& label) override;

    /**
    @param label identifier of the command (`IDC_*`)
    @return whether the command was executed, failed, or is not one of the
    commands of this class
    */
    CommandResult execute(const java::String& label);

    /**
    Adds the objects of a file to the scene.
    @param file obj or ply file (the formats read by the C++ port)
    @return false if the file can not be read (the Java version throws)
    */
    bool importObjects(const java::File& file);

    /**
    Writes the objects of the scene to a file.
    @param file destination file
    @param format format of the file
    @return false if the file can not be written (the Java version throws)
    */
    bool exportObjects(const java::File& file, ExportFormat format);

    /**
    Replaces the palette used to present depth maps.
    @param file Gimp palette (gpl) file
    @return false if the file can not be read (the Java version throws)
    */
    bool loadPalette(const java::File& file);
};

#endif

#ifndef __GUI_STATE__
#define __GUI_STATE__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"

/**
State of the GUI of the editor that does not depend on the GUI technology in
use: the language file, which panels are shown, the full screen mode and the
folders proposed by file dialogs. The I18N texts themselves are the
`ApplicationModel::getI18nContext()`.
*/
class GuiState {
public:
    /** Folder with the I18N files (one JSON file per language) used to build
    the GUI: the one of the Java application, which the C++ port shares. */
    static const char* const GUI_LANGUAGE_FOLDER;
    static const char* const JSON_EXTENSION;
    /** Folder of the data of the Java application (i.e. the icons, whose
    paths in the I18N files are relative to it), shared by the C++ port. */
    static const char* const APPLICATION_DATA_FOLDER;

private:
    java::String languageGuiFile;
    bool modifyPanelSelected;
    bool fullScreenGuiMode;
    java::String readFolder;
    java::String writeFolder;

public:
    GuiState();

    /**
    @return folder proposed when opening files: the one of the last file read
    */
    const java::String& getReadFolder() const;
    void setReadFolder(const java::String& readFolder);

    /**
    @return folder proposed when saving files: the one of the last file
    written
    */
    const java::String& getWriteFolder() const;
    void setWriteFolder(const java::String& writeFolder);

    /**
    @return I18N file the GUI is built from
    */
    const java::String& getLanguageGuiFile() const;
    void setLanguageGuiFile(const java::String& languageGuiFile);

    /**
    @return true if the panel that edits the selected body is shown
    */
    bool isModifyPanelSelected() const;
    void setModifyPanelSelected(bool modifyPanelSelected);

    /**
    @return true if only the drawing area is shown, without the rest of the
    GUI
    */
    bool isFullScreenGuiMode() const;
    void setFullScreenGuiMode(bool fullScreenGuiMode);

    /**
    Switches between showing only the drawing area and showing all the GUI.
    */
    void toggleFullScreenGuiMode();

    /**
    @return the identifiers of the languages available for the GUI, sorted:
    the names (without extension) of the JSON files in the GUI language
    folder
    */
    static java::ArrayList<java::String> listLanguages();

    /**
    @param language identifier of a language (see `listLanguages`)
    @return the I18N file of that language
    */
    static java::String languageFile(const java::String& language);

    /**
    @return the identifier of the language currently used by the GUI
    */
    java::String getCurrentLanguage() const;
};

#endif

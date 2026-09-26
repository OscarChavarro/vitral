#include <dirent.h>
#include <sys/stat.h>
#include <unistd.h>

#include "java/io/File.h"
#include "java/util/ArrayList.txx"
#include "model/GuiState.h"

// The C++ port shares the I18N files of the Java application
const char* const GuiState::GUI_LANGUAGE_FOLDER =
    "../../../../java/testsuite/ApplicationCases/SceneEditorApplication/etc/gui/";
const char* const GuiState::JSON_EXTENSION = ".json";
const char* const GuiState::APPLICATION_DATA_FOLDER =
    "../../../../java/testsuite/ApplicationCases/SceneEditorApplication/";

namespace {
bool endsWith(const java::String& text, const java::String& suffix)
{
    return text.length() >= suffix.length() &&
        text.substring(text.length() - suffix.length()).equals(suffix);
}

java::String currentDirectory()
{
    char buffer[4096];
    if ( getcwd(buffer, sizeof(buffer)) == nullptr ) {
        return java::String(".");
    }
    return java::String(buffer);
}
}

GuiState::GuiState()
{
    languageGuiFile = java::String(GUI_LANGUAGE_FOLDER) + "english" +
        JSON_EXTENSION;
    modifyPanelSelected = false;
    fullScreenGuiMode = false;
    readFolder = currentDirectory() + "/../../../../etc/geometry";
    writeFolder = ".";
}

const java::String& GuiState::getReadFolder() const
{
    return readFolder;
}

void GuiState::setReadFolder(const java::String& readFolder)
{
    this->readFolder = readFolder;
}

const java::String& GuiState::getWriteFolder() const
{
    return writeFolder;
}

void GuiState::setWriteFolder(const java::String& writeFolder)
{
    this->writeFolder = writeFolder;
}

const java::String& GuiState::getLanguageGuiFile() const
{
    return languageGuiFile;
}

void GuiState::setLanguageGuiFile(const java::String& languageGuiFile)
{
    this->languageGuiFile = languageGuiFile;
}

bool GuiState::isModifyPanelSelected() const
{
    return modifyPanelSelected;
}

void GuiState::setModifyPanelSelected(bool modifyPanelSelected)
{
    this->modifyPanelSelected = modifyPanelSelected;
}

bool GuiState::isFullScreenGuiMode() const
{
    return fullScreenGuiMode;
}

void GuiState::setFullScreenGuiMode(bool fullScreenGuiMode)
{
    this->fullScreenGuiMode = fullScreenGuiMode;
}

void GuiState::toggleFullScreenGuiMode()
{
    fullScreenGuiMode = !fullScreenGuiMode;
}

java::ArrayList<java::String> GuiState::listLanguages()
{
    java::ArrayList<java::String> languages;
    java::String extension(JSON_EXTENSION);
    DIR* directory = opendir(GUI_LANGUAGE_FOLDER);

    if ( directory != nullptr ) {
        struct dirent* entry;
        while ( (entry = readdir(directory)) != nullptr ) {
            java::String name(entry->d_name);
            java::String path = java::String(GUI_LANGUAGE_FOLDER) + name;
            struct stat status;
            if ( stat(path.c_str(), &status) == 0 &&
                 S_ISREG(status.st_mode) && endsWith(name, extension) ) {
                languages.add(name.substring(0,
                    name.length() - extension.length()));
            }
        }
        closedir(directory);
    }

    // Sorted by insertion (the list is short)
    long i;
    long j;
    for ( i = 1; i < languages.size(); i++ ) {
        java::String key = languages.get(i);
        for ( j = i - 1; j >= 0 && key < languages.get(j); j-- ) {
            languages.set(j + 1, languages.get(j));
        }
        languages.set(j + 1, key);
    }
    return languages;
}

java::String GuiState::languageFile(const java::String& language)
{
    return java::String(GUI_LANGUAGE_FOLDER) + language + JSON_EXTENSION;
}

java::String GuiState::getCurrentLanguage() const
{
    java::String name = java::File(languageGuiFile).getName();
    java::String extension(JSON_EXTENSION);

    if ( endsWith(name, extension) ) {
        name = name.substring(0, name.length() - extension.length());
    }
    return name;
}

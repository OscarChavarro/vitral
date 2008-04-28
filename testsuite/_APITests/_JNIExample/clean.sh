#!/bin/sh
rm -rf `find . -name "*~"` javaProgram/tmp javaProgram/classes
rm -rf myLibraryNative/_ide/MicrosoftVisualStudioDotNet2005/Release
rm -rf myLibraryNative/_ide/MicrosoftVisualStudioDotNet2005/Debug
rm -rf myLibraryNative/_ide/MicrosoftVisualStudioDotNet2005/*.ncb
rm -rf myLibraryNative/_ide/MicrosoftVisualStudioDotNet2005/*.suo
rm -rf myLibraryNative/_ide/MicrosoftVisualStudioDotNet2005/*.vcproj.*
rm -rf myLibraryNative/lib/*
rm -rf myLibraryNative/tmp/*
rm -rf nativeProgram/_ide/MicrosoftVisualStudioDotNet2005/Release
rm -rf nativeProgram/_ide/MicrosoftVisualStudioDotNet2005/Debug
rm -rf nativeProgram/_ide/MicrosoftVisualStudioDotNet2005/*.ncb
rm -rf nativeProgram/_ide/MicrosoftVisualStudioDotNet2005/*.suo
rm -rf nativeProgram/_ide/MicrosoftVisualStudioDotNet2005/*.vcproj.*
rm -rf nativeProgram/*.dll
rm -rf nativeProgram/*.exe
rm -rf nativeProgram/*.ilk
rm -rf nativeProgram/*.pdb
rm -rf nativeProgram/lib/*
rm -rf nativeProgram/tmp/*

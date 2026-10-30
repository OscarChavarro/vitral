#include <stdio.h>
#include <string.h>
#include <dir.h>

int main(int argc, char** argv)
{
    int valid = argc == 4 && strcmp(argv[0], "argtest") == 0 &&
        strcmp(argv[1], "alpha") == 0 &&
        strcmp(argv[2], "two words") == 0 && strcmp(argv[3], "-z") == 0 &&
        argv[argc] == 0;
    char cwd[68], contents[5] = {0};
    FILE* file;
    if (!getcwd(cwd, sizeof(cwd))) valid = 0;
    else printf("Current directory: %s\n", cwd);
    file = fopen("CWDTEST.DAT", "wb");
    if (!file) valid = 0;
    else { if (fwrite("test", 1, 4, file) != 4 || fclose(file)) valid = 0; }
    file = fopen("CWDTEST.DAT", "rb");
    if (!file) valid = 0;
    else { if (fread(contents, 1, 4, file) != 4 || fclose(file) || strcmp(contents, "test")) valid = 0; }
    printf("Argument and cwd contract: %s\n", valid ? "PASS" : "FAIL");
    return valid ? 0 : 1;
}

#include <stdio.h>

int
main(int /*argc*/, char * /*argv*/ [])
{
    FILE *fd;
    unsigned char val;

    fd = fopen("outcpp.raw", "wb");
    for ( long int i = 0; i < 1048576L; i++ ) {
        val = 0x01;
	fwrite(&val, 1, 1, fd);
        val = 0x02;
	fwrite(&val, 1, 1, fd);
        val = 0x03;
	fwrite(&val, 1, 1, fd);
    }
    fclose(fd);
    return 1;
}

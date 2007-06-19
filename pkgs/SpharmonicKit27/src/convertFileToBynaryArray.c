#include <stdio.h>

int
main(int argc, char *argv[] ) {
    FILE *fd;
    unsigned char byte;
    int i;

    if ( argc != 2 ) {
	fprintf(stderr, "Usage: %s file\n", argv[0]);
	fflush(stdout);
	return -1;
    }

    fd = fopen(argv[1], "rb");

    printf("unsigned char data[] = {\n");

    for ( i = 0; !feof(fd); i++ ) {
        fread(&byte, 1, 1, fd);
	printf("0x%02X, ", byte);
	if ( i != 0 && i % 13 == 0 ) {
	    printf("\n");
	}
    }
    printf("\n};\n");
    fclose(fd);
    return 0;
}

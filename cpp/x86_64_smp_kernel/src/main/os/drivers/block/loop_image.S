/* The FAT image of loopKernelRootFileSystem (built by scripts/makeFatImage.sh), embedded in the kernel image.
   linker64.ld places this section between __loop_image_start and __loop_image_end, and /dev/loop0 is the
   block device over that memory (drivers/block/LoopDevice.cpp). LOOP_IMAGE_PATH comes from CMakeLists.txt. */
    .section .loopimage, "aw", @progbits
    .incbin LOOP_IMAGE_PATH

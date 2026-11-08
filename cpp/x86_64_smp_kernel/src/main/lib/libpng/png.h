#ifndef VITRAL_LIBPNG_PNG_H
#define VITRAL_LIBPNG_PNG_H
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>

typedef uint32_t png_uint_32;
typedef int32_t png_int_32;
typedef struct png_control *png_controlp;
typedef struct { png_controlp opaque; png_uint_32 version; png_uint_32 width; png_uint_32 height; png_uint_32 format; png_uint_32 flags; png_uint_32 colormap_entries; png_uint_32 warning_or_error; char message[64]; } png_image, *png_imagep;
typedef struct { uint8_t red; uint8_t green; uint8_t blue; } png_color;
#define PNG_IMAGE_VERSION 1
#define PNG_FORMAT_FLAG_ALPHA 1U
#define PNG_FORMAT_FLAG_COLOR 2U
#define PNG_FORMAT_GRAY 0U
#define PNG_FORMAT_RGB PNG_FORMAT_FLAG_COLOR
#define PNG_FORMAT_RGBA (PNG_FORMAT_FLAG_COLOR | PNG_FORMAT_FLAG_ALPHA)
#define PNG_IMAGE_SIZE(image) ((size_t)(image).width * (image).height * (((image).format & PNG_FORMAT_FLAG_COLOR ? 3 : 1) + ((image).format & PNG_FORMAT_FLAG_ALPHA ? 1 : 0)))
int png_image_begin_read_from_memory(png_imagep image, const void *memory, size_t size) asm("png_image_begin_read_from_memory");
int png_image_begin_read_from_stdio(png_imagep image, FILE *file) asm("png_image_begin_read_from_stdio");
int png_image_begin_read_from_file(png_imagep image, const char *filename) asm("png_image_begin_read_from_file");
int png_image_finish_read(png_imagep image, const png_color *background, void *buffer, png_int_32 row_stride, void *colormap) asm("png_image_finish_read");
void png_image_free(png_imagep image) asm("png_image_free");
typedef unsigned char png_byte;
typedef png_byte *png_bytep;
typedef png_bytep *png_bytepp;
typedef void *png_voidp;
typedef struct png_struct_def *png_structp;
typedef struct png_info_def *png_infop;
typedef void (*png_error_ptr)(png_structp, const char *);

#define PNG_LIBPNG_VER_STRING "1.6.0"
#define PNG_COLOR_TYPE_GRAY 0
#define PNG_COLOR_TYPE_RGB 2
#define PNG_COLOR_TYPE_PALETTE 3
#define PNG_COLOR_TYPE_GRAY_ALPHA 4
#define PNG_COLOR_TYPE_RGB_ALPHA 6
#define PNG_INFO_tRNS 0x0010
#define PNG_FILLER_AFTER 1
#define PNG_INTERLACE_NONE 0
#define PNG_COMPRESSION_TYPE_DEFAULT 0
#define PNG_FILTER_TYPE_DEFAULT 0
#define png_jmpbuf(png) png_get_jump_buffer(png)
#ifndef setjmp
#define setjmp(buffer) __builtin_setjmp(buffer)
#endif

void **png_get_jump_buffer(png_structp png) asm("png_get_jump_buffer");
png_structp png_create_read_struct(const char *version, png_voidp error_ptr, png_error_ptr error_fn, png_error_ptr warning_fn) asm("png_create_read_struct");
png_structp png_create_write_struct(const char *version, png_voidp error_ptr, png_error_ptr error_fn, png_error_ptr warning_fn) asm("png_create_write_struct");
png_infop png_create_info_struct(png_structp png) asm("png_create_info_struct");
void png_destroy_read_struct(png_structp *png, png_infop *info, png_infop *end_info) asm("png_destroy_read_struct");
void png_destroy_write_struct(png_structp *png, png_infop *info) asm("png_destroy_write_struct");
void png_init_io(png_structp png, FILE *file) asm("png_init_io");
void png_read_info(png_structp png, png_infop info) asm("png_read_info");
png_uint_32 png_get_image_width(png_structp png, png_infop info) asm("png_get_image_width");
png_uint_32 png_get_image_height(png_structp png, png_infop info) asm("png_get_image_height");
png_byte png_get_color_type(png_structp png, png_infop info) asm("png_get_color_type");
png_byte png_get_bit_depth(png_structp png, png_infop info) asm("png_get_bit_depth");
png_uint_32 png_get_valid(png_structp png, png_infop info, png_uint_32 flag) asm("png_get_valid");
void png_set_strip_16(png_structp png) asm("png_set_strip_16");
void png_set_palette_to_rgb(png_structp png) asm("png_set_palette_to_rgb");
void png_set_expand_gray_1_2_4_to_8(png_structp png) asm("png_set_expand_gray_1_2_4_to_8");
void png_set_tRNS_to_alpha(png_structp png) asm("png_set_tRNS_to_alpha");
void png_set_gray_to_rgb(png_structp png) asm("png_set_gray_to_rgb");
void png_set_strip_alpha(png_structp png) asm("png_set_strip_alpha");
void png_set_filler(png_structp png, png_uint_32 filler, int flags) asm("png_set_filler");
void png_read_update_info(png_structp png, png_infop info) asm("png_read_update_info");
void png_read_image(png_structp png, png_bytepp rows) asm("png_read_image");
void png_set_IHDR(png_structp png, png_infop info, png_uint_32 width, png_uint_32 height, int bit_depth, int color_type, int interlace_type, int compression_type, int filter_type) asm("png_set_IHDR");
void png_write_info(png_structp png, png_infop info) asm("png_write_info");
void png_write_image(png_structp png, png_bytepp rows) asm("png_write_image");
void png_write_end(png_structp png, png_infop info) asm("png_write_end");

#endif

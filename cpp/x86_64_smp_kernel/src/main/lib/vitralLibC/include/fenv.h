#ifndef __FENV_H__
#define __FENV_H__

// Rounding modes of the floating point environment, with the values of the x87 control word (and of MXCSR shifted
// 3 bits right). The kernel starts in round to nearest; only the rounding mode is supported.
#define FE_TONEAREST 0x000
#define FE_DOWNWARD 0x400
#define FE_UPWARD 0x800
#define FE_TOWARDZERO 0xc00

extern "C" {
// The rounding mode of the x87 unit (the SSE unit is kept equal by fesetround).
int fegetround(void);
// Sets the rounding mode of the x87 and SSE units; returns 0, or nonzero for an unknown mode.
int fesetround(int mode);
}

#endif

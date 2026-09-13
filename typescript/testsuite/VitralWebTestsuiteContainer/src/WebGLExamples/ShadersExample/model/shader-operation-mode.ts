/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/model/ShaderOperationMode.java`.
 *
 * Java's `enum` carries a `next()` that walks the declaration order and wraps,
 * which is what the `[.]` binding calls. TypeScript has no `ordinal()`, so the
 * declaration order is the array below and `next` indexes it; the two names,
 * their order and the wrap are the Java ones.
 */
export type ShaderOperationMode = 'OPENGL_4_1' | 'SOFTWARE';

const VALUES: readonly ShaderOperationMode[] = ['OPENGL_4_1', 'SOFTWARE'];

export function nextShaderOperationMode(mode: ShaderOperationMode): ShaderOperationMode {
  const nextIndex = (VALUES.indexOf(mode) + 1) % VALUES.length;
  return VALUES[nextIndex]!;
}

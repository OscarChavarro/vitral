# TangibleInterfaceLabelsCreator

A command-line tool that generates an A4 PDF sheet with **red tangible interface
label squares**. Each square contains:

- A red outer frame.
- A red filled circle at the geometric center, with radius equal to `10%` of the
  square side length.
- A black label title placed in the upper half of the square, supporting
  multiple lines through embedded `\n`.

This first version emits a fixed set of labels:

- `Ray`
- `Omni\nLight`
- `SpotLight`
- `Camera`
- `Cutting\nPlane`
- `Object`

## Usage

```sh
tangibleInterfaceLabelsCreator [-size SIZE] [OUTPUT.pdf]
```

## Options

- `-size SIZE`: Side length of the red label square. Accepts `40mm` or plain
  `40`. Default: `40mm`.
- `OUTPUT.pdf`: Positional output path. If omitted, a name like
  `labels_40mm_a4.pdf` is used.

## Building and running

```sh
./scripts/compile.sh
testsuite/Tools/TangibleInterfaceLabelsCreator/run.sh -size 40mm out.pdf
```

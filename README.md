# JBasic

JBasic is a BASIC interpreter in Java.

![Mandelbrot Set](./samples/mandelbrot.png)

## Reference
Every statement needs to be on one line only. Spacing is essential for
arithmetic operators.

Currently, arithmetic expressions are evaluated left-to-right, unless
overridden by parenthesis. This is simpler but erroneous mathematically.
So 2+4*2 is 12, not 10.

There is no AND, OR boolean operators, yet.

Line numbers or explicit labels can be used. No need to increment line
numbers but they need to be unique or otherwise last one overrides the
previous line numbers.

SCREEN and PLOT statements are available for graphics programming.
Only mode 13 (320x200) is available. 
See Mandelbrot.bas for an example use, from QuiteBasic.com.



Serkan Kenar

Dubai, 2019.

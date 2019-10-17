1 SCREEN 13
5 LET L = 100
10 FOR I = 0 TO 320
20 FOR J = 0 TO 200
25 PLOT I , J , 1
30 LET U = I / 100 - 1.5
40 LET V = J / 100 - 1
60 LET X = U
70 LET Y = V
80 LET N = 0
90 LET R = X * X
100 LET Q = Y * Y
110 IF R + Q > 4 THEN GOTO plot
120 IF N >= L THEN GOTO plot
130 LET Y = 2 * X * Y + V
140 LET X = R - Q + U
150 LET N = N + 1
160 GOTO 90
plot: LET C = 0
IF N >= 10 THEN LET C = 8 * (N - 10) / (L - 10)
200 PLOT I , J , C
210 NEXT J
220 NEXT I
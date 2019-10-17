package org.feyvi.serkan.jbasic;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JBasicTest {
    @Test
    void testHelloWorld() {
        String source = "10 PRINT 3.14\n20 GOTO 10\n";
        JBasic interpreter = new JBasic(source);
        var errors = interpreter.getErrors();
        assertEquals(0, errors.size());
    }

    @Test
    void testFunctionCallExpression() {
        List<String> source = new ArrayList<>(Arrays.asList(
                "10 IF SQR(4) = 2 THEN PRINT 3.14",
                "15 PRINT SQR(4)",
                "20 PRINT ABS(-4)",
                "30 PRINT SGN(-4)",
                "40 PRINT ROUND(EXP(4))",
                "50 PRINT ROUND(SIN(90 * 3.1415 / 180.0))",
                "60 PRINT ROUND(COS(90 * 3.1415 / 180.0))",
                "70 PRINT ROUND(4.49)",
                "80 PRINT CEIL(4.49)"
        ));
        JBasic interpreter = new JBasic(source);
        var errors = interpreter.getErrors();
        errors.forEach(System.err::println);
        assertEquals(0, errors.size());
        String output = captureRunOutput(interpreter);
        assertEquals(output, "3.14\n2.0\n4.0\n-1.0\n55.0\n1.0\n0.0\n4.0\n5.0\n");
    }

    private String captureRunOutput(JBasic interpreter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8);
        interpreter.run(ps);
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    void testWrongForLoops() {
        List<String> source = new ArrayList<>(Arrays.asList(
                "10 FOR I = 1 TO 100",
                "20 NEXT J"
        ));
        JBasic interpreter = new JBasic(source);
        var errors = interpreter.getErrors();
        assertEquals(1, errors.size());
        String expectedError = "20 NEXT J\n" +
                "Error [Line 1]: Invalid variable for the for loop, Expecting I, found J";
        assertEquals(expectedError, errors.get(0));
    }
}
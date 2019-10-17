/* JBasic - BASIC interpreter
 *
 * Serkan Kenar
 * Dubai, 2019.
 */
package org.feyvi.serkan.jbasic;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/*
 jBasic - BASIC Interpreter

 The original dartmouth reference:
 http://bitsavers.trailing-edge.com/pdf/dartmouth/BASIC_Oct64.pdf

  Reference:
  Every statement needs to be on one line only. Spacing is essential for arithmetic operators.

  Arithmetic expressions are evaluated left-to-right, unless overridden by parenthesis.
  This is simpler but erroneous mathematically. So 2+4*2 is 12, not 10.

  There is no AND, OR boolean operators, yet.

  Line numbers or explicit labels can be used. No need to increment line numbers but they need to be
  unique or otherwise last one overrides the previous line numbers.

  SCREEN and PLOT statements are available for graphics programming.
  Only mode 13 (320x200) is available.
 */

class JBasicContext {
    Canvas canvas;
    int counter;
    Map<String, BasicValue> vars;
    Map<String, Integer> labels;
    PrintStream out;

    JBasicContext(Map<String, Integer> labels) {
        this.counter = 0;
        this.labels = labels;
        this.vars = new HashMap<>();
    }
}

class Statement {
    public void run(JBasicContext context) {
        // do nothing
    }
}

// Expression is like statement, but has a value
class BasicExpression extends Statement {
    private BasicValue value;

    BasicExpression() {
    }

    BasicExpression(BasicValue value) {
        this.value = value;
    }

    BasicValue getValue(JBasicContext context) {
        return this.value;
    }
}

class VariableExpression extends BasicExpression {
    private String varName;

    VariableExpression(String varName) {
        this.varName = varName;
    }

    @Override
    public BasicValue getValue(JBasicContext context) {
        return context.vars.get(this.varName);
    }
}

class ArithmeticExpression extends BasicExpression {
    private final BasicExpression leftExpr;
    private final BasicExpression rightExpr;
    private final String operator;

    ArithmeticExpression(BasicExpression leftExpr, String operator, BasicExpression rightExpr) {
        this.leftExpr = leftExpr;
        this.rightExpr = rightExpr;
        this.operator = operator;
    }

    @Override
    BasicValue getValue(JBasicContext context) {
        Double leftVal = leftExpr.getValue(context).dblValue;
        Double rightVal = rightExpr.getValue(context).dblValue;
        if (operator.equalsIgnoreCase("+")) {
            return new BasicValue(leftVal + rightVal);
        } else if (operator.equalsIgnoreCase("-")) {
            return new BasicValue(leftVal - rightVal);
        } else if (operator.equalsIgnoreCase("*")) {
            return new BasicValue(leftVal * rightVal);
        } else if (operator.equalsIgnoreCase("/")) {
            return new BasicValue(leftVal / rightVal);
        } else if (operator.equalsIgnoreCase("^")) {
            return new BasicValue(Math.pow(leftVal, rightVal));
        } else if (operator.equalsIgnoreCase("=")) {
            return new BasicValue(leftVal.equals(rightVal) ? 1.0 : 0.0);
        } else if (operator.equalsIgnoreCase("<")) {
            return new BasicValue(leftVal < rightVal ? 1.0 : 0.0);
        } else if (operator.equalsIgnoreCase("<=")) {
            return new BasicValue(leftVal <= rightVal ? 1.0 : 0.0);
        } else if (operator.equalsIgnoreCase(">")) {
            return new BasicValue(leftVal > rightVal ? 1.0 : 0.0);
        } else if (operator.equalsIgnoreCase(">=")) {
            return new BasicValue(leftVal >= rightVal ? 1.0 : 0.0);
        }
        return null;
    }
}

class CallExpression extends BasicExpression {

    private final String funcName;
    private final BasicExpression params;

    CallExpression(String funcName, BasicExpression params) {
        this.funcName = funcName;
        this.params = params;
    }

    @Override
    BasicValue getValue(JBasicContext context) {
        BasicValue val = null;
        if (funcName.equalsIgnoreCase("ABS")) {
            val = new BasicValue(Math.abs(params.getValue(context).dblValue));
        } else if (funcName.equalsIgnoreCase("SQR")) {
            val = new BasicValue(Math.sqrt(params.getValue(context).dblValue));
        } else if (funcName.equalsIgnoreCase("EXP")) {
            val = new BasicValue(Math.exp(params.getValue(context).dblValue));
        } else if (funcName.equalsIgnoreCase("SIN")) {
            val = new BasicValue(Math.sin(params.getValue(context).dblValue));
        } else if (funcName.equalsIgnoreCase("COS")) {
            val = new BasicValue(Math.cos(params.getValue(context).dblValue));
        } else if (funcName.equalsIgnoreCase("ROUND")) {
            val = new BasicValue((double) Math.round(params.getValue(context).dblValue));
        } else if (funcName.equalsIgnoreCase("SGN")) {
            val = new BasicValue(Math.signum(params.getValue(context).dblValue));
        } else if (funcName.equalsIgnoreCase("CEIL")) {
            val = new BasicValue(Math.ceil(params.getValue(context).dblValue));
        }
        return val;
    }
}

class BasicValue {

    BasicValue() {
    }

    BasicValue(Double dblValue) {
        this.dblValue = dblValue;
    }

    BasicValue(BasicValue value) {
        this.dblValue = value.dblValue;
    }

    Double dblValue;

    @Override
    public String toString() {
        return dblValue.toString();
    }
}

class LetStatement extends Statement {
    private String variable;
    private BasicExpression expr;

    LetStatement(String varName, BasicExpression expr) {
        this.variable = varName;
        this.expr = expr;
    }

    @Override
    public void run(JBasicContext context) {
        context.vars.put(variable, expr.getValue(context));
    }
}

class GotoStatement extends Statement {
    private String label;
    private Integer counter;

    GotoStatement(int counter) {
        this.counter = counter;
    }

    GotoStatement(String label) {
        this.label = label;
    }

    @Override
    public void run(JBasicContext context) {
        context.counter = counter != null ? this.counter : context.labels.get(this.label);
    }
}

class PrintStatement extends Statement {
    private ArrayList<BasicExpression> expressions;

    PrintStatement(ArrayList<BasicExpression> expressions) {
        this.expressions = expressions;
    }

    public void run(JBasicContext context) {
        for (BasicExpression expr : expressions) {
            context.out.println(expr.getValue(context));
        }
    }
}

class ForStatement extends Statement {
    private final String varName;
    private final BasicExpression start;
    private final BasicExpression end;
    private BasicValue counterVar;
    private int endFor;

    ForStatement(String varName, BasicExpression start, BasicExpression end) {
        this.varName = varName;
        this.start = start;
        this.end = end;
        this.counterVar = null;
    }

    void setEndFor(int endFor) {
        this.endFor = endFor;
    }

    @Override
    public void run(JBasicContext context) {
        if (this.counterVar == null) {
            this.counterVar = new BasicValue(start.getValue(context));
            context.vars.put(varName, counterVar);
        } else {
            this.counterVar.dblValue++;
            if (this.counterVar.dblValue > end.getValue(context).dblValue) {
                context.counter = this.endFor;
                resetCounter(context);
            }
        }
    }

    private void resetCounter(JBasicContext context) {
        context.vars.remove(varName);
        this.counterVar = null;
    }

    String getVarName() {
        return this.varName;
    }
}

class IfStatement extends Statement {
    private final BasicExpression condition;
    private final Statement thenStatement;

    IfStatement(BasicExpression condition, Statement thenStatement) {
        this.condition = condition;
        this.thenStatement = thenStatement;
    }

    @Override
    public void run(JBasicContext context) {
        if (this.condition.getValue(context).dblValue == 1.0) {
            thenStatement.run(context);
        }
    }
}

class ScreenStatement extends Statement {
    private final BasicExpression mode;

    ScreenStatement(BasicExpression mode) {
        this.mode = mode;
    }

    @Override
    public void run(JBasicContext context) {
        if (mode.getValue(context).dblValue == 13.0) {
            JFrame frame = new JFrame("jBASIC");
            Canvas canvas = new Canvas();
            canvas.setSize(640, 400);
            frame.add(canvas);
            frame.pack();
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            context.canvas = canvas;
        }
    }
}

class PlotStatement extends Statement {
    private final BasicExpression x;
    private final BasicExpression y;
    private final BasicExpression c;

    PlotStatement(BasicExpression x, BasicExpression y, BasicExpression c) {
        this.x = x;
        this.y = y;
        this.c = c;
    }

    @Override
    public void run(JBasicContext context) {
        Graphics2D g = (Graphics2D) context.canvas.getGraphics();
        float colorValue = c.getValue(context).dblValue.floatValue();
        colorValue /= 8.0; // FIXME
        int xVal = x.getValue(context).dblValue.intValue();
        int yVal = y.getValue(context).dblValue.intValue();
        g.setColor(new Color(colorValue, colorValue, colorValue));

        g.fillRect(xVal * 2, yVal * 2, 2, 2);
    }
}

public class JBasic {

    private final List<String> source;
    private String[] tokens;
    private int currentLine;
    private int tokenIdx;
    private ArrayList<Statement> statements;
    private HashMap<String, Integer> labels;
    private ArrayList<String> errors;
    private Stack<Integer> forStatements;

    public JBasic(String source) {
        this.source = Arrays.asList(source.trim().split("\\s*\n\\s*"));
        setupInterpreter();
        parse();
    }

    public JBasic(List<String> sourceLines) {
        this.source = sourceLines;
        setupInterpreter();
        parse();
    }

    private void setupInterpreter() {
        this.errors = new ArrayList<>();
        this.statements = new ArrayList<>();
        this.labels = new HashMap<>();
        this.forStatements = new Stack<>();
    }

    private void parse() {
        currentLine = 0;
        for (String line : source) {
            String label;
            String verb;
            tokenIdx = 0;
            tokens = line.replaceAll("\\(", " \\( ")
                    .replaceAll("\\)", " \\) ")
                    .split("\\s+");
            if (tokens.length == 0) continue;
            if (Character.isDigit(tokens[0].charAt(0))) {
                label = tokens[0];
                verb = tokens[1];
                tokenIdx = 2;
            } else if (tokens[0].endsWith(":")) {
                label = tokens[0].substring(0, tokens[0].length() - 1);
                verb = tokens[1];
                tokenIdx = 2;
            } else {
                label = String.valueOf(currentLine);
                verb = tokens[0];
                tokenIdx = 1;
            }
            statements.add(parseStatement(verb));
            labels.put(label, statements.size() - 1);
            currentLine++;
        }
    }

    private Statement parseStatement(String verb) {
        Statement statement = null;
        if (verb.equalsIgnoreCase("LET")) {
            statement = parseLetStatement();
        } else if (verb.equalsIgnoreCase("PRINT")) {
            statement = parsePrintStatement();
        } else if (verb.equalsIgnoreCase("GOTO")) {
            statement = parseGotoStatement();
        } else if (verb.equalsIgnoreCase("FOR")) {
            statement = parseForStatement();
        } else if (verb.equalsIgnoreCase("NEXT")) {
            statement = parseNextStatement();
        } else if (verb.equalsIgnoreCase("IF")) {
            statement = parseIfStatement();
        } else if (verb.equalsIgnoreCase("SCREEN")) {
            statement = parseScreenStatement();
        } else if (verb.equalsIgnoreCase("PLOT")) {
            statement = parsePlotStatement();
        }
        return statement;
    }

    private Statement parsePlotStatement() {
        BasicExpression x = parseExpression();
        consume(",");
        BasicExpression y = parseExpression();
        consume(",");
        BasicExpression c = parseExpression();
        return new PlotStatement(x, y, c);
    }

    private Statement parseScreenStatement() {
        BasicExpression mode = parseExpression();
        return new ScreenStatement(mode);
    }

    private Statement parseIfStatement() {
        BasicExpression q = parseExpression();
        consume("THEN");
        String verb = tokens[tokenIdx++];
        Statement thenStatement = parseStatement(verb);
        return new IfStatement(q, thenStatement);
    }

    private Statement parseNextStatement() {
        String varName = tokens[tokenIdx];
        int forStatementId = forStatements.peek();
        ForStatement fs = (ForStatement) statements.get(forStatementId);
        if (!fs.getVarName().equalsIgnoreCase(varName)) {
            error(String.format("Invalid variable for the for loop, Expecting %s, found %s",
                    fs.getVarName(), varName));
        }
        forStatements.pop();
        fs.setEndFor(statements.size() + 1);
        return new GotoStatement(forStatementId);
    }

    private Statement parseForStatement() {
        String varName = tokens[tokenIdx++];
        consume("=");
        BasicExpression start = parseExpression();
        consume("TO");
        BasicExpression end = parseExpression();
        forStatements.push(statements.size());
        return new ForStatement(varName, start, end);
    }

    private void consume(String expected) {
        if (!tokens[tokenIdx].equals(expected)) {
            error(String.format("Expecting %s but got %s", expected, tokens[tokenIdx]));
        }
        tokenIdx++;
    }

    private void error(String errorMessage) {
        errors.add(String.format("%s\nError [Line %d]: %s", source.get(currentLine),
                currentLine, errorMessage));
    }

    private Statement parseGotoStatement() {
        return new GotoStatement(tokens[tokenIdx++]);
    }

    private PrintStatement parsePrintStatement() {
        ArrayList<BasicExpression> exprs = new ArrayList<>();
        var expr = parseExpression();
        exprs.add(expr);
        return new PrintStatement(exprs);
    }

    private LetStatement parseLetStatement() {
        String variable = tokens[tokenIdx++];
        consume("=");
        return new LetStatement(variable, parseExpression());
    }

    private BasicExpression parseExpression() {
        BasicExpression leftVal = parseAtom();
        BasicExpression resultExpr = leftVal;
        if (tokenIdx >= tokens.length) return leftVal;
        while (tokenIdx < tokens.length) {
            // a very poor man's operator check
            String thisToken = tokens[tokenIdx];
            if ((thisToken.length() == 1 && "+-*/^<>=".contains(thisToken)) ||
                    (thisToken.length() == 2 && ">=<=".contains(thisToken))) {
                String operator = tokens[tokenIdx++];
                BasicExpression expr = parseAtom();
                resultExpr = new ArithmeticExpression(resultExpr, operator, expr);
            } else {
                break;
            }
        }
        return resultExpr;
    }

    private BasicExpression parseAtom() {
        BasicValue val = new BasicValue();
        BasicExpression expr;
        try {
            val.dblValue = Double.valueOf(tokens[tokenIdx]);
            expr = new BasicExpression(val);
            tokenIdx++;
        } catch (NumberFormatException e) {
            if (tokens[tokenIdx].startsWith("(")) {
                consume("(");
                expr = parseExpression();
                consume(")");
            } else if (tokenIdx + 1 < tokens.length && tokens[tokenIdx + 1].equals("(")) {
                expr = parseCallExpression();
            } else {
                expr = new VariableExpression(tokens[tokenIdx]);
            }
        }
        //tokenIdx++;
        return expr;
    }

    private BasicExpression parseCallExpression() {
        String funcName = tokens[tokenIdx++];
        consume("(");
        BasicExpression paramsExpr = parseExpression();
        consume(")");
        return new CallExpression(funcName, paramsExpr);
    }

    public List<String> getErrors() {
        return this.errors;
    }

    private void run() {
        run(System.out);
    }

    public void run(PrintStream stream) {
        JBasicContext context = new JBasicContext(this.labels);
        context.out = stream;
        run(context);
    }

    private void run(JBasicContext context) {
        while (context.counter < this.statements.size()) {
            Statement currentStatement = this.statements.get(context.counter);
            context.counter++;
            currentStatement.run(context);
        }
    }

    private static void halt(String msg) {
        System.err.println(msg);
        System.exit(-1);
    }

    public static void main(String[] args) {
        JBasic interpreter;
        List<String> sourceLines;
        if (args.length == 0)
            halt("No source given.");

        try {
            sourceLines = Files.readAllLines(Paths.get(args[0]));
            interpreter = new JBasic(sourceLines);
            interpreter.parse();
            List<String> errors = interpreter.getErrors();
            if (errors.size() != 0) {
                errors.forEach(System.err::println);
                halt("Interpretation failed.");
            }

            interpreter.run();

        } catch (IOException e) {
            halt(String.format("Unable to read file %s", args[1]));
        }
    }

}

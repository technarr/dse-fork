package tools.aqua.dse.objects;

import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.functions.Function;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Objects {

    public static final Function<BuiltinTypes.BoolType> extendsFct = new Function("extends",
            BuiltinTypes.BOOL, BuiltinTypes.STRING, BuiltinTypes.STRING);

    public static final  Function<BuiltinTypes.BoolType> initializesFct = new Function("initializes",
            BuiltinTypes.BOOL, BuiltinTypes.STRING, BuiltinTypes.STRING);

    private final HashMap<String, Clazz> clazzes = new HashMap<>();

    public void initObjectsStructure(SolverContext ctx) {
        ArrayList<String> methods = new ArrayList<>();
        for (Clazz c : clazzes.values()) {
            methods.addAll(Arrays.stream(c.getConstructors()).collect(Collectors.toList()));
            for (String other : clazzes.keySet()) {
                FunctionExpression application = new FunctionExpression(extendsFct,
                        new Constant<String>(BuiltinTypes.STRING, c.getName()),
                        new Constant<String>(BuiltinTypes.STRING, other));

                ctx.add(new PropositionalCompound(application, LogicalOperator.EQUIV,
                        c.hasSuper(other) ? ExpressionUtil.TRUE : ExpressionUtil.FALSE));
            }
        }
        for (Clazz c : clazzes.values()) {
            for (String m : methods) {
                FunctionExpression application = new FunctionExpression(initializesFct,
                        new Constant<String>(BuiltinTypes.STRING, m),
                        new Constant<String>(BuiltinTypes.STRING, c.getName()));

                ctx.add(new PropositionalCompound(application, LogicalOperator.EQUIV,
                        c.hasConstructor(m) ? ExpressionUtil.TRUE : ExpressionUtil.FALSE));
            }
        }
    }

    public Objects(String config) {
        fromString(config);
        String[] cNames = clazzes.keySet().toArray(new String[] {});
        Clazz NULL = new Clazz("_NULL", cNames, new String[] {"_NULL()"});
        clazzes.put("_NULL", NULL);
    }

    private void fromString(String text) {
        text = text.trim();
        while (text.startsWith("class")) {
            int splitIndex =  text.indexOf("}");
            String entry = text.substring(5, text.indexOf("}")).trim();
            classFromString(entry);
            text = text.substring(splitIndex+1).trim();
        }
        if (!text.isEmpty()) {
            throw new RuntimeException("cannot parse: " + text);
        }
    }

    private void classFromString(String text) {
        text = text.trim();
        int splitIndex1 =  text.indexOf("extends");
        int splitIndex2 =  text.indexOf("{");
        if (splitIndex1 < 0) {
            splitIndex1 = splitIndex2;
        }
        String clazz = text.substring(0, splitIndex1).trim();
        String[] superClasses = new String[] {};
        if (splitIndex1 < splitIndex2) {
            for (int i=0; i<superClasses.length; i++) {
                superClasses[i] = superClasses[i].trim();
            }
        }
        String[] constructors = text.substring(splitIndex2+1).trim().split(",");

        for (int i=0; i<constructors.length; i++) {
            constructors[i] = constructors[i].trim();
        }
        if (constructors.length == 1 && constructors[0].isEmpty()) constructors = new String[] {};
        this.clazzes.put(clazz, new Clazz(clazz, superClasses, constructors));
    }

    public HashMap<String, Clazz> getClazzes() {
        return clazzes;
    }
}

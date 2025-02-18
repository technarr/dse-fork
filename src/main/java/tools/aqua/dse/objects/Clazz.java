package tools.aqua.dse.objects;

import java.util.Arrays;

public class Clazz {

    private final String name;
    private final String[] superClasses;
    private final String[] constructors;

    public Clazz(String name, String[] superClasses, String[] constructors) {
        this.name = name;
        this.superClasses = superClasses;
        this.constructors = constructors;
    }

    public String getName() {
        return name;
    }

    public String[] getSuperClasses() {
        return superClasses;
    }

    public String[] getConstructors() {
        return constructors;
    }

    @Override
    public String toString() {
        return "Clazz{" +
                "name='" + name + '\'' +
                ", superClasses=" + Arrays.toString(superClasses) +
                ", constructors=" + Arrays.toString(constructors) +
                '}';
    }

    public boolean hasSuper(String other) {
        return name.equals(other) || Arrays.stream(superClasses).filter( (n) -> n.equals(other) ).findFirst().isPresent();
    }

    public boolean hasConstructor(String m) {
        return Arrays.stream(constructors).filter( (n) -> n.equals(m) ).findFirst().isPresent();
    }
}

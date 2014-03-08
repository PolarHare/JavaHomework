package ru.ifmo.ctddev.polyarnyi.task3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Date: 08.03.14 at 13:27
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class Implementor {

    private static final String USAGE = "Proper Usage is: \"java Implementor [className] [className]*\"\n" +
            "Where [className] - is a full path to class ot interface to be mocked.";
    private static final String ERROR_CLASS_NOT_FOUND_PREFIX = "Class \"";
    private static final String ERROR_CLASS_NOT_FOUND_SUFFIX = "\" not found!";
    private static final String ERROR_IO_EXCEPTION = "Input/Output error has occurred!";
    private static final String CLASS_IMPLEMENTATION_NAME_SUFFIX = "Impl";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(USAGE);
            return;
        }
        for (String className : args) {
            System.out.println("Generating implementation for " + className + "...");
            try {
                Class<?> clazz = Class.forName(className);
                Implementor implementor = new Implementor();
                String fileName = "task3/src/main/java/" + clazz.getPackage().getName().replace(".", "/")
                        + (clazz.getPackage().getName().isEmpty() ? "" : "/")
                        + clazz.getSimpleName() + CLASS_IMPLEMENTATION_NAME_SUFFIX + ".java";
                File newFile = new File(fileName);
                if (!newFile.getParentFile().exists() && !newFile.getParentFile().mkdirs()) {
                    throw new IOException("Folders in path to new file \"" + fileName + "\" was not successfully created!");
                }
                try (FileWriter out = new FileWriter(newFile)) {
                    implementor.generateImplementation(clazz, clazz.getSimpleName() + CLASS_IMPLEMENTATION_NAME_SUFFIX,
                            out);
                }
            } catch (ClassNotFoundException e) {
                System.out.println("  " + ERROR_CLASS_NOT_FOUND_PREFIX + className + ERROR_CLASS_NOT_FOUND_SUFFIX);
            } catch (IOException e) {
                System.out.println("  " + ERROR_IO_EXCEPTION);
            }
        }
    }

    private static final String LF = "\n";

    private void generateImplementation(Class<?> clazz, String implClassName, Appendable out) throws IOException {
        final String packageName = clazz.getPackage().getName();
        appendPackage(packageName, out);
        out.append(LF);
        Set<Class<?>> usedClasses = findUsedClasses(clazz);
        if (!usedClasses.isEmpty()) {
            appendImports(usedClasses, out);
            out.append(LF);
        }
        appendClassDeclaration(clazz, implClassName, out);
        appendConstructor(clazz, implClassName, out);
        appendMethodsImplementations(clazz, out);
        out.append("}");
    }

    private void appendPackage(String packageName, Appendable out) throws IOException {
        out.append("package ").append(packageName).append(";").append(LF);
    }

    private void appendImports(Set<Class<?>> classes, Appendable out) throws IOException {
        for (Class clazz : classes) {
            out.append("import ").append(clazz.getCanonicalName()).append(";").append(LF);
        }
    }

    private void appendClassDeclaration(Class<?> clazz, String implClassName, Appendable out) throws IOException {
        out.append("public class ").append(implClassName).append(" ");
        if (clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isArray() || clazz.isEnum() || clazz.isLocalClass()
                || clazz.isMemberClass() || clazz.isPrimitive() || clazz.isSynthetic()) {
            throw new IllegalArgumentException();
        } else if (Modifier.isFinal(clazz.getModifiers())) {
            throw new IllegalArgumentException("We can't mock final classes!");
        }

        if (clazz.isInterface()) {
            out.append("implements ").append(clazz.getSimpleName());
        } else {
            out.append("extends ").append(clazz.getSimpleName());
        }
        out.append(" {" + LF);
    }

    private void appendConstructor(Class<?> clazz, String classImplName, Appendable out) throws IOException {
        Constructor<?>[] constructors = clazz.getConstructors();
        boolean defaultConstructorExists = (constructors.length == 0);
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0) {
                defaultConstructorExists = true;
            }
        }
        if (!defaultConstructorExists) {
            out.append(LF);
            out.append("    public ").append(classImplName).append("() {").append(LF);
            out.append("        super(");
            Class<?>[] parameterTypes = constructors[0].getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameter = parameterTypes[i];
                out.append(getDefaultValueForClass(parameter));
                if (i != parameterTypes.length - 1) {
                    out.append(", ");
                } else {
                    out.append(");").append(LF);
                }
            }
            out.append("    }").append(LF);
        }
    }

    private void appendMethodsImplementations(Class<?> clazz, Appendable out) throws IOException {
        for (Method method : getListOfMethodsToBeOverriden(clazz)) {
            out.append(LF);
            String modifier;
            out.append("    @Override").append(LF);
            if (Modifier.isPublic(method.getModifiers())) {
                modifier = "    public ";
            } else if (Modifier.isProtected(method.getModifiers())) {
                modifier = "    protected ";
            } else if (!Modifier.isProtected(method.getModifiers()) && !Modifier.isPublic(method.getModifiers())
                    && !Modifier.isPrivate(method.getModifiers())) {//package local modifier
                modifier = "    ";
            } else {
                throw new IllegalArgumentException();
            }
            out.append(modifier)
                    .append(method.getReturnType().getSimpleName()).append(" ")
                    .append(method.getName()).append("(");

            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> paramType = parameterTypes[i];
                out.append(paramType.getSimpleName()).append(" arg").append(Integer.toString(i + 1));
                if (i != parameterTypes.length - 1) {
                    out.append(", ");
                }
            }
            out.append(") {").append(LF);
            if (!method.getReturnType().equals(Void.TYPE)) {
                out.append("        return ").append(getDefaultValueForClass(method.getReturnType())).append(";").append(LF);
            }
            out.append("    }").append(LF);
        }
    }

    private String getDefaultValueForClass(Class<?> type) {
        if (type.isPrimitive()) {
            if (type.equals(Boolean.TYPE)) {
                return "false";
            } else {
                return "0";
            }
        } else {
            return "null";
        }
    }

    private Set<Class<?>> findUsedClasses(Class<?> clazz) {
        Set<Class<?>> classes = new HashSet<>();
        for (Method method : getListOfMethodsToBeOverriden(clazz)) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                addUsedClasses(classes, parameterType, clazz);
            }
            addUsedClasses(classes, method.getReturnType(), clazz);
        }
        return classes;
    }

    private void addUsedClasses(Set<Class<?>> setOfUsedClasses, Class<?> candidate, Class<?> rootClass) {
        if (candidate.isArray()) {
            setOfUsedClasses.add(getArrayRootClass(candidate));
        } else if (!candidate.isPrimitive()
                && !candidate.getPackage().getName().startsWith("java.lang")
                && !candidate.getPackage().getName().equals(rootClass.getPackage().getName())) {
            setOfUsedClasses.add(candidate);
        }
    }

    private static final class MethodSignature {
        private final Method method;

        private MethodSignature(Method method) {
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            if (method == null) {
                if (that.method != null) {
                    return false;
                }
            } else {
                if (!method.getName().equals(that.method.getName())) {
                    return false;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                Class<?>[] thatParameterTypes = that.method.getParameterTypes();
                if (parameterTypes.length != thatParameterTypes.length) {
                    return false;
                }
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (!parameterTypes[i].equals(thatParameterTypes[i])) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return method != null ? method.getName().hashCode() : 0;
        }
    }

    private List<Method> getListOfMethodsToBeOverriden(Class<?> clazz) {
        HashSet<MethodSignature> methodsSignatures = new HashSet<>();
        for (Method method : clazz.getMethods()) {
            //Only for public not static and not final methods
            if (Modifier.isProtected(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                methodsSignatures.add(new MethodSignature(method));
            }
        }
        for (Method method : clazz.getMethods()) {
            //Only for protected not static and not final methods
            if (Modifier.isPublic(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                methodsSignatures.add(new MethodSignature(method));
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            //Only for package local not static and not final methods
            if (!Modifier.isProtected(method.getModifiers()) && !Modifier.isPublic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())) {
                methodsSignatures.add(new MethodSignature(method));
            }
        }
        List<Method> methods = new ArrayList<>();
        for (MethodSignature methodSignature : methodsSignatures) {
            methods.add(methodSignature.method);
        }
        return methods;
    }

    private Class<?> getArrayRootClass(Class<?> parameterType) {
        if (!parameterType.isArray()) {
            throw new IllegalArgumentException();
        }
        if (parameterType.getComponentType().isArray()) {
            return getArrayRootClass(parameterType.getComponentType());
        } else {
            return parameterType.getComponentType();
        }
    }

}

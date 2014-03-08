package ru.ifmo.ctddev.polyarnyi.task3;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/**
 * Date: 08.03.14 at 13:27
 *
 * @author Nickolay Polyarniy aka PolarNick
 */
public class Implementor {

    private static final String USAGE = "Proper Usage is: \"java Implementor [pathToSrcRoot] [className] [className]*\"\n" +
            "Where [pathToSrcRoot] - path to root of all packages for generated java sources." +
            "Where [className] - is a full path to class ot interface to be mocked.\n";
    private static final String ERROR_CLASS_NOT_FOUND_PREFIX = "Class \"";
    private static final String ERROR_CLASS_NOT_FOUND_SUFFIX = "\" not found!";
    private static final String ERROR_IO_EXCEPTION = "Input/Output error has occurred!";
    private static final String CLASS_IMPLEMENTATION_NAME_SUFFIX = "Impl";

    public static void main(String[] args) {
        if (args.length <= 1) {
            System.out.println(USAGE);
            return;
        }
        String pathToSrcRoot = args[0];
        for (int i = 1; i < args.length; i++) {
            String className = args[i];
            System.out.println("Generating implementation for " + className + "...");
            try {
                Class<?> clazz = Class.forName(className);
                Implementor implementor = new Implementor();
                String fileName = pathToSrcRoot + clazz.getPackage().getName().replace(".", "/")
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
        Map<String, Class<?>> importedClasses = findUsedClassesFromAnotherPackage(clazz);
        if (!importedClasses.values().isEmpty()) {
            appendImports(importedClasses.values(), out);
            out.append(LF);
        }
        appendClassDeclaration(clazz, implClassName, out, importedClasses);
        appendConstructor(clazz, implClassName, out);
        appendMethodsImplementations(clazz, out, importedClasses);
        out.append("}");
    }

    private void appendPackage(String packageName, Appendable out) throws IOException {
        out.append("package ").append(packageName).append(";").append(LF);
    }

    private void appendImports(Collection<Class<?>> classes, Appendable out) throws IOException {
        for (Class clazz : classes) {
            out.append("import ").append(clazz.getCanonicalName()).append(";").append(LF);
        }
    }

    private void appendClassDeclaration(Class<?> clazz, String implClassName, Appendable out, Map<String, Class<?>> importedClasses) throws IOException {
        String genericParamsPrefix = generateGenericParamsPrefix(clazz.getTypeParameters(), importedClasses);
        out.append("public class ").append(implClassName).append(genericParamsPrefix).append(" ");
        if (clazz.isAnnotation() || clazz.isAnonymousClass() || clazz.isArray() || clazz.isEnum() || clazz.isLocalClass()
                || clazz.isMemberClass() || clazz.isPrimitive() || clazz.isSynthetic()) {
            throw new IllegalArgumentException();
        } else if (Modifier.isFinal(clazz.getModifiers())) {
            throw new IllegalArgumentException("We can't mock final classes!");
        }

        if (clazz.isInterface()) {
            out.append("implements ");
        } else {
            out.append("extends ");
        }
        String genericParamsSuffix = generateGenericParamsSuffix(clazz.getTypeParameters());
        out.append(getSimpleName(clazz, importedClasses)).append(genericParamsSuffix);
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

    private void appendMethodsImplementations(Class<?> clazz, Appendable out, Map<String, Class<?>> importedClasses) throws IOException {
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
            out.append(modifier);

            String genericArgumentsPrefix = generateGenericParamsPrefix(method.getTypeParameters(), importedClasses);
            if (!genericArgumentsPrefix.isEmpty()) {
                genericArgumentsPrefix += " ";
            }
            out.append(genericArgumentsPrefix).append(toStringGenericTypedClass(method.getGenericReturnType(), importedClasses)).append(" ")
                    .append(method.getName()).append("(");

            Type[] parameterTypes = method.getGenericParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                out.append(toStringGenericTypedClass(parameterTypes[i], importedClasses)).append(" arg").append(Integer.toString(i + 1));
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

    private Map<String, Class<?>> findUsedClassesFromAnotherPackage(Class<?> rootClass) {
        Map<String, Class<?>> classes = new HashMap<>();
        for (Method method : getListOfMethodsToBeOverriden(rootClass)) {
            for (TypeVariable type : method.getTypeParameters()) {
                for (Type bound : type.getBounds()) {
                    addUsedClassesFromAnotherPackage(classes, findUsedClasses(bound, rootClass), rootClass);
                }
            }
            for (Type type : method.getGenericParameterTypes()) {
                addUsedClassesFromAnotherPackage(classes, findUsedClasses(type, rootClass), rootClass);
            }
            addUsedClassesFromAnotherPackage(classes, findUsedClasses(method.getGenericReturnType(), rootClass), rootClass);
        }
        return classes;
    }

    private void addUsedClassesFromAnotherPackage(Map<String, Class<?>> setOfUsedClasses, Map<String, Class<?>> candidates, Class<?> rootClass) {
        for (Class<?> candidate : candidates.values()) {
            addUsedClassFromAnotherPackage(setOfUsedClasses, candidate, rootClass);
        }
    }

    private void addUsedClassFromAnotherPackage(Map<String, Class<?>> setOfUsedClasses, Class<?> candidate, Class<?> rootClass) {
        if (candidate.isArray()) {
            setOfUsedClasses.put(candidate.getSimpleName(), getArrayRootClass(candidate));
        } else if (!candidate.isPrimitive()
                && !candidate.getPackage().getName().startsWith("java.lang")
                && !candidate.getPackage().getName().equals(rootClass.getPackage().getName())) {
            setOfUsedClasses.put(candidate.getSimpleName(), candidate);
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

    private Map<String, Class<?>> findUsedClasses(Type type, Class<?> rootClass) {
        Map<String, Class<?>> classes = new HashMap<>();
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> clazz = (Class<?>) ((ParameterizedType) type).getRawType();
            addUsedClassFromAnotherPackage(classes, clazz, rootClass);
            Type[] args = parameterizedType.getActualTypeArguments();
            for (Type arg : args) {
                addUsedClassesFromAnotherPackage(classes, findUsedClasses(arg, rootClass), rootClass);
            }
        } else if (type instanceof GenericArrayType) {
            addUsedClassesFromAnotherPackage(classes,
                    findUsedClasses(((GenericArrayType) type).getGenericComponentType(), rootClass), rootClass);
        } else if (type instanceof Class) {
            addUsedClassFromAnotherPackage(classes, (Class<?>) type, rootClass);
        } else if (!(type instanceof WildcardType)
                && !(type instanceof TypeVariable)) {
            throw new IllegalStateException();
        }
        return classes;
    }

    private String toStringGenericTypedClass(Type type, Map<String, Class<?>> importedClasses) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            String result = getSimpleName(((Class<?>) parameterizedType.getRawType()), importedClasses) + "<";
            Type[] args = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                result += toStringGenericTypedClass(args[i], importedClasses);
                if (i != args.length - 1) {
                    result += ", ";
                } else {
                    result += ">";
                }
            }
            return result;
        } else if (type instanceof TypeVariable) {
            return getName(((TypeVariable) type));
        } else if (type instanceof GenericArrayType) {
            return toStringGenericTypedClass(((GenericArrayType) type).getGenericComponentType(), importedClasses) + "[]";
        } else if (type instanceof Class) {
            return getSimpleName(((Class) type), importedClasses);
        } else if (type instanceof WildcardType) {
            return type.toString();
        } else {
            throw new IllegalStateException();
        }
    }

    private String generateGenericParamsPrefix(TypeVariable<?>[] genericArguments, Map<String, Class<?>> importedClasses) {
        String result = "";
        if (genericArguments.length != 0) {
            result = "<";
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result += getName(arg);
                Type[] bounds = arg.getBounds();
                if (bounds.length > 0 && !bounds[0].equals(Object.class)) {
                    result += " extends ";
                    for (int j = 0; j < bounds.length; j++) {
                        result += toStringGenericTypedClass(bounds[j], importedClasses);
                        if (j != bounds.length - 1) {
                            result += " & ";
                        }
                    }
                }
                if (i != genericArguments.length - 1) {
                    result += ", ";
                } else {
                    result += ">";
                }
            }
        }
        return result;
    }

    private String generateGenericParamsSuffix(TypeVariable<?>[] genericArguments) {
        String result = "";
        if (genericArguments.length != 0) {
            result = "<";
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result += getName(arg);
                if (i != genericArguments.length - 1) {
                    result += ", ";
                } else {
                    result += ">";
                }
            }
        }
        return result;
    }

    private String getSimpleName(Class<?> clazz, Map<String, Class<?>> importedClasses) {
        if (importedClasses.containsKey(clazz.getSimpleName())
                && !importedClasses.get(clazz.getSimpleName()).equals(clazz)) {
            return clazz.getCanonicalName();
        } else {
            return clazz.getSimpleName();
        }
    }

    private String getName(TypeVariable<?> arg) {
        return arg.getName(); //TODO: use translations!
    }

}

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
        Map<String, Map<String, String>> genericNamesTranslation = new HashMap<>();
        initGenericNamesTranslation(genericNamesTranslation, clazz);
        final String packageName = clazz.getPackage().getName();
        appendPackage(packageName, out);
        out.append(LF);
        Map<String, Class<?>> importedClasses = findUsedClassesFromAnotherPackage(clazz);
        if (!importedClasses.values().isEmpty()) {
            appendImports(importedClasses.values(), out);
            out.append(LF);
        }
        appendClassDeclaration(clazz, implClassName, out, importedClasses, genericNamesTranslation);
        appendConstructor(clazz, implClassName, out, importedClasses, genericNamesTranslation);
        appendMethodsImplementations(clazz, out, importedClasses, genericNamesTranslation);
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

    private void appendClassDeclaration(Class<?> clazz, String implClassName, Appendable out, Map<String, Class<?>> importedClasses,
                                        Map<String, Map<String, String>> genericNamesTranslation) throws IOException {
        Set<String> classGenericParams = new HashSet<>();
        for (TypeVariable var : clazz.getTypeParameters()) {
            classGenericParams.add(var.getName());
        }
        String genericParamsPrefix = generateGenericParamsPrefix(clazz.getTypeParameters(), importedClasses, genericNamesTranslation, classGenericParams);
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
        String genericParamsSuffix = generateGenericParamsSuffix(clazz.getTypeParameters(), genericNamesTranslation, classGenericParams);
        out.append(getSimpleName(clazz, importedClasses)).append(genericParamsSuffix);
        out.append(" {" + LF);
    }

    private void appendConstructor(Class<?> clazz, String classImplName, Appendable out, Map<String,
            Class<?>> importedClasses, Map<String, Map<String, String>> genericNamesTranslation) throws IOException {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        boolean defaultConstructorExists = constructors.length == 0;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0 && !Modifier.isPrivate(constructor.getModifiers())) {
                defaultConstructorExists = true;
            }
        }
        if (!defaultConstructorExists) {
            Constructor<?> superConstructor = getSuperConstructorToUse(clazz);
            if (superConstructor == null) {
                throw new IllegalArgumentException("Superclass has no accessible constructor!");
            }
            out.append(LF);
            out.append("    public ").append(classImplName).append("() ");
            Type[] genericExceptions = superConstructor.getGenericExceptionTypes();
            if (genericExceptions.length != 0) {
                out.append("throws ");
                for (int i = 0; i < genericExceptions.length; i++) {
                    Type type = genericExceptions[i];
                    out.append(toStringGenericTypedClass(type, importedClasses, genericNamesTranslation, new HashSet<String>()));
                    if (i != genericExceptions.length - 1) {
                        out.append(", ");
                    } else {
                        out.append(" ");
                    }
                }
            }
            out.append("{").append(LF);
            out.append("        super(");
            Class<?>[] parameterTypes = superConstructor.getParameterTypes();
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

    private Constructor<?> getSuperConstructorToUse(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        boolean defaultConstructorExists = constructors.length == 0;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == 0 && !Modifier.isPrivate(constructor.getModifiers())) {
                defaultConstructorExists = true;
            }
        }
        if (!defaultConstructorExists) {
            for (Constructor<?> constructor : constructors) {
                if (!Modifier.isPrivate(constructor.getModifiers())) {
                    return constructor;
                }
            }
        }
        return null;
    }

    private void appendMethodsImplementations(Class<?> clazz, Appendable out, Map<String, Class<?>> importedClasses, Map<String, Map<String, String>> genericNamesTranslation) throws IOException {
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

            Set<String> methodGenericParams = new HashSet<>();
            for (TypeVariable type : method.getTypeParameters()) {
                methodGenericParams.add(type.getName());
            }
            String genericArgumentsPrefix = generateGenericParamsPrefix(method.getTypeParameters(), importedClasses, genericNamesTranslation, methodGenericParams);
            if (!genericArgumentsPrefix.isEmpty()) {
                genericArgumentsPrefix += " ";
            }
            out.append(genericArgumentsPrefix).append(toStringGenericTypedClass(method.getGenericReturnType(), importedClasses, genericNamesTranslation, methodGenericParams)).append(" ")
                    .append(method.getName()).append("(");

            Type[] parameterTypes = method.getGenericParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                out.append(toStringGenericTypedClass(parameterTypes[i], importedClasses, genericNamesTranslation, methodGenericParams)).append(" arg").append(Integer.toString(i + 1));
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
        Constructor<?> superConstructor = getSuperConstructorToUse(rootClass);
        if (superConstructor != null) {
            for (Type exceptionType : superConstructor.getGenericExceptionTypes()) {
                addUsedClassesFromAnotherPackage(classes, findUsedClasses(exceptionType, rootClass), rootClass);
            }
        }
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
        addToSetAllOverridableMethodsFromParents(methodsSignatures, clazz, clazz.getPackage().getName());
        List<Method> methods = new ArrayList<>();
        for (MethodSignature methodSignature : methodsSignatures) {
            methods.add(methodSignature.method);
        }
        return methods;
    }

    private void addToSetAllOverridableMethodsFromParents(HashSet<MethodSignature> methodsSignatures, Class<?> clazz, String packageOfImpl) {
        for (Method method : clazz.getDeclaredMethods()) {
            //Only for package local not static and not final methods
            if (!Modifier.isProtected(method.getModifiers()) && !Modifier.isPublic(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())
                    && !Modifier.isFinal(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                    && clazz.getPackage().getName().equals(packageOfImpl)) {
                methodsSignatures.add(new MethodSignature(method));
            }
        }
        for (Class<?> parent : clazz.getInterfaces()) {
            addToSetAllOverridableMethodsFromParents(methodsSignatures, parent, packageOfImpl);
        }
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null) {
            addToSetAllOverridableMethodsFromParents(methodsSignatures, superclass, packageOfImpl);
        }
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

    private String toStringGenericTypedClass(Type type, Map<String, Class<?>> importedClasses, Map<String, Map<String, String>> genericNamesTranslation,
                                             Set<String> exclusionNames) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            String result = getSimpleName(((Class<?>) parameterizedType.getRawType()), importedClasses) + "<";
            Type[] args = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < args.length; i++) {
                result += toStringGenericTypedClass(args[i], importedClasses, genericNamesTranslation, exclusionNames);
                if (i != args.length - 1) {
                    result += ", ";
                } else {
                    result += ">";
                }
            }
            return result;
        } else if (type instanceof TypeVariable) {
            return getName(((TypeVariable) type), genericNamesTranslation, exclusionNames);
        } else if (type instanceof GenericArrayType) {
            return toStringGenericTypedClass(((GenericArrayType) type).getGenericComponentType(), importedClasses, genericNamesTranslation, exclusionNames) + "[]";
        } else if (type instanceof Class) {
            return getSimpleName(((Class) type), importedClasses);
        } else if (type instanceof WildcardType) {
            return type.toString();
        } else {
            throw new IllegalStateException();
        }
    }

    private String generateGenericParamsPrefix(TypeVariable<?>[] genericArguments, Map<String, Class<?>> importedClasses, Map<String, Map<String, String>> genericNamesTranslation,
                                               Set<String> methodGenericParams) {
        String result = "";
        if (genericArguments.length != 0) {
            result = "<";
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result += getName(arg, genericNamesTranslation, methodGenericParams);
                Type[] bounds = arg.getBounds();
                if (bounds.length > 0 && !bounds[0].equals(Object.class)) {
                    result += " extends ";
                    for (int j = 0; j < bounds.length; j++) {
                        result += toStringGenericTypedClass(bounds[j], importedClasses, genericNamesTranslation, methodGenericParams);
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

    private String generateGenericParamsSuffix(TypeVariable<?>[] genericArguments, Map<String, Map<String, String>> genericNamesTranslation,
                                               Set<String> methodGenericParams) {
        String result = "";
        if (genericArguments.length != 0) {
            result = "<";
            for (int i = 0; i < genericArguments.length; i++) {
                TypeVariable<?> arg = genericArguments[i];
                result += getName(arg, genericNamesTranslation, methodGenericParams);
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
        if (!importedClasses.containsKey(clazz.getSimpleName())
                || !importedClasses.get(clazz.getSimpleName()).equals(clazz)) {
            return clazz.getCanonicalName();
        } else {
            return clazz.getSimpleName();
        }
    }

    private String getName(TypeVariable<?> type, Map<String, Map<String, String>> genericNamesTranslation,
                           Set<String> exclusionNames) {
        if (exclusionNames != null && exclusionNames.contains(type.getName())) {
            return type.getName();
        }
        Class<?> classOfDeclaration;
        if (type.getGenericDeclaration() instanceof Method) {
            classOfDeclaration = ((Method) type.getGenericDeclaration()).getDeclaringClass();
        } else if (type.getGenericDeclaration() instanceof Class<?>) {
            classOfDeclaration = ((Class) type.getGenericDeclaration());
        } else {
            throw new IllegalStateException();
        }
        return genericNamesTranslation.get(classOfDeclaration.getCanonicalName()).get(type.getName());
    }

    private void initGenericNamesTranslation(Map<String, Map<String, String>> genericNamesTranslation, Class<?> realClass) {
        Map<String, String> passedParams = new HashMap<>();
        for (TypeVariable<?> variable : realClass.getTypeParameters()) {
            passedParams.put(variable.getName(), variable.getName());
        }
        initGenericNamesTranslation(passedParams, genericNamesTranslation, realClass);
    }

    private void initGenericNamesTranslation(Map<String, String> passedParams,
                                             Map<String, Map<String, String>> genericNamesTranslation, Class<?> clazz) {
        Map<String, String> realNameByCurName = new HashMap<>();
        for (TypeVariable<?> variable : clazz.getTypeParameters()) {
            String realValue = passedParams.get(variable.getName());
            realNameByCurName.put(variable.getName(), realValue);
        }
        genericNamesTranslation.put(clazz.getCanonicalName(), realNameByCurName);

        for (Type type : clazz.getGenericInterfaces()) {
            parseClassParent(type, genericNamesTranslation, clazz);
        }
        parseClassParent(clazz.getGenericSuperclass(), genericNamesTranslation, clazz);
    }

    private void parseClassParent(Type parent,
                                  Map<String, Map<String, String>> genericNamesTranslation, Class<?> clazz) {
        if (parent instanceof ParameterizedType) {
            Map<String, String> parentArguments = new HashMap<>();
            ParameterizedType parameterizedParent = (ParameterizedType) parent;
            Class<?> parentClass = (Class<?>) parameterizedParent.getRawType();
            Type[] actualTypeArguments = parameterizedParent.getActualTypeArguments();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                parentArguments.put(parentClass.getTypeParameters()[i].getName(), toString(actualTypeArguments[i], genericNamesTranslation, clazz));
            }
            initGenericNamesTranslation(parentArguments, genericNamesTranslation, parentClass);
        } else if (!(parent instanceof Class<?>) && parent != null) {
            throw new IllegalStateException();
        }
    }

    private String toString(Type type, Map<String, Map<String, String>> genericNamesTranslation, Class<?> clazz) {
        if (type instanceof GenericArrayType) {
            return toString(((GenericArrayType) type).getGenericComponentType(), genericNamesTranslation, clazz) + "[]";
        } else if (type instanceof ParameterizedType) {
            String result = ((Class<?>) ((ParameterizedType) type).getRawType()).getCanonicalName();
            result += "<";
            Type[] actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
            for (int i = 0; i < actualTypeArguments.length; i++) {
                Type arg = actualTypeArguments[i];
                result += toString(arg, genericNamesTranslation, clazz);
                if (i != actualTypeArguments.length - 1) {
                    result += ", ";
                } else {
                    result += ">";
                }
            }
            return result;
        } else if (type instanceof TypeVariable) {
            return genericNamesTranslation.get(clazz.getCanonicalName()).get(((TypeVariable) type).getName());
        } else if (type instanceof Class<?>) {
            return ((Class) type).getCanonicalName();
        } else {
            throw new IllegalStateException();
        }
    }

}

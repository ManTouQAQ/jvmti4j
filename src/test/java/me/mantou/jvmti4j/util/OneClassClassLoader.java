package me.mantou.jvmti4j.util;

public class OneClassClassLoader extends ClassLoader {
    private final String clazzName;
    private final byte[] clazzData;

    public OneClassClassLoader(ClassLoader parent, String clazzName, byte[] clazzData) {
        super(parent);
        this.clazzName = clazzName;
        this.clazzData = clazzData;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return loadedClass;
        }

        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            return findClass(name);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (!name.equals(clazzName)) {
            throw new ClassNotFoundException("Only can load clazz:" + clazzName);
        }
        return defineClass(name, clazzData, 0, clazzData.length);
    }
}

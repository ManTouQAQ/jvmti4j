package me.mantou.jvmti4j;

import me.mantou.jvmti4j.util.OneClassClassLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class JVMTISchedulerTest {
    private Path workDir;

    @BeforeEach
    void setUp() {
        workDir = Paths.get(System.getProperty("user.dir"));
        System.load(workDir.resolve("jvmti4j-native/cmake-build-release/jvmti4j_native.dll").toString());
    }

    @Test
    void internalNameTest() {
        assertEquals(Type.getInternalName(JVMTIScheduler.LoadHook.class), "me/mantou/jvmti4j/JVMTIScheduler$LoadHook");
    }

    @Test
    void retransformAvailableTest() {
        JVMTIScheduler.setLoadHook(
                (clazz, originalData) -> {
                    assertEquals(clazz, JVMTISchedulerTest.class);
                    return originalData;
                }
        );
        JVMTIScheduler.retransformClass(JVMTISchedulerTest.class);
    }

    @Test
    void getLoadedClassesTest() {
        assertTrue(
                Arrays.stream(JVMTIScheduler.getLoadedClasses())
                        .anyMatch(c -> c == JVMTISchedulerTest.class)
        );
    }

    @Test
    void getClassLoaderClassesTest() throws ClassNotFoundException {
        File clazzFile = workDir.resolve("build/classes/java/test/me/mantou/jvmti4j/model/TestObj.class").toFile();
        byte[] buf;
        try (FileInputStream inputStream = new FileInputStream(clazzFile)) {
            buf = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        OneClassClassLoader classLoader = new OneClassClassLoader(null, "me.mantou.jvmti4j.model.TestObj", buf);
        classLoader.loadClass("java.lang.String");
        classLoader.loadClass("me.mantou.jvmti4j.model.TestObj");

        for (Class<?> loaderClass : JVMTIScheduler.getClassLoaderClasses(classLoader)) {
            if (loaderClass.isArray()) continue;
            if (loaderClass.getClassLoader() != classLoader) continue;
            assertEquals(loaderClass.getName(), "me.mantou.jvmti4j.model.TestObj");
        }
    }

    @Test
    void errorCatchTest() {
        JVMTIScheduler.setLoadHook(
                (clazz, originalData) -> new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE}
        );

        assertThrowsExactly(JVMTIException.class, () -> JVMTIScheduler.retransformClass(JVMTISchedulerTest.class));
    }

    @Test
    void retransformTest() {
        JVMTIScheduler.setLoadHook(
                (clazz, originalData) -> {
                    if (clazz == Dog.class) {
                        ClassReader classReader = new ClassReader(originalData);
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
                        classReader.accept(new ClassVisitor(Opcodes.ASM9, classWriter) {
                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("getName")) {
                                    return new MethodVisitor(Opcodes.ASM9, mv) {
                                        @Override
                                        public void visitCode() {
                                            mv.visitLdcInsn("abc");
                                            mv.visitInsn(Opcodes.ARETURN);
                                        }
                                    };
                                }
                                return mv;
                            }
                        }, ClassReader.SKIP_FRAMES);
                        return classWriter.toByteArray();
                    }
                    return null;
                }
        );
        Dog dog = new Dog();
        assertEquals(dog.getName(), "qwe");
        JVMTIScheduler.retransformClass(Dog.class);
        assertEquals(dog.getName(), "abc");
    }

    public static abstract class Animal {
        public abstract String getName();
    }

    public static class Dog extends Animal {
        @Override
        public String getName() {
            return "qwe";
        }
    }
}

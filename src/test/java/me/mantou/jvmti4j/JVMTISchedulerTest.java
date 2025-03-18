package me.mantou.jvmti4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

import static org.junit.jupiter.api.Assertions.*;

public class JVMTISchedulerTest {

    @BeforeEach
    void setUp() {
        System.load("E:\\Personal\\IdeaProjects\\jvmti4j\\jvmti4j-native\\cmake-build-release\\jvmti4j_native.dll");
    }

    @Test
    void internalNameTest() {
        assertEquals(Type.getInternalName(JVMTIScheduler.LoadHook.class), "me/mantou/jvmti4j/JVMTIScheduler$LoadHook");
    }

    @Test
    void retransformAvailableTest() throws InterruptedException {
        JVMTIScheduler.setLoadHook(
                (clazz, originalData) -> {
                    assertEquals(clazz, JVMTISchedulerTest.class);
                    return originalData;
                }
        );
        JVMTIScheduler.retransformClass(JVMTISchedulerTest.class);
    }

    @Test
    void retransformTest(){
        JVMTIScheduler.setLoadHook(
                (clazz, originalData) -> {
                    if (clazz == Dog.class){
                        ClassReader classReader = new ClassReader(originalData);
                        ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES);
                        classReader.accept(new ClassVisitor(Opcodes.ASM9, classWriter) {
                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                                if (name.equals("getName")){
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

    public static abstract class Animal{
        public abstract String getName();
    }

    public static class Dog extends Animal{
        @Override
        public String getName() {
            return "qwe";
        }
    }
}

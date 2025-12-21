package org.globsframework.memory;

import org.objectweb.asm.*;

public class AllocationTrackingTransformer extends ClassVisitor {
    public AllocationTrackingTransformer(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodVisitor(Opcodes.ASM9, mv) {
            @Override
            public void visitTypeInsn(int opcode, String type) {
                super.visitTypeInsn(opcode, type);
                if (opcode == Opcodes.NEW) {
                    mv.visitLdcInsn(type);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/globsframework/memory/AllocationRecorderUtil", "record", "(Ljava/lang/String;)V", false);
                } else if (opcode == Opcodes.ANEWARRAY) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn(type);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/globsframework/memory/AllocationRecorderUtil", "recordArray", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                }
            }

            @Override
            public void visitIntInsn(int opcode, int operand) {
                super.visitIntInsn(opcode, operand);
                if (opcode == Opcodes.NEWARRAY) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("primitive_array_" + operand);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "org/globsframework/memory/AllocationRecorderUtil", "recordArray", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
                }
            }
        };
    }
}

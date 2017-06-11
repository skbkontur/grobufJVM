package grobuf

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal object UnrolledBinarySearchBuilder {
    fun build(methodVisitor: MethodVisitor, keys: List<Long>, valueSlot: Int,
              defaultLabel: Label, caseBuilder: MethodVisitor.(Int) -> Unit) {
        val context = EmittingContext(keys, valueSlot, defaultLabel, caseBuilder)
        methodVisitor.genBinarySearch(context, 0, keys.size - 1)
    }

    private class EmittingContext(val keys: List<Long>, val valueSlot: Int,
                                  val defaultLabel: Label, val caseBuilder: MethodVisitor.(Int) -> Unit)

    private fun MethodVisitor.genBinarySearch(context: EmittingContext, left: Int, right: Int): Unit = with(context) {
        when {
            left > right -> visitJumpInsn(Opcodes.GOTO, defaultLabel)

            right - left + 1 <= 3 -> // Bunch of ifs.
                for (index in left..right) {
                    loadSlot<Long>(valueSlot)              // stack: [value]
                    visitLdcInsn(keys[index])              // stack: [value, keys[index]]
                    val nextLabel = Label()
                    visitInsn(Opcodes.LCMP)
                    visitJumpInsn(Opcodes.IFNE, nextLabel) // if (value != keys[index]) goto next; stack: []
                    caseBuilder(index)
                    visitLabel(nextLabel)
                }

            else -> {
                val middle = (left + right) / 2
                loadSlot<Long>(valueSlot)                  // stack: [value]
                visitLdcInsn(keys[middle])                 // stack: [value, keys[middle]]
                val nextLabel = Label()
                visitInsn(Opcodes.LCMP)
                visitJumpInsn(Opcodes.IFNE, nextLabel)     // if (value != keys[middle]) goto next; stack: []
                caseBuilder(middle)
                visitLabel(nextLabel)
                loadSlot<Long>(valueSlot)                  // stack: [value]
                visitLdcInsn(keys[middle])                 // stack: [value, keys[middle]]
                val goLeftLabel = Label()
                visitInsn(Opcodes.LCMP)
                visitJumpInsn(Opcodes.IFLT, goLeftLabel)   // if (value < keys[middle]) goto goLeft; stack: []
                genBinarySearch(this, middle + 1, right)

                visitLabel(goLeftLabel)
                genBinarySearch(this, left, middle - 1)
            }
        }
    }
}
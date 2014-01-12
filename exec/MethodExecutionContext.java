package simplify.exec;

import java.util.List;
import java.util.logging.Logger;

import simplify.Simplifier;

import com.google.common.collect.LinkedListMultimap;

public class MethodExecutionContext {

    private static final Logger log = Logger.getLogger(Simplifier.class.getSimpleName());

    private final LinkedListMultimap<Integer, RegisterStore> registers;
    private final int registerCount;
    private final int parameterCount;
    private final int remainingCallDepth;
    private RegisterStore returnRegister;
    private RegisterStore methodReturnRegister;

    public MethodExecutionContext(int registerCount, int parameterCount, int remainingCallDepth) {
        registers = LinkedListMultimap.create();
        this.registerCount = registerCount;
        this.parameterCount = parameterCount;
        this.remainingCallDepth = remainingCallDepth;
    }

    public int getRemaingCallDepth() {
        return remainingCallDepth;
    }

    public int getRegisterCount() {
        return registerCount;
    }

    protected void addRegister(int register, RegisterStore rs) {
        registers.put(register, rs);
    }

    public void addParameterRegister(int parameterIndex, RegisterStore rs) {
        registers.put(getParameterStart() + parameterIndex, rs);
    }

    public void addParameterRegister(int parameterIndex, String type, Object value) {
        RegisterStore current = new RegisterStore(type, value);
        registers.put(getParameterStart() + parameterIndex, current);
    }

    public void addRegister(int register, String type, Object value, int index) {
        RegisterStore current = new RegisterStore(type, value);
        current.getReferenced().add(index);
        registers.put(register, current);
    }

    @Override
    public MethodExecutionContext clone() {
        MethodExecutionContext myClone = new MethodExecutionContext(this.registerCount, this.parameterCount,
                        this.remainingCallDepth);
        for (Integer register : registers.keySet()) {
            for (RegisterStore rs : registers.get(register)) {
                myClone.addRegister(register, rs.clone());
            }
        }

        if (methodReturnRegister != null) {
            myClone.methodReturnRegister = this.methodReturnRegister.clone();
        }

        if (returnRegister != null) {
            myClone.returnRegister = this.returnRegister.clone();
        }

        return myClone;
    }

    public int getParameterStart() {
        return registerCount - parameterCount;
    }

    public RegisterStore getRegister(int register, int index) {
        List<RegisterStore> historical = registers.get(register);

        if (historical.size() == 0) {
            return null;
        }

        RegisterStore current = historical.get(historical.size() - 1);
        current.getReferenced().add(index);

        return current;
    }

    public String peekRegisterType(int register) {
        List<RegisterStore> historical = registers.get(register);
        RegisterStore current = historical.get(historical.size() - 1);

        return current.getType();
    }

    public Object peekRegisterValue(int register) {
        List<RegisterStore> historical = registers.get(register);
        RegisterStore current = historical.get(historical.size() - 1);

        return current.getValue();
    }

    public Object getRegisterValue(int register, int index) {
        RegisterStore current = getRegister(register, index);
        current.getUsed().add(index);
        return current.getValue();
    }

    public RegisterStore getReturnRegister() {
        return returnRegister;
    }

    public void setMethodReturnRegister(RegisterStore rs) {
        methodReturnRegister = new RegisterStore(rs.getType(), rs.getValue());
    }

    public RegisterStore getMethodReturnRegister() {
        RegisterStore methodReturn = methodReturnRegister;
        methodReturnRegister = null;

        return methodReturn;
    }

    public void setReturnRegister(int register, int index) {
        returnRegister = getRegister(register, index);
        returnRegister.getUsed().add(index);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Integer key : registers.keySet()) {
            sb.append("[");
            for (RegisterStore register : registers.get(key)) {
                sb.append("r").append(key).append(": ").append(register).append(",\n");
            }
            sb.delete(sb.length() - 2, sb.length()).append("]\n");

            if (methodReturnRegister != null) {
                sb.append("methodReturn").append(": ").append(methodReturnRegister).append("\n");
            }

            if (returnRegister != null) {
                sb.append("return").append(": ").append(returnRegister).append("\n");
            }
        }

        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    public void updateOrAddRegister(int register, String type, Object value, int index) {
        // "update" means type is unchanged
        RegisterStore current = getRegister(register, index);
        if (current == null) {
            addRegister(register, type, value, index);
            current = getRegister(register, index);
        }
        current.setValue(value);
    }
}
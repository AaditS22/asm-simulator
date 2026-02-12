package io.github.AaditS22.asmsimulator.backend.util;

public enum RegisterInfo {
    // NOTE: LLMs were used to automate creating the values for each sub-register, as it is a tedious process manually

    RAX("rax", 8),
    EAX("rax", 4),
    AX("rax", 2),
    AL("rax", 1),

    RDI("rdi", 8),
    EDI("rdi", 4),
    DI("rdi", 2),
    DIL("rdi", 1),

    RSI("rsi", 8),
    ESI("rsi", 4),
    SI("rsi", 2),
    SIL("rsi", 1),

    RDX("rdx", 8),
    EDX("rdx", 4),
    DX("rdx", 2),
    DL("rdx", 1),

    RCX("rcx", 8),
    ECX("rcx", 4),
    CX("rcx", 2),
    CL("rcx", 1),

    R8("r8", 8),
    R8D("r8", 4),
    R8W("r8", 2),
    R8B("r8", 1),

    R9("r9", 8),
    R9D("r9", 4),
    R9W("r9", 2),
    R9B("r9", 1),

    R10("r10", 8),
    R10D("r10", 4),
    R10W("r10", 2),
    R10B("r10", 1),

    R11("r11", 8),
    R11D("r11", 4),
    R11W("r11", 2),
    R11B("r11", 1),

    RSP("rsp", 8),
    ESP("rsp", 4),
    SP("rsp", 2),
    SPL("rsp", 1),

    RBX("rbx", 8),
    EBX("rbx", 4),
    BX("rbx", 2),
    BL("rbx", 1),

    RBP("rbp", 8),
    EBP("rbp", 4),
    BP("rbp", 2),
    BPL("rbp", 1),

    R12("r12", 8),
    R12D("r12", 4),
    R12W("r12", 2),
    R12B("r12", 1),

    R13("r13", 8),
    R13D("r13", 4),
    R13W("r13", 2),
    R13B("r13", 1),

    R14("r14", 8),
    R14D("r14", 4),
    R14W("r14", 2),
    R14B("r14", 1),

    R15("r15", 8),
    R15D("r15", 4),
    R15W("r15", 2),
    R15B("r15", 1);

    private final String baseRegister;
    private final int numBytes;

    RegisterInfo(String baseRegister, int numBytes) {
        this.baseRegister = baseRegister;
        this.numBytes = numBytes;
    }

    public String getBaseRegister() { return baseRegister; }
    public int getNumBytes() { return numBytes; }
}

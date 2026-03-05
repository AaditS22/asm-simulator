## Languages
The application currently only supports x86-64 AT&T assembly, although Intel syntax is a planned addition

## Instructions
#### Movement
- MOV
- MOVZB
- MOVZW
- PUSH
- POP
- LEA
#### Arithmetic
- ADD
- INC
- DEC
- SUB
- MUL/IMUL
- DIV/IDIV
#### Branching
- CALL (special C functions include printf, scanf, exit)
- JE/JNE/JG/JGE/JL/JLE
- JMP
- RET
- LOOP
#### Logical
- AND
- CMP
- NEG
- NOT
- OR
- XOR
- SHL/SHR
- TEST
- NOP
#### SYSCALL
- Read (syscall 0)
- Write (syscall 1)
- Exit (syscall 60)
- Group exit (syscall 231)
## Data
#### Sections
- TEXT
- DATA
- BSS
- RODATA

#### Directives
- .byte/.word/.long/.quad
- .ascii
- .asciz/.string
- .space/.skip/.zero
- .align

Support for the .include directive (and multiple files, along with syscalls to read/write from files) is the next planned addition.

# Assembly x86-64 Visualizer

---

### Overview
This application will be used to help learners and professionals alike code
in assembly (AT&T syntax). It will allow users to input their assembly code, then run it step-by-step
and visualize how registers, the stack, and relevant static memory is changing with each instruction. This will be significantly more
user-friendly than the **GNU debugger**, commonly used to debug assembly code, and can thus
also aid in debugging.

---

### Planned Features


**Core**:
* Ability to input custom code (as file or text)
* Support for the most common instructions (ADD/SUB/MOV/etc)
* Simulation of user's code, allowing instruction-by-instruction execution
* Visualization of the state of registers, stack, and relevant memory addresses
---
**Additions**:
* Custom messages for each executed instruction detailing the operation in an understandable way (for teaching)
* Ability to view the state of certain flags (ZF/CF/etc)
* Support for printf and scanf (with custom IO simulator)
* Support for instructions without size suffixes when applicable
---

### Target Users

* Undergraduate students taking Computer Organization / Systems courses
* Learners new to x86-64 assembly
* Professional/recreational assembly coders looking for a more intuitive debugger

---

### Code Requirements
* Java backend & JavaFX frontend
* Code adheres to custom checkstyle configuration
* Important features validated with unit testing

---
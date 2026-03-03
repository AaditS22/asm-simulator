# asm-simulator
*An application to visualize and debug Assembly x86-64 AT&amp;T code.*

This application was created to help students (and professionals) learn and debug Assembly code. Current tools are often lacking in certain aspects (such as ability to view the entire CPU state at once in a simplified format), and this aims to solve those issues, making a very complicated language easier to understand.

## Features
- **Simulator**: Ability to simulate custom code (line-by-line, direct run to the end, or autoplayed with a custom delay)
- **Visualization**: View exactly how different parts of the CPU (such as memory, flags, and registers) are changing with color highlighting, drawing attention to the exact changes
- **Editor**: Simple personalized editor that applies color formatting to AT&T code, saving entered code across sessions
- **Local/Web Versions**: Two versions of the application, one for the web and one for desktop. Both have the same features, but this allows a more balanced spread of users and reduced server load on the web version
- **Language Support**: Supports most instructions and data directives for x86-64 AT&T Assembly. A list of supported instructions (and planned additions) can be found at SUPPORT.md

## Tech Stack
- **Backend**: Plain Java (core CPU emulation logic) & Spring-boot (REST API to communicate with frontend)
- **Web Frontend**: React, Vite, Tailwind CSS
- **Desktop Frontend**: JavaFX & RichTextFX (for editor styling)
- **DevOps**: Github Actions (to create automated local build for Windows/Mac/Linux)
- **Testing**: Backend logic tested using thorough JUnit tests

## Installation & Usage
Prerequisites: Java 17, JavaFX 21, Node.js v18+, Maven <br>
Clone the project using:
```bash
git clone https://github.com/AaditS22/asm-simulator.git
```
**Running the web version** <br>
Run the backend:
```bash
mvn spring-boot:run
```
Run the frontend:
```bash
cd web
npm install
npm run dev
```
**Running the local version**
```bash
mvn javafx:run
```

## Architecture
The application follows MVC protocols, properly separating UI and business logic.

**Simulation Engine**:
The main simulation works using a custom simplified CPU emulator. The state of the CPU is held in different Java objects, and each instruction mutates the state based on how it is meant to. User input is parsed by performing a first-pass to collect labels, and a second-pass to write them to memory and collect instructions. The instructions are then fed into a "Simulator" object, which is the main connection to the frontend.

**Web & API**:
The React frontend communicates to the Spring backend by making RESTful API calls. It is stateless in the sense that each request can be handled by the server without any knowledge of previous requests. An example API call is a POST to /step, which runs a step of the instruction and alters the state of the CPU based on the isntruction. The frontend, in return, receives the updated state along with other useful information (such as potential output to the terminal) encoded in JSON format.

**User Concurrency**:
Although it is stateless, users are required to pass a "session-id" header in each request. The server holds a LinkedHashMap of session-ids and their respective Simulator objects, and uses an LRU cache algorithm to replace the oldest inactive user when the max limit (1000 concurrent sessions) is reached.

## Other Information
- **License**: The project is open-source, under an MIT license (see LICENSE for more info)
- **AI Disclaimer**: AI was used in certain parts of the project, most heavily for the UI (both local and web) & unit tests. All files that have had AI contributions are tagged with comments in the source code, some with information about how/why it was used.
- **Privacy Notice**: Vercel Web Analytics is used to collect data about the number of users and the country/device/OS of users. All data is completely anonymous. 

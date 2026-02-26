import React, { useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import HomeView from './components/home/HomeView'
import EditorView from './components/editor/EditorView'
import SimulatorView from './components/simulator/SimulatorView'

const PLACEHOLDER =
    '# Example code\n' +
    '# This moves 0 to %rdi, signalling a successful exit\n' +
    '# Then calls the exit function to quit the program\n' +
    '.text\n' +
    '.global main\n' +
    'main:\n' +
    '  movq $0, %rdi\n' +
    '  call exit\n'

export default function App() {
    const [code, setCode] = useState(PLACEHOLDER)

    return (
        <BrowserRouter>
            <Routes>
                <Route path="/"          element={<HomeView />} />
                <Route path="/editor"    element={<EditorView code={code} onCodeChange={setCode} />} />
                <Route path="/simulator" element={<SimulatorView code={code} />} />
                <Route path="*"          element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    )
}
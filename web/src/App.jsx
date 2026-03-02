import React, { useState, useCallback } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import HomeView from './components/home/HomeView'
import EditorView from './components/editor/EditorView'
import SimulatorView from './components/simulator/SimulatorView'
import ServerDownView from './components/shared/ServerDownView'

const STORAGE_KEY = 'asm_sim_code'

const PLACEHOLDER =
    '# Example code\n' +
    '# This moves 0 to %rdi, signalling a successful exit\n' +
    '# Then calls the exit function to quit the program\n' +
    '.text\n' +
    '.global main\n' +
    'main:\n' +
    '  movq $0, %rdi\n' +
    '  call exit\n'

function loadCode() {
    try {
        return localStorage.getItem(STORAGE_KEY) ?? PLACEHOLDER
    } catch {
        return PLACEHOLDER
    }
}

function saveCode(code) {
    try {
        localStorage.setItem(STORAGE_KEY, code)
    } catch {}
}

export default function App() {
    const [code, setCode] = useState(loadCode)
    const [routerKey, setRouterKey] = useState(0)

    const handleCodeChange = useCallback((newCode) => {
        setCode(newCode)
        saveCode(newCode)
    }, [])

    const forceNavigate = useCallback((path) => {
        window.history.replaceState(null, '', path)
        setRouterKey(k => k + 1)
    }, [])

    return (
        <BrowserRouter key={routerKey}>
            <Routes>
                <Route path="/"          element={<HomeView />} />
                <Route path="/editor"    element={<EditorView code={code} onCodeChange={handleCodeChange} />} />
                <Route path="/simulator" element={<SimulatorView code={code} forceNavigate={forceNavigate} />} />
                <Route path="*"          element={<Navigate to="/" replace />} />
                <Route path="/server-down" element={<ServerDownView />} />
            </Routes>
        </BrowserRouter>
    )
}
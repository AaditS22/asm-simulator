import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import HomeView from './components/home/HomeView'
import EditorView from './components/editor/EditorView'
import SimulatorView from './components/simulator/SimulatorView'

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/"          element={<HomeView />} />
                <Route path="/editor"    element={<EditorView />} />
                <Route path="/simulator" element={<SimulatorView />} />
                <Route path="*"          element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    )
}
import React, { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import TopBar from '../shared/TopBar'
import PaneCard from '../shared/PaneCard'
import StackPanel from './StackPanel'
import RegistersPanel from './RegistersPanel'
import FlagsPanel from './FlagsPanel'
import MemoryPanel from './MemoryPanel'

// DISCLAIMER: This component was largely written with the help of LLMs
const MNEMONICS =
    'mov[a-z]*|add[a-z]*|sub[a-z]*|imul[a-z]*|mul[a-z]*|idiv[a-z]*|div[a-z]*|' +
    'and[a-z]*|or[a-z]*|xor[a-z]*|not[a-z]*|neg[a-z]*|cmp[a-z]*|test[a-z]*|' +
    'shl[a-z]*|shr[a-z]*|sar[a-z]*|sal[a-z]*|lea[a-z]*|' +
    'push[a-z]*|pop[a-z]*|inc[a-z]*|dec[a-z]*|' +
    'jmp|je|jne|jg|jge|jl|jle|ja|jae|jb|jbe|jz|jnz|js|jns|jo|jno|jc|jnc|' +
    'call|ret|nop|cdq|cqto|cltd|cltq|syscall|int|hlt|leave|enter|' +
    'movzb[a-z]*|movzw[a-z]*|movs[a-z]*'

const ASM_PATTERN = new RegExp(
    '(?<COMMENT>#[^\\n]*)' +
    '|(?<STRING>"(?:[^"\\\\]|\\\\.)*")' +
    '|(?<SECTION>\\.(?:text|data|bss|rodata)(?=\\s|$))' +
    '|(?<DIRECTIVE>\\.(?:quad|long|word|byte|ascii|asciz|string|skip|zero|globl|global|equ|set|align|comm|lcomm|fill|space|type|size|section)(?=\\s|$|,))' +
    '|(?<LABEL>^[ \\t]*[A-Za-z_.][A-Za-z0-9_.]*:)' +
    '|(?<MNEMONIC>(?<![A-Za-z0-9_.])(?:' + MNEMONICS + ')(?=[ \\t,\\n#]|$))' +
    '|(?<REGISTER>%[a-zA-Z0-9]+)' +
    '|(?<IMMEDIATE>\\$(?:0x[0-9a-fA-F]+|-?[0-9]+|0b[01]+))',
    'gm'
)

// Matches EditorView's custom theme perfectly
const TOKEN_STYLES = {
    COMMENT:   { color: '#6A8759' },
    STRING:    { color: '#6A8759' },
    SECTION:   { color: '#C792EA', fontWeight: 'bold' },
    DIRECTIVE: { color: '#F78C6C' },
    LABEL:     { color: '#C792EA', fontWeight: 'bold' },
    MNEMONIC:  { color: '#E8A845', fontWeight: 'bold' },
    REGISTER:  { color: '#56A6E8' },
    IMMEDIATE: { color: '#A8D880' },
}

function highlightLine(text) {
    if (!text) return [{ text: ' ', style: { color: '#BBBBBB' } }]
    const tokens = []
    let lastEnd = 0
    ASM_PATTERN.lastIndex = 0
    let match
    while ((match = ASM_PATTERN.exec(text)) !== null) {
        if (match.index > lastEnd) {
            tokens.push({ text: text.slice(lastEnd, match.index), style: { color: '#BBBBBB' } })
        }
        const groups = match.groups
        let style = { color: '#BBBBBB' }
        for (const key of Object.keys(TOKEN_STYLES)) {
            if (groups[key] != null) { style = TOKEN_STYLES[key]; break }
        }
        tokens.push({ text: match[0], style })
        lastEnd = match.index + match[0].length
    }
    if (lastEnd < text.length) tokens.push({ text: text.slice(lastEnd), style: { color: '#BBBBBB' } })
    if (tokens.length === 0) tokens.push({ text: ' ', style: { color: '#BBBBBB' } })
    return tokens
}

function ErrorModal({ onClose }) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
            <div
                className="flex flex-col items-center gap-5 px-8 py-7 bg-bg-base border border-border-soft rounded"
                style={{ boxShadow: '0 8px 32px rgba(0,0,0,0.6)', maxWidth: 360 }}
            >
                <span className="font-sans text-[14px] font-bold" style={{ color: '#E06C75' }}>Parse Error</span>
                <span className="font-sans text-[12px] text-text-bright text-center leading-relaxed">
                    There was an error parsing your code.
                    Check the terminal for the full error message to see what went wrong
                    and go back to the editor to fix it.
                </span>
                <button
                    onClick={onClose}
                    className="px-6 py-1.5 rounded font-sans text-[12px] text-text-bright bg-bg-raised border border-border-soft hover:bg-bg-hover transition-colors"
                >
                    OK
                </button>
            </div>
        </div>
    )
}

export default function SimulatorView({ code, forceNavigate }) {
    const navigate = useNavigate()
    const codeScrollRef = useRef(null)
    const terminalScrollRef = useRef(null)
    const terminalInputRef = useRef(null)
    const autoPlayRef = useRef(null)
    const speedRef = useRef(250)
    const containerRef = useRef(null)
    const isSteppingRef = useRef(false)

    const [state, setState] = useState(null)
    const [parseSuccess, setParseSuccess] = useState(false)
    const [showParseError, setShowParseError] = useState(false)
    const [terminalLines, setTerminalLines] = useState([{ text: 'No program running.', color: '#777777', arrow: false }])
    const [stepCount, setStepCount] = useState(0)
    const [instrDesc, setInstrDesc] = useState(null)
    const [instrMnemonic, setInstrMnemonic] = useState(null)
    const [highlightedLine, setHighlightedLine] = useState(-1)
    const [controlsEnabled, setControlsEnabled] = useState(true)
    const [isAutoPlaying, setIsAutoPlaying] = useState(false)
    const [terminalInputActive, setTerminalInputActive] = useState(false)
    const [terminalInputValue, setTerminalInputValue] = useState('')
    const [terminalHeight, setTerminalHeight] = useState(175)
    const [speed, setSpeed] = useState(250)
    const [resizerHover, setResizerHover] = useState(false)

    const codeLines = (code || '').split(/\r?\n/)

    useEffect(() => {
        window.__setSimState = setState
        return () => { delete window.__setSimState }
    }, [])

    useEffect(() => {
        if (!code || code.trim() === '') return
        api.load(code).then(res => {
            if (res.success) {
                setParseSuccess(true)
                setState(res.state)
                setTerminalLines([{ text: 'Parsed successfully. Ready to simulate.', color: '#4EC94E', arrow: false }])
                highlightInstruction(res.state)
            }
        }).catch(err => {
            setParseSuccess(false)
            setControlsEnabled(false)
            let msg = 'Parse error'
            try { msg = JSON.parse(err.message)?.error || err.message } catch { msg = err.message }
            setTerminalLines([{ text: msg, color: '#FF9B94', arrow: false }])
            setShowParseError(true)
        })
        return () => { if (autoPlayRef.current) clearInterval(autoPlayRef.current) }
    }, [])

    const highlightInstruction = useCallback((s) => {
        if (!s || s.currentLineNumber < 0) {
            setHighlightedLine(-1)
            return
        }
        setHighlightedLine(s.currentLineNumber)
        scrollToLine(s.currentLineNumber)
    }, [])

    function scrollToLine(lineIndex) {
        if (!codeScrollRef.current) return
        const rowHeight = 22
        const container = codeScrollRef.current
        const viewportHeight = container.clientHeight
        const totalHeight = codeLines.length * rowHeight
        if (totalHeight <= viewportHeight) return
        const rowCenterY = lineIndex * rowHeight + rowHeight / 2
        let idealTop = rowCenterY - viewportHeight / 2
        idealTop = Math.max(0, Math.min(idealTop, totalHeight - viewportHeight))
        container.scrollTop = idealTop
    }

    function scrollTerminalToBottom() {
        requestAnimationFrame(() => {
            if (terminalScrollRef.current) {
                terminalScrollRef.current.scrollTop = terminalScrollRef.current.scrollHeight
            }
        })
    }

    function addTerminalLine(text, color, arrow = false) {
        setTerminalLines(prev => [...prev, { text, color, arrow }])
        scrollTerminalToBottom()
    }

    function appendTerminalOutput(text, color) {
        if (!text) return
        const parts = String(text).split(/\r?\n/).filter(p => p.length > 0)
        setTerminalLines(prev => [...prev, ...parts.map(p => ({ text: p, color, arrow: false }))])
        scrollTerminalToBottom()
    }

    async function handleStep() {
        if (!parseSuccess) return
        try {
            const res = await api.step()
            if (res.error) {
                if (res.error.includes('halted')) return
                appendTerminalOutput('\nError: ' + res.error, '#FF9B94')
                setHighlightedLine(-1)
                setControlsEnabled(false)
                stopAutoPlay()
                return
            }
            setStepCount(prev => prev + 1)
            setState(res.state)
            setInstrDesc(res.description)
            setInstrMnemonic(res.mnemonic ? res.mnemonic.toUpperCase() : null)

            if (res.hasOutput && res.output) {
                appendTerminalOutput(res.output, '#D4D4D4')
            }

            if (res.state?.status === 'HALTED') {
                handleHalt(res.state)
                return
            }
            if (res.state?.status === 'WAITING_FOR_INPUT') {
                stopAutoPlay()
                setControlsEnabled(false)
                activateTerminalInput()
                return
            }
            highlightInstruction(res.state)
        } catch (err) {
            stopAutoPlay()
            let msg = err.message
            try { msg = JSON.parse(err.message)?.error || err.message } catch {}
            appendTerminalOutput('\nError: ' + msg, '#FF9B94')
            setHighlightedLine(-1)
            setControlsEnabled(false)
        }
    }

    function handleHalt(s) {
        stopAutoPlay()
        setControlsEnabled(false)
        setHighlightedLine(-1)
        api.getState().then(fullState => {
            const code = fullState?.registers?.rdi ?? 0
            const color = code === 0 ? '#4EC94E' : '#FF9B94'
            addTerminalLine(`[Program exited with code ${code}]`, color)
        }).catch(() => {
            addTerminalLine('[Program halted]', '#4EC94E')
        })
    }

    async function handleReset() {
        stopAutoPlay()
        if (!parseSuccess) return
        try {
            const res = await api.reset()
            setState(res)
            setStepCount(0)
            setInstrDesc(null)
            setInstrMnemonic(null)
            setTerminalLines([{ text: 'Reset. Ready to simulate.', color: '#4EC94E', arrow: false }])
            setControlsEnabled(true)
            setTerminalInputActive(false)
            setTerminalInputValue('')
            highlightInstruction(res)
        } catch (err) {
            appendTerminalOutput('\nReset error: ' + err.message, '#FF9B94')
        }
    }

    function toggleAutoPlay() {
        if (isAutoPlaying) {
            stopAutoPlay()
        } else {
            if (!parseSuccess) return
            if (state?.status === 'HALTED' || state?.status === 'WAITING_FOR_INPUT') return
            setIsAutoPlaying(true)
            setControlsEnabled(false)

            autoPlayRef.current = setInterval(async () => {
                if (isSteppingRef.current) return
                isSteppingRef.current = true
                try {
                    const res = await api.step()
                    if (res.error) {
                        if (res.error.includes('halted')) {
                            stopAutoPlay()
                            isSteppingRef.current = false
                            return
                        }
                        appendTerminalOutput('\nError: ' + res.error, '#FF9B94')
                        setHighlightedLine(-1)
                        stopAutoPlay()
                        setControlsEnabled(false)
                        isSteppingRef.current = false
                        return
                    }
                    setStepCount(prev => prev + 1)
                    setState(res.state)
                    setInstrDesc(res.description)
                    setInstrMnemonic(res.mnemonic ? res.mnemonic.toUpperCase() : null)

                    if (res.hasOutput && res.output) {
                        appendTerminalOutput(res.output, '#D4D4D4')
                    }

                    if (res.state?.status === 'HALTED') {
                        handleHalt(res.state)
                        isSteppingRef.current = false
                        return
                    }
                    if (res.state?.status === 'WAITING_FOR_INPUT') {
                        stopAutoPlay()
                        setControlsEnabled(false)
                        activateTerminalInput()
                        isSteppingRef.current = false
                        return
                    }
                    highlightInstruction(res.state)
                } catch (err) {
                    stopAutoPlay()
                    let msg = err.message
                    try { msg = JSON.parse(err.message)?.error || err.message } catch {}
                    if (msg.toLowerCase().includes('halted')) {
                        handleHalt(state)
                    } else {
                        appendTerminalOutput('\nError: ' + msg, '#FF9B94')
                        setHighlightedLine(-1)
                        setControlsEnabled(false)
                    }
                }
                isSteppingRef.current = false
            }, speedRef.current)
        }
    }

    function stopAutoPlay() {
        if (autoPlayRef.current) {
            clearInterval(autoPlayRef.current)
            autoPlayRef.current = null
        }
        isSteppingRef.current = false
        setIsAutoPlaying(false)
        setControlsEnabled(true)
    }

    async function handleRunToEnd() {
        if (!parseSuccess) return
        if (state?.status === 'HALTED' || state?.status === 'WAITING_FOR_INPUT') return
        stopAutoPlay()
        setControlsEnabled(false)
        try {
            const res = await api.run()
            if (res.error && !res.state) {
                appendTerminalOutput('\nError: ' + res.error, '#FF9B94')
                setControlsEnabled(false)
                return
            }
            setState(res.state)
            setStepCount(res.state?.stepCount ?? 0)
            if (res.lastDescription) {
                setInstrDesc(res.lastDescription)
                setInstrMnemonic(res.lastMnemonic ? res.lastMnemonic.toUpperCase() : null)
            }
            if (res.output) appendTerminalOutput(res.output, '#D4D4D4')

            if (res.state?.status === 'HALTED') {
                const exitCode = res.exitCode ?? 0
                const color = exitCode === 0 ? '#4EC94E' : '#FF9B94'
                addTerminalLine(`[Program exited with code ${exitCode}]`, color)
                setHighlightedLine(-1)
                setControlsEnabled(false)
            } else if (res.state?.status === 'WAITING_FOR_INPUT') {
                activateTerminalInput()
            } else {
                highlightInstruction(res.state)
                setControlsEnabled(true)
            }
        } catch (err) {
            let msg = err.message
            try { msg = JSON.parse(err.message)?.error || err.message } catch {}
            appendTerminalOutput('\nError: ' + msg, '#FF9B94')
            setControlsEnabled(false)
        }
    }

    function activateTerminalInput() {
        setTerminalInputActive(true)
        setTerminalInputValue('')
        setTimeout(() => terminalInputRef.current?.focus(), 50)
    }

    async function handleTerminalSubmit() {
        const value = terminalInputValue
        addTerminalLine(value, '#D4D4D4', true)
        setTerminalInputActive(false)
        setTerminalInputValue('')
        try {
            const res = await api.input(value)
            if (res.error) {
                appendTerminalOutput('\n[Input error] ' + res.error, '#FF9B94')
                return
            }
            setStepCount(prev => prev + 1)
            setState(res.state)
            setInstrDesc(res.description)
            setInstrMnemonic(res.mnemonic ? res.mnemonic.toUpperCase() : null)
            if (res.hasOutput && res.output) appendTerminalOutput(res.output, '#D4D4D4')

            if (res.state?.status === 'HALTED') {
                handleHalt(res.state)
                return
            }
            if (res.state?.status === 'WAITING_FOR_INPUT') {
                activateTerminalInput()
                return
            }
            setControlsEnabled(true)
            highlightInstruction(res.state)
        } catch (err) {
            appendTerminalOutput('\n[Input error] ' + err.message, '#FF9B94')
        }
    }

    function handleSpeedChange(e) {
        const val = Number(e.target.value)
        setSpeed(val)
        speedRef.current = val
        if (isAutoPlaying && autoPlayRef.current) {
            clearInterval(autoPlayRef.current)
            autoPlayRef.current = setInterval(() => handleStep(), val)
        }
    }

    function handleResizeDrag(e) {
        if (!containerRef.current) return
        const containerRect = containerRef.current.getBoundingClientRect()
        let newHeight = containerRect.bottom - e.clientY
        if (newHeight < 145) newHeight = 145
        const maxAllowed = containerRect.height - 250
        if (newHeight > maxAllowed) newHeight = maxAllowed
        setTerminalHeight(newHeight)
    }

    const hasDescription = instrDesc && instrDesc.trim()

    return (
        <div ref={containerRef} className="flex flex-col w-full h-full bg-bg-base">
            {showParseError && <ErrorModal onClose={() => setShowParseError(false)} />}

            {/* Title Bar */}
            <TopBar
                left={
                    <div className="flex items-center">
                        <span className="font-sans text-[12px] font-bold text-amber">ASM SIM</span>
                        <span className="font-sans text-[12px] mx-2" style={{ color: '#777777' }}>/</span>
                        <span className="font-sans text-[12px] text-text-muted">Simulator</span>
                    </div>
                }
                right={
                    <button
                        onClick={() => {
                            if (autoPlayRef.current) {
                                clearInterval(autoPlayRef.current)
                                autoPlayRef.current = null
                            }
                            forceNavigate('/editor')
                        }}
                        className="font-sans text-[11.5px] text-text-primary bg-bg-raised px-3 py-1 rounded hover:bg-bg-hover hover:text-text-bright transition-colors"
                    >
                        ← Editor
                    </button>
                }
            />

            {/* Control Bar */}
            <div className="flex items-center gap-3 px-4 py-2 border-b border-border-soft" style={{ backgroundColor: '#1E1F22' }}>
                <button
                    onClick={handleReset}
                    className="font-sans text-[12px] font-bold px-3 py-1.5 rounded transition-colors"
                    style={{ backgroundColor: '#3C3F41', color: '#BBBBBB', border: '1px solid #424547' }}
                    onMouseEnter={e => { e.currentTarget.style.backgroundColor = '#4C5052'; e.currentTarget.style.color = '#E8E8E8' }}
                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = '#3C3F41'; e.currentTarget.style.color = '#BBBBBB' }}
                >
                    ⟳ Restart
                </button>

                <button
                    disabled={!controlsEnabled}
                    onClick={handleStep}
                    className="font-sans text-[12px] font-bold px-4 py-1.5 rounded transition-colors disabled:opacity-50"
                    style={{ backgroundColor: '#E8A845', color: '#1E1E1E' }}
                    onMouseEnter={e => { if (!e.currentTarget.disabled) e.currentTarget.style.backgroundColor = '#F5BE6A' }}
                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = '#E8A845' }}
                >
                    Step ▶
                </button>

                <div className="flex items-center gap-2">
                    <button
                        disabled={!controlsEnabled && !isAutoPlaying}
                        onClick={toggleAutoPlay}
                        className="font-sans text-[12px] font-bold px-3 py-1.5 rounded transition-colors disabled:opacity-50"
                        style={{
                            backgroundColor: isAutoPlaying ? '#E8A845' : '#3C3F41',
                            color: isAutoPlaying ? '#1E1E1E' : '#BBBBBB',
                            border: '1px solid #424547',
                        }}
                        onMouseEnter={e => { if (!e.currentTarget.disabled) e.currentTarget.style.backgroundColor = isAutoPlaying ? '#F5BE6A' : '#4C5052' }}
                        onMouseLeave={e => { e.currentTarget.style.backgroundColor = isAutoPlaying ? '#E8A845' : '#3C3F41' }}
                    >
                        {isAutoPlaying ? 'Pause ⏸' : 'Autoplay ▶▶'}
                    </button>
                    <span className="font-sans text-[12px] text-text-muted">Delay:</span>
                    <input
                        type="range" min={10} max={1000} value={speed}
                        onChange={handleSpeedChange}
                        className="w-[100px]"
                        style={{ accentColor: '#E8A845' }}
                    />
                </div>

                <button
                    disabled={!controlsEnabled}
                    onClick={handleRunToEnd}
                    className="font-sans text-[12px] font-bold px-3 py-1.5 rounded transition-colors disabled:opacity-50"
                    style={{ backgroundColor: '#3C3F41', color: '#BBBBBB', border: '1px solid #424547' }}
                    onMouseEnter={e => { if (!e.currentTarget.disabled) { e.currentTarget.style.backgroundColor = '#4C5052'; e.currentTarget.style.color = '#E8E8E8' } }}
                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = '#3C3F41'; e.currentTarget.style.color = '#BBBBBB' }}
                >
                    Run to End ⏭
                </button>

                <div className="flex-1" />
                <span className="font-mono text-[11.5px] text-text-muted px-1">
                    step&nbsp;&nbsp;{state?.stepCount ?? stepCount}
                </span>
            </div>

            {/* Main Content */}
            <div className="flex flex-1 min-h-0" style={{ backgroundColor: '#2B2B2B' }}>
                {/* Code Pane (Left) */}
                <div className="flex flex-col h-full" style={{ minWidth: 330, width: 420, maxWidth: 560, backgroundColor: '#1E1F22' }}>
                    {/* Code Header */}
                    <div
                        className="flex items-center px-3.5 border-b border-border-soft"
                        style={{ height: 36, minHeight: 36, backgroundColor: '#313335' }}
                    >
                        <span className="font-sans text-[11.5px] font-bold text-text-primary">Source Code</span>
                        <div className="flex-1" />
                        <span
                            className="font-sans text-[9.5px] font-bold text-text-muted px-[7px] py-[2px] rounded"
                            style={{ backgroundColor: '#3C3F41', letterSpacing: '0.5px' }}
                        >
                            READ ONLY / Go back to the editor to edit code
                        </span>
                    </div>

                    {/* Code Lines */}
                    <div ref={codeScrollRef} className="flex-1 overflow-auto" style={{ backgroundColor: '#1E1F22' }}>
                        <div style={{ backgroundColor: '#1E1F22' }}>
                            {codeLines.map((line, i) => (
                                <div
                                    key={i}
                                    className="flex items-center"
                                    style={{
                                        minHeight: 22, height: 22,
                                        backgroundColor: highlightedLine === i ? '#214283' : '#1E1F22',
                                    }}
                                >
                                    <span
                                        className="font-mono text-[13px] text-right shrink-0"
                                        style={{
                                            width: 48, minWidth: 48,
                                            padding: '1px 8px',
                                            color: '#555555',
                                            backgroundColor: highlightedLine === i ? '#2A2B2E' : '#252527',
                                        }}
                                    >
                                        {i + 1}
                                    </span>
                                    <span className="font-mono text-[13px]" style={{ padding: '1px 14px 1px 10px', whiteSpace: 'pre' }}>
                                        {highlightLine(line).map((tok, j) => (
                                            <span key={j} style={tok.style}>{tok.text}</span>
                                        ))}
                                    </span>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Instruction Description Pane */}
                    <div style={{
                        minHeight: 110, maxHeight: 140,
                        backgroundColor: '#1C1E21',
                        borderTop: '2px solid #E8A845',
                    }}>
                        <div className="flex items-center gap-2 px-3.5 pt-2 pb-1.5">
                            <span
                                className="font-sans text-[9px] font-bold px-2 py-[2px] rounded"
                                style={{
                                    letterSpacing: '0.6px',
                                    backgroundColor: hasDescription ? '#2E2508' : '#2A2A2A',
                                    border: `1px solid ${hasDescription ? '#5A4010' : '#424547'}`,
                                    color: hasDescription ? '#E8A845' : '#777777',
                                }}
                            >
                                {hasDescription ? instrMnemonic : 'AWAITING EXECUTION'}
                            </span>
                            <div className="flex-1" />
                            <span className="text-[11px]" style={{ color: '#3A3010' }}>⚙</span>
                        </div>
                        <p
                            className="font-sans text-[12px] px-4 pb-2.5 leading-relaxed"
                            style={{ color: hasDescription ? '#E8E8E8' : '#555759' }}
                        >
                            {hasDescription
                                ? instrDesc
                                : 'A simple explanation of each executed instruction will be visible here'}
                        </p>
                    </div>
                </div>

                {/* Vertical Divider */}
                <div style={{ width: 1, backgroundColor: '#424547' }} />

                {/* Visualization Area (Right) */}
                <div className="flex flex-col flex-1 min-h-0 min-w-0 p-3.5 gap-3" style={{ backgroundColor: '#2B2B2B' }}>
                    <div className="flex gap-3 flex-1 min-h-0">
                        {/* Stack */}
                        <div className="flex flex-col h-full" style={{ minWidth: 260, width: 280, maxWidth: 320 }}>
                            <PaneCard title="Stack" style={{ height: '100%' }}>
                                <StackPanel state={state} />
                            </PaneCard>
                        </div>

                        {/* Right Column: Memory+Flags top, Registers bottom */}
                        <div className="flex flex-col flex-1 min-w-0 gap-3">
                            {/* Adjusted height to make Registers far more visible */}
                            <div className="flex gap-3" style={{ minHeight: 200, height: 230, maxHeight: 230 }}>
                                <div className="flex-1 min-w-0">
                                    <PaneCard title="Memory" style={{ height: '100%' }}>
                                        <MemoryPanel state={state} />
                                    </PaneCard>
                                </div>
                                <div style={{ minWidth: 158, width: 162, maxWidth: 162 }}>
                                    <PaneCard title="Flags" style={{ height: '100%' }}>
                                        <FlagsPanel state={state} />
                                    </PaneCard>
                                </div>
                            </div>
                            <div className="flex-1 min-h-0">
                                <PaneCard title="Registers" style={{ height: '100%' }}>
                                    <RegistersPanel state={state} />
                                </PaneCard>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Terminal Resizer */}
            <div
                className="flex items-center justify-center cursor-ns-resize select-none"
                style={{ height: 8, backgroundColor: '#2B2B2B' }}
                onMouseEnter={() => setResizerHover(true)}
                onMouseLeave={() => setResizerHover(false)}
                onMouseDown={(e) => {
                    e.preventDefault()
                    const onMove = (ev) => handleResizeDrag(ev)
                    const onUp = () => { window.removeEventListener('mousemove', onMove); window.removeEventListener('mouseup', onUp) }
                    window.addEventListener('mousemove', onMove)
                    window.addEventListener('mouseup', onUp)
                }}
            >
                <div style={{
                    width: 40, height: 3,
                    backgroundColor: resizerHover ? '#E8A845' : '#424547',
                    borderRadius: 2,
                }} />
            </div>

            {/* Terminal */}
            <div
                className="flex flex-col"
                style={{
                    height: terminalHeight, minHeight: 145,
                    backgroundColor: '#141618',
                    borderTop: '1px solid #424547',
                    boxShadow: terminalInputActive ? '0 -4px 20px rgba(78,201,78,0.18)' : 'none',
                    transition: 'box-shadow 0.2s ease-in-out',
                }}
            >
                {/* Terminal Header */}
                <div
                    className="flex items-center px-3.5"
                    style={{
                        height: 28, minHeight: 28,
                        backgroundColor: '#1A1C1E',
                        borderBottom: '1px solid #424547',
                    }}
                >
                    <span className="font-sans text-[10.5px] font-bold text-text-muted">Terminal</span>
                    <div className="flex-1" />
                    <span className="font-sans text-[10px]" style={{ color: '#444849' }}>Input enabled on scanf</span>
                </div>

                {/* Terminal Output */}
                <div
                    ref={terminalScrollRef}
                    className="flex-1 overflow-y-auto overflow-x-hidden"
                    style={{ backgroundColor: '#141618' }}
                >
                    {terminalLines.map((line, i) => (
                        <div key={i} className="flex items-start" style={{ minHeight: 20 }}>
                            {line.arrow && (
                                <span className="font-mono text-[12px] shrink-0" style={{ color: line.color, padding: '1px 6px 1px 14px' }}>❯</span>
                            )}
                            <span
                                className="font-mono text-[12px]"
                                style={{
                                    color: line.color,
                                    padding: line.arrow ? '1px 14px 1px 0' : '1px 14px',
                                    whiteSpace: line.arrow ? 'nowrap' : 'pre-wrap',
                                    wordBreak: 'break-word',
                                }}
                            >
                                {line.text}
                            </span>
                        </div>
                    ))}
                </div>

                {/* Terminal Input Row */}
                <div
                    className="flex items-center"
                    style={{
                        height: 30, minHeight: 30,
                        backgroundColor: '#141618',
                        borderTop: '1px solid #424547',
                    }}
                >
                    <span className="font-mono text-[12px] text-text-muted" style={{ padding: '0 8px 0 14px' }}>❯</span>
                    <input
                        ref={terminalInputRef}
                        type="text"
                        value={terminalInputValue}
                        onChange={e => setTerminalInputValue(e.target.value)}
                        onKeyDown={e => { if (e.key === 'Enter' && terminalInputActive) handleTerminalSubmit() }}
                        readOnly={!terminalInputActive}
                        placeholder={terminalInputActive ? 'Type input and press Enter...' : 'No input requested yet.'}
                        className="flex-1 bg-transparent border-none outline-none font-mono text-[12px]"
                        style={{
                            color: terminalInputActive ? '#4EC94E' : '#777777',
                            caretColor: terminalInputActive ? '#4EC94E' : 'transparent',
                        }}
                    />
                </div>
            </div>
        </div>
    )
}
import React, { useState, useRef, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import ReactCodeMirror from '@uiw/react-codemirror'
import { EditorView as CMEditorView, ViewPlugin, Decoration } from '@codemirror/view'
import { RangeSetBuilder } from '@codemirror/state'
import TopBar from '../shared/TopBar'
import { api } from '../../api/client';

// ─── ASM Syntax Highlighting ─────────────────────────────────────────────────

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

function resolveClass(groups) {
    if (groups.COMMENT)   return 'cm-asm-comment'
    if (groups.STRING)    return 'cm-asm-string'
    if (groups.SECTION)   return 'cm-asm-section'
    if (groups.DIRECTIVE) return 'cm-asm-directive'
    if (groups.LABEL)     return 'cm-asm-label'
    if (groups.MNEMONIC)  return 'cm-asm-mnemonic'
    if (groups.REGISTER)  return 'cm-asm-register'
    if (groups.IMMEDIATE) return 'cm-asm-immediate'
    return null
}

function buildDecorations(view) {
    const builder = new RangeSetBuilder()
    const text = view.state.doc.toString()
    ASM_PATTERN.lastIndex = 0
    let match
    while ((match = ASM_PATTERN.exec(text)) !== null) {
        const cls = resolveClass(match.groups)
        if (cls) builder.add(match.index, match.index + match[0].length, Decoration.mark({ class: cls }))
    }
    return builder.finish()
}

const asmSyntaxPlugin = ViewPlugin.fromClass(
    class {
        constructor(view) { this.decorations = buildDecorations(view) }
        update(update) {
            if (update.docChanged || update.viewportChanged)
                this.decorations = buildDecorations(update.view)
        }
    },
    { decorations: v => v.decorations }
)

const asmTheme = CMEditorView.theme({
    '&': {
        backgroundColor: '#1E1F22',
        color: '#BBBBBB',
        height: '100%',
        fontFamily: "'JetBrains Mono', Consolas, 'Courier New', monospace",
        fontSize: '13.5px',
    },
    '.cm-content': { padding: '0', caretColor: '#BBBBBB' },
    '.cm-line': { padding: '0 12px 0 4px' },
    '.cm-scroller': { overflow: 'auto', fontFamily: 'inherit' },
    '.cm-gutters': {
        backgroundColor: '#252527',
        borderRight: '1px solid #424547',
        color: '#555555',
        minWidth: '56px',
    },
    '.cm-lineNumbers .cm-gutterElement': {
        fontFamily: "'JetBrains Mono', Consolas, monospace",
        fontSize: '12px',
        padding: '0 8px 0 8px',
        minWidth: '56px',
        textAlign: 'right',
        boxSizing: 'border-box',
    },
    '.cm-activeLine': { backgroundColor: 'transparent' },
    '.cm-activeLineGutter': { backgroundColor: '#2A2B2E' },
    '&.cm-focused .cm-selectionBackground, .cm-selectionBackground': {
        backgroundColor: '#214283',
    },
    '.cm-cursor': { borderLeftColor: '#BBBBBB' },
    '.cm-asm-comment':   { color: '#6A8759' },
    '.cm-asm-string':    { color: '#6A8759' },
    '.cm-asm-label':     { color: '#C792EA', fontWeight: 'bold' },
    '.cm-asm-section':   { color: '#C792EA', fontWeight: 'bold' },
    '.cm-asm-directive': { color: '#F78C6C' },
    '.cm-asm-mnemonic':  { color: '#E8A845', fontWeight: 'bold' },
    '.cm-asm-register':  { color: '#56A6E8' },
    '.cm-asm-immediate': { color: '#A8D880' },
})

// ─── Quick Guide data ─────────────────────────────────────────────────────────

const GUIDE_ITEMS = [
    { type: 'section', label: 'Data Transfer' },
    { type: 'entry',   mnemonic: 'movq',        operands: 'src, dst',  desc: 'Copy src to dst' },
    { type: 'entry',   mnemonic: 'leaq',        operands: 'addr, dst', desc: 'Load address' },
    { type: 'entry',   mnemonic: 'pushq',       operands: 'src',       desc: 'Push to stack' },
    { type: 'entry',   mnemonic: 'popq',        operands: 'dst',       desc: 'Pop from stack' },

    { type: 'spacer' },
    { type: 'section', label: 'Arithmetic' },
    { type: 'entry',   mnemonic: 'addq',        operands: 'src, dst',  desc: 'dst += src' },
    { type: 'entry',   mnemonic: 'subq',        operands: 'src, dst',  desc: 'dst -= src' },
    { type: 'entry',   mnemonic: 'imulq',       operands: 'src, dst',  desc: 'dst *= src' },
    { type: 'entry',   mnemonic: 'idivq',       operands: 'src',       desc: 'rdx:rax / src' },
    { type: 'entry',   mnemonic: 'incq',        operands: 'dst',       desc: 'dst++' },
    { type: 'entry',   mnemonic: 'decq',        operands: 'dst',       desc: 'dst--' },
    { type: 'entry',   mnemonic: 'negq',        operands: 'dst',       desc: 'dst = -dst' },

    { type: 'spacer' },
    { type: 'section', label: 'Logical' },
    { type: 'entry',   mnemonic: 'andq',        operands: 'src, dst',  desc: 'Bitwise AND' },
    { type: 'entry',   mnemonic: 'orq',         operands: 'src, dst',  desc: 'Bitwise OR' },
    { type: 'entry',   mnemonic: 'xorq',        operands: 'src, dst',  desc: 'Bitwise XOR' },
    { type: 'entry',   mnemonic: 'notq',        operands: 'dst',       desc: 'Bitwise NOT' },
    { type: 'entry',   mnemonic: 'shlq',        operands: 'amt, dst',  desc: 'Shift left' },
    { type: 'entry',   mnemonic: 'shrq',        operands: 'amt, dst',  desc: 'Shift right' },

    { type: 'spacer' },
    { type: 'section', label: 'Control Flow' },
    { type: 'entry',   mnemonic: 'cmpq',        operands: 'a, b',      desc: 'Set flags (b-a)' },
    { type: 'entry',   mnemonic: 'jmp',         operands: 'label',     desc: 'Unconditional jump' },
    { type: 'entry',   mnemonic: 'je / jne',    operands: 'label',     desc: 'Jump if equal/not' },
    { type: 'entry',   mnemonic: 'jg / jl',     operands: 'label',     desc: 'Jump if greater/less' },
    { type: 'entry',   mnemonic: 'call',        operands: 'label',     desc: 'Call subroutine' },
    { type: 'entry',   mnemonic: 'ret',         operands: '',          desc: 'Return' },

    { type: 'spacer' },
    { type: 'section', label: 'General Registers' },
    { type: 'entry',   mnemonic: '%rax',        operands: '',          desc: 'caller-saved' },
    { type: 'entry',   mnemonic: '%rdx',        operands: '',          desc: 'caller-saved' },
    { type: 'entry',   mnemonic: '%rcx',        operands: '',          desc: 'caller-saved' },
    { type: 'entry',   mnemonic: '%r8-%r11',    operands: '',          desc: 'caller-saved' },
    { type: 'entry',   mnemonic: '%rbx',        operands: '',          desc: 'callee-saved' },
    { type: 'entry',   mnemonic: '%r12-%r15',   operands: '',          desc: 'callee-saved' },

    { type: 'spacer' },
    { type: 'section', label: 'Special Registers' },
    { type: 'entry',   mnemonic: '%rdi / %rsi', operands: '',          desc: 'Arg 1 / Arg 2' },
    { type: 'entry',   mnemonic: '%rsp',        operands: '',          desc: 'Stack pointer' },
    { type: 'entry',   mnemonic: '%rbp',        operands: '',          desc: 'Base pointer' },
    { type: 'entry',   mnemonic: '%rip',        operands: '',          desc: 'Instruction pointer' },
]

// ─── Sub-components ───────────────────────────────────────────────────────────

function RefSection({ label }) {
    return (
        <div className="px-4 pt-[10px] pb-1">
            <span className="font-mono text-[9.5px] font-bold tracking-wide text-amber uppercase">
                {label}
            </span>
        </div>
    )
}

function RefEntry({ mnemonic, operands, desc }) {
    const [hovered, setHovered] = useState(false)
    return (
        <div
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            className="px-4 py-[3px] cursor-default transition-colors"
            style={{ backgroundColor: hovered ? '#3C3F41' : 'transparent' }}
        >
            <div className="flex items-baseline gap-1">
                <span className="font-mono text-[11px] font-bold text-amber">{mnemonic}</span>
                {operands && (
                    <span className="font-mono text-[11px]" style={{ color: '#56A6E8' }}>
                        {operands}
                    </span>
                )}
            </div>
            <div className="font-sans text-[10.5px] text-text-muted">{desc}</div>
        </div>
    )
}

// ─── Main Component ───────────────────────────────────────────────────────────

export default function EditorView({ code, onCodeChange }) {
    const navigate = useNavigate()
    const fileInputRef = useRef(null)
    const extensions = useMemo(() => [asmSyntaxPlugin, asmTheme], [])

    const lineCount = code === '' ? 0 : code.split('\n').length
    const charCount = code.length
    const canSimulate = code.trim().length > 0

    const handleSimulateClick = async () => {
        try {
            await api.ping();
            navigate('/simulator');
        } catch (error) {
            console.error("Server is down or unreachable:", error);
            navigate('/server-down');
        }
    };

    function handleFileUpload(e) {
        const file = e.target.files[0]
        if (!file) return
        const reader = new FileReader()
        reader.onload = (ev) => onCodeChange(ev.target.result)
        reader.readAsText(file)
        e.target.value = ''
    }

    return (
        <div className="flex flex-col w-full h-full bg-bg-base">
            <TopBar
                left={
                    <div className="flex items-center">
                        <span className="font-sans text-[12px] font-bold text-amber">ASM SIM</span>
                        <span className="font-sans text-[12px] mx-2" style={{ color: '#777777' }}>/</span>
                        <span className="font-sans text-[12px] text-text-muted">Editor</span>
                    </div>
                }
                right={
                    <button
                        onClick={() => navigate('/')}
                        className="font-sans text-[12px] text-text-primary bg-bg-raised border border-border-soft
                                   px-3 py-1 rounded hover:bg-bg-hover transition-colors"
                    >
                        ← Home
                    </button>
                }
            />

            <div className="flex flex-1 gap-6 px-8 py-7 bg-bg-base min-h-0">

                {/* ── Left panel ── */}
                <div
                    className="flex flex-col bg-bg-panel border border-border-soft rounded-md overflow-hidden"
                    style={{ width: 230, minWidth: 210, maxWidth: 250, boxShadow: '0 6px 20px rgba(0,0,0,0.4)' }}
                >
                    <div className="px-4 pt-4 pb-3">
                        <span className="font-mono text-[10px] font-bold text-text-muted tracking-widest">
                            QUICK GUIDE
                        </span>
                    </div>
                    <div className="h-px bg-border-soft" />

                    <div className="flex-1 overflow-y-auto py-2 min-h-0">
                        {GUIDE_ITEMS.map((item, i) => {
                            if (item.type === 'section') return <RefSection key={i} label={item.label} />
                            if (item.type === 'spacer')  return <div key={i} style={{ height: 4 }} />
                            return (
                                <RefEntry
                                    key={i}
                                    mnemonic={item.mnemonic}
                                    operands={item.operands}
                                    desc={item.desc}
                                />
                            )
                        })}
                    </div>

                    <div className="h-px bg-border-soft" />

                    <div className="flex flex-col items-center gap-2 px-4 pt-4 pb-[18px]">
                        <p className="font-sans text-[11px] text-text-muted text-center leading-relaxed w-full">
                            Load code into the simulator
                        </p>
                        <button
                            onClick={handleSimulateClick}
                            disabled={!canSimulate}
                            className="w-full font-sans text-[12.5px] font-bold text-amber bg-bg-raised
                                       border border-amber py-2.5 rounded
                                       hover:bg-amber hover:text-[#1E1E1E]
                                       disabled:opacity-40 disabled:cursor-not-allowed
                                       transition-colors"
                        >
                            Simulate
                        </button>
                    </div>
                </div>

                {/* ── Editor panel ── */}
                <div
                    className="flex-1 flex flex-col bg-bg-editor border border-border-soft rounded-md min-w-0 overflow-hidden"
                    style={{ boxShadow: '0 8px 24px rgba(0,0,0,0.5)' }}
                >
                    <div className="flex items-center px-3.5 h-[38px] min-h-[38px] bg-bg-panel border-b border-border-soft rounded-t-md">
                        <div className="ml-auto">
                            <button
                                onClick={() => fileInputRef.current?.click()}
                                className="font-sans text-[11.5px] text-text-primary bg-bg-raised border border-border-soft
                                           px-3 py-1.5 rounded hover:bg-bg-hover transition-colors"
                            >
                                Upload .s/.asm File
                            </button>
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept=".s,.asm,.S"
                                onChange={handleFileUpload}
                                className="hidden"
                            />
                        </div>
                    </div>

                    <div className="flex-1 min-h-0 overflow-hidden">
                        <ReactCodeMirror
                            value={code}
                            onChange={onCodeChange}
                            extensions={extensions}
                            theme="none"
                            basicSetup={{
                                lineNumbers: true,
                                foldGutter: false,
                                dropCursor: false,
                                allowMultipleSelections: false,
                                indentOnInput: false,
                                bracketMatching: false,
                                closeBrackets: false,
                                autocompletion: false,
                                rectangularSelection: false,
                                crosshairCursor: false,
                                highlightActiveLine: false,
                                highlightSelectionMatches: false,
                                closeBracketsKeymap: false,
                                searchKeymap: false,
                            }}
                            style={{ height: '100%' }}
                        />
                    </div>

                    <div className="flex items-center px-3.5 h-[26px] min-h-[26px] bg-bg-panel border-t border-border-soft rounded-b-md">
                        <span className="font-mono text-[10.5px] text-amber">x86-64 AT&T</span>
                        <div className="ml-auto">
                            <span className="font-mono text-[11px] text-text-muted">
                                {lineCount}L&nbsp;&nbsp;{charCount}C
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
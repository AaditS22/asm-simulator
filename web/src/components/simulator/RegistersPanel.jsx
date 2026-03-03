import React, { useState, useRef, useEffect, useLayoutEffect, useCallback } from 'react'

// DISCLAIMER: This component was largely written with the help of LLMs
const REG_INFO = [
    { reg: 'rax', name32: 'eax', name16: 'ax', name8: 'al' },
    { reg: 'rcx', name32: 'ecx', name16: 'cx', name8: 'cl' },
    { reg: 'rdi', name32: 'edi', name16: 'di', name8: 'dil' },
    { reg: 'rdx', name32: 'edx', name16: 'dx', name8: 'dl' },
    { reg: 'rsi', name32: 'esi', name16: 'si', name8: 'sil' },
    { reg: 'r8',  name32: 'r8d',  name16: 'r8w',  name8: 'r8b' },
    { reg: 'r9',  name32: 'r9d',  name16: 'r9w',  name8: 'r9b' },
    { reg: 'r10', name32: 'r10d', name16: 'r10w', name8: 'r10b' },
    { reg: 'r11', name32: 'r11d', name16: 'r11w', name8: 'r11b' },
    { reg: 'rbx', name32: 'ebx', name16: 'bx', name8: 'bl' },
    { reg: 'r12', name32: 'r12d', name16: 'r12w', name8: 'r12b' },
    { reg: 'r13', name32: 'r13d', name16: 'r13w', name8: 'r13b' },
    { reg: 'r14', name32: 'r14d', name16: 'r14w', name8: 'r14b' },
    { reg: 'r15', name32: 'r15d', name16: 'r15w', name8: 'r15b' },
]

const CALLER_SAVED = new Set(['rax','rcx','rdx','rsi','rdi','r8','r9','r10','r11'])
const CALLEE_SAVED = new Set(['rbx','r12','r13','r14','r15'])

const BG_RAISED = '#3C3F41'
const BG_HOVER = '#4C5052'
const BORDER_SOFT = '#424547'
const AMBER = '#E8A845'
const BLUE = '#56A6E8'
const TEXT_PRIMARY = '#BBBBBB'
const TEXT_BRIGHT = '#E8E8E8'
const TEXT_MUTED = '#777777'
const CHANGED_BG = '#2B1E00'
const CHANGED_BDR = '#5A4010'

const BIT_DEFAULT = '#2A2C2E'
const BIT_EAX = '#1A2A3A'
const BIT_AX = '#1E3A50'
const BIT_AL = '#254A65'
const BIT_BORDER = '#3A3C3E'
const BIT_ONE_TEXT = TEXT_BRIGHT
const BIT_ZERO_TEXT = '#555555'
const HIGHLIGHT_GLOW = '#3A5A80'
const HIGHLIGHT_BORDER = '#5A8ABF'

function toLong(v) {
    if (v === undefined || v === null) return 0n
    return BigInt(v)
}

function formatHex(v) {
    const n = toLong(v)
    if (n === 0n) return '0x0'
    if (n < 0n) return '-0x' + (-n).toString(16).toUpperCase()
    return '0x' + n.toString(16).toUpperCase()
}

function formatHex64(v) {
    const n = toLong(v) & 0xFFFFFFFFFFFFFFFFn
    return '0x' + n.toString(16).toUpperCase().padStart(16, '0')
}

function formatDec64(v) {
    const n = toLong(v)
    if (n > 0x7FFFFFFFFFFFFFFFn) return (n - 0x10000000000000000n).toString()
    return n.toString()
}

function mask(bytes) {
    if (bytes === 8) return 0xFFFFFFFFFFFFFFFFn
    return (1n << BigInt(bytes * 8)) - 1n
}

function formatSubHex(v, bytes) {
    const n = (toLong(v) & mask(bytes))
    const digits = bytes * 2
    return '0x' + n.toString(16).toUpperCase().padStart(digits, '0')
}

function formatDecSigned(v, bytes) {
    let n = toLong(v) & mask(bytes)
    const signBit = 1n << BigInt(bytes * 8 - 1)
    if (n >= signBit) n = n - (1n << BigInt(bytes * 8))
    return n.toString()
}

function bitBg(bitIdx) {
    if (bitIdx < 8) return BIT_AL
    if (bitIdx < 16) return BIT_AX
    if (bitIdx < 32) return BIT_EAX
    return BIT_DEFAULT
}

function RegCard({ reg, info, value, changed, expandedReg, onToggleExpand }) {
    const [hov, setHov] = useState(false)
    const cardRef = useRef(null)
    const isCaller = CALLER_SAVED.has(reg)
    const isCallee = CALLEE_SAVED.has(reg)
    const nameColor = isCaller ? '#73C991' : (isCallee ? '#E8A845' : AMBER)
    const isExpanded = expandedReg === reg

    const bg = changed ? CHANGED_BG : (hov ? BG_HOVER : BG_RAISED)
    const bdr = changed ? CHANGED_BDR : BORDER_SOFT

    const [expandHov, setExpandHov] = useState(false)

    return (
        <div
            ref={cardRef}
            onMouseEnter={() => setHov(true)}
            onMouseLeave={() => setHov(false)}
            style={{
                backgroundColor: bg,
                border: `1px solid ${bdr}`,
                borderRadius: 5,
                padding: '7px 10px',
                width: 180,
                minWidth: 160,
                maxWidth: 220,
                cursor: 'default',
            }}
        >
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <span className="font-mono text-[11px] font-bold" style={{ color: nameColor }}>
                    %{reg}
                </span>
                <div style={{ flex: 1 }} />
                <span
                    className="font-sans text-[9px]"
                    style={{
                        color: expandHov ? AMBER : TEXT_MUTED,
                        cursor: 'pointer',
                        padding: '1px 5px',
                        backgroundColor: expandHov ? '#3A3215' : 'transparent',
                        borderRadius: 3,
                        border: `1px solid ${expandHov ? '#5A4010' : 'transparent'}`,
                    }}
                    onMouseEnter={() => setExpandHov(true)}
                    onMouseLeave={() => setExpandHov(false)}
                    onClick={(e) => { e.stopPropagation(); onToggleExpand(reg, cardRef) }}
                >
                    {isExpanded ? '▾ Collapse' : '▸ Expand'}
                </span>
            </div>
            <div style={{ marginTop: 3 }}>
                <span className="font-mono text-[10.5px]" style={{ color: TEXT_PRIMARY }}>
                    {formatHex(value)}
                </span>
            </div>
        </div>
    )
}

function BitCell({ bitIdx, bitVal, highlighted }) {
    const bg = highlighted ? HIGHLIGHT_GLOW : bitBg(bitIdx)
    const border = highlighted ? HIGHLIGHT_BORDER : BIT_BORDER
    const color = bitVal === 1 ? BIT_ONE_TEXT : BIT_ZERO_TEXT
    const finalColor = highlighted ? TEXT_BRIGHT : color

    return (
        <span
            className="font-mono text-[9.5px] inline-flex items-center justify-center"
            style={{
                width: 15, minWidth: 15, height: 18, minHeight: 18,
                backgroundColor: bg,
                border: `0.5px solid ${border}`,
                color: finalColor,
                textAlign: 'center',
            }}
        >
            {bitVal}
        </span>
    )
}

function ByteBoundaryLabels({ highBit, lowBit }) {
    const byteGroups = (highBit - lowBit + 1) / 8
    const elements = []
    for (let g = 0; g < byteGroups; g++) {
        const byteHigh = highBit - g * 8
        const byteLow = byteHigh - 7
        const byteWidth = 8 * 15 + 7
        elements.push(
            <div key={g} style={{ display: 'flex', width: byteWidth, minWidth: byteWidth }}>
                <span className="font-mono text-[8px]" style={{ color: TEXT_MUTED, minWidth: 20, textAlign: 'left' }}>{byteHigh}</span>
                <div style={{ flex: 1 }} />
                <span className="font-mono text-[8px]" style={{ color: TEXT_MUTED, minWidth: 20, textAlign: 'right' }}>{byteLow}</span>
            </div>
        )
        if (g < byteGroups - 1) {
            elements.push(<div key={`s${g}`} style={{ width: 3, minWidth: 3 }} />)
        }
    }
    return <div style={{ display: 'flex', alignItems: 'center' }}>{elements}</div>
}

function BitRow({ value, highBit, lowBit, highlightedBits }) {
    const n = toLong(value) & 0xFFFFFFFFFFFFFFFFn
    const cells = []
    for (let i = highBit; i >= lowBit; i--) {
        const bit = Number((n >> BigInt(i)) & 1n)
        const hl = highlightedBits ? (i >= highlightedBits[0] && i <= highlightedBits[1]) : false
        cells.push(<BitCell key={i} bitIdx={i} bitVal={bit} highlighted={hl} />)
        if (i > lowBit && (i - lowBit) % 8 === 0) {
            cells.push(<span key={`g${i}`} style={{ width: 3, minWidth: 3, display: 'inline-block' }} />)
        }
    }
    return <div style={{ display: 'flex', alignItems: 'center', gap: 1 }}>{cells}</div>
}

function SubRegBar({ highBit, lowBit }) {
    const ticks = []
    for (let i = highBit; i >= lowBit; i--) {
        const color = i < 8 ? BIT_AL : (i < 16 ? BIT_AX : (i < 32 ? BIT_EAX : 'transparent'))
        ticks.push(
            <span key={i} style={{
                width: 15, minWidth: 15, height: 3, minHeight: 3,
                backgroundColor: color, borderRadius: 1, display: 'inline-block',
            }} />
        )
        if (i > lowBit && (i - lowBit) % 8 === 0) {
            ticks.push(<span key={`g${i}`} style={{ width: 3, minWidth: 3, display: 'inline-block' }} />)
        }
    }
    return <div style={{ display: 'flex', alignItems: 'center', gap: 1, paddingTop: 1 }}>{ticks}</div>
}

function SubRegRow({ name, value, bytes, colorHint, bitRange, highBitIdx, lowBitIdx, showDec, onHover }) {
    const [hov, setHov] = useState(false)
    const valStr = showDec
        ? formatDecSigned(value, bytes) + '  (' + formatSubHex(value, bytes) + ')'
        : formatSubHex(value, bytes) + '  (' + formatDecSigned(value, bytes) + ')'

    return (
        <div
            onMouseEnter={() => { setHov(true); onHover([lowBitIdx, highBitIdx]) }}
            onMouseLeave={() => { setHov(false); onHover(null) }}
            style={{
                display: 'flex', alignItems: 'center', gap: 6,
                padding: '3px 8px 3px 6px', borderRadius: 4, cursor: 'pointer',
                backgroundColor: hov ? '#1A2E45' : 'transparent',
                border: `1px solid ${hov ? '#2A4A6A' : 'transparent'}`,
            }}
        >
            <span style={{ width: 4, minWidth: 4, height: 20, backgroundColor: colorHint, borderRadius: 2, display: 'inline-block' }} />
            <span className="font-mono text-[11px] font-bold" style={{ color: BLUE, minWidth: 52 }}>%{name}</span>
            <span className="font-mono text-[10px]" style={{ color: TEXT_PRIMARY }}>= {valStr}</span>
            <div style={{ flex: 1 }} />
            <span className="font-sans text-[9px]" style={{ color: TEXT_MUTED }}>{bitRange}</span>
        </div>
    )
}

function ExpandPopup({ reg, info, value, anchorRect, onClose }) {
    const [showDec, setShowDec] = useState(false)
    const [highlightedBits, setHighlightedBits] = useState(null)
    const popupRef = useRef(null)

    const [leftPos, setLeftPos] = useState(() => anchorRect.left)
    const [topPos, setTopPos] = useState(() => anchorRect.bottom + 4)

    const fullVal = toLong(value)
    const val32 = fullVal & 0xFFFFFFFFn
    const val16 = fullVal & 0xFFFFn
    const val8 = fullVal & 0xFFn

    const [toggleHov, setToggleHov] = useState(false)

    useLayoutEffect(() => {
        if (!popupRef.current) return
        const rect = popupRef.current.getBoundingClientRect()

        // Horizontal: keep within viewport
        let newLeft = anchorRect.left
        if (newLeft + rect.width > window.innerWidth - 10) {
            newLeft = Math.max(10, window.innerWidth - rect.width - 10)
        }
        setLeftPos(newLeft)

        // Vertical: try below first, then above, then clamp to viewport
        let newTop = anchorRect.bottom + 4
        if (newTop + rect.height > window.innerHeight - 10) {
            const aboveTop = anchorRect.top - rect.height - 4
            if (aboveTop >= 10) {
                newTop = aboveTop
            } else {
                // Neither direction fits cleanly — clamp to bottom of viewport
                newTop = Math.max(10, window.innerHeight - rect.height - 10)
            }
        }
        setTopPos(newTop)
    }, [anchorRect])

    useEffect(() => {
        function handleClickOutside(e) {
            if (popupRef.current && !popupRef.current.contains(e.target)) {
                onClose()
            }
        }
        document.addEventListener('click', handleClickOutside)
        return () => document.removeEventListener('click', handleClickOutside)
    }, [onClose])

    const popupStyle = {
        position: 'fixed',
        left: leftPos,
        top: topPos,
        zIndex: 9999,
        backgroundColor: '#252729',
        border: `1px solid ${BORDER_SOFT}`,
        borderRadius: 6,
        padding: '10px 14px',
        boxShadow: '0 6px 20px rgba(0,0,0,0.6)',
        display: 'flex', flexDirection: 'column', gap: 4,
    }

    return (
        <div ref={popupRef} style={popupStyle}>
            {/* Title row */}
            <div style={{ display: 'flex', alignItems: 'center' }}>
                <span className="font-mono text-[12px] font-bold" style={{ color: AMBER }}>%{reg}</span>
                <span className="font-mono text-[12px]" style={{ color: TEXT_MUTED }}>&nbsp;&nbsp;=&nbsp;&nbsp;</span>
                <span className="font-mono text-[12px] font-bold" style={{ color: TEXT_BRIGHT }}>
                    {showDec ? formatDec64(fullVal) : formatHex64(fullVal)}
                </span>
                <div style={{ flex: 1 }} />
                <span
                    className="font-sans text-[9px] font-bold"
                    style={{
                        color: toggleHov ? TEXT_BRIGHT : TEXT_MUTED,
                        backgroundColor: toggleHov ? BG_HOVER : BG_RAISED,
                        borderRadius: 3, border: `1px solid ${BORDER_SOFT}`,
                        padding: '2px 6px', cursor: 'pointer', letterSpacing: 0.5,
                    }}
                    onMouseEnter={() => setToggleHov(true)}
                    onMouseLeave={() => setToggleHov(false)}
                    onClick={() => setShowDec(!showDec)}
                >
                    {showDec ? 'HEX' : 'DEC'}
                </span>
            </div>

            {/* BIT-LEVEL VIEW */}
            <span className="font-sans text-[8.5px] font-bold tracking-wider" style={{ color: TEXT_MUTED, paddingTop: 4 }}>
                BIT-LEVEL VIEW
            </span>

            {/* Bits section */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                <ByteBoundaryLabels highBit={63} lowBit={32} />
                <BitRow value={fullVal} highBit={63} lowBit={32} highlightedBits={highlightedBits} />
                <div style={{ height: 4 }} />
                <ByteBoundaryLabels highBit={31} lowBit={0} />
                <BitRow value={fullVal} highBit={31} lowBit={0} highlightedBits={highlightedBits} />
                <SubRegBar highBit={31} lowBit={0} />
            </div>

            {/* Hint */}
            <span className="font-sans text-[9px] italic" style={{ color: TEXT_MUTED, paddingTop: 2 }}>
                ℹ  Hover a sub-register below to highlight its bits
            </span>

            {/* Divider */}
            <div style={{ height: 1, backgroundColor: BORDER_SOFT }} />

            {/* SUB-REGISTERS */}
            <span className="font-sans text-[8.5px] font-bold tracking-wider" style={{ color: TEXT_MUTED, paddingTop: 4 }}>
                SUB-REGISTERS
            </span>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <SubRegRow name={info.name32} value={val32} bytes={4} colorHint={BIT_EAX}
                           bitRange="bits 31–0" highBitIdx={31} lowBitIdx={0}
                           showDec={showDec} onHover={setHighlightedBits} />
                <SubRegRow name={info.name16} value={val16} bytes={2} colorHint={BIT_AX}
                           bitRange="bits 15–0" highBitIdx={15} lowBitIdx={0}
                           showDec={showDec} onHover={setHighlightedBits} />
                <SubRegRow name={info.name8} value={val8} bytes={1} colorHint={BIT_AL}
                           bitRange="bits 7–0" highBitIdx={7} lowBitIdx={0}
                           showDec={showDec} onHover={setHighlightedBits} />
            </div>
        </div>
    )
}

export default function RegistersPanel({ state }) {
    const registers = state?.registers || {}
    const prevValues = useRef({})
    const [changedRegs, setChangedRegs] = useState(new Set())
    const [expandedReg, setExpandedReg] = useState(null)
    const [anchorRect, setAnchorRect] = useState(null)
    const lastToggleTime = useRef(0)

    useEffect(() => {
        if (state?.stepCount === 0) {
            setChangedRegs(new Set())
            prevValues.current = { ...registers }
            return
        }
        const changed = new Set()
        for (const info of REG_INFO) {
            const current = registers[info.reg]
            const prev = prevValues.current[info.reg]
            if (prev !== undefined && prev !== current) {
                changed.add(info.reg)
            }
        }
        setChangedRegs(changed)
        prevValues.current = { ...registers }
    }, [registers, state?.stepCount])

    const handleToggleExpand = useCallback((reg, cardRef) => {
        const now = Date.now()
        if (now - lastToggleTime.current < 250) return
        lastToggleTime.current = now

        if (expandedReg === reg) {
            setExpandedReg(null)
            setAnchorRect(null)
            return
        }

        if (cardRef.current) {
            const rect = cardRef.current.getBoundingClientRect()
            setAnchorRect(rect)
        }
        setExpandedReg(reg)
    }, [expandedReg])

    const handleClosePopup = useCallback(() => {
        setExpandedReg(null)
        setAnchorRect(null)
    }, [])

    const expandedInfo = expandedReg ? REG_INFO.find(i => i.reg === expandedReg) : null

    return (
        <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minHeight: 0 }}>
            {/* Legend */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '4px 10px 0 14px' }}>
                <span style={{ color: '#73C991', fontSize: 14 }}>■</span>
                <span className="font-sans text-[10.5px]" style={{ color: TEXT_MUTED }}>Caller-saved</span>
                <span>&nbsp;&nbsp;&nbsp;</span>
                <span style={{ color: '#E8BA36', fontSize: 14 }}>■</span>
                <span className="font-sans text-[10.5px]" style={{ color: TEXT_MUTED }}>Callee-saved</span>
            </div>

            {/* Cards FlowPane */}
            <div style={{
                flex: 1, overflowY: 'auto', overflowX: 'hidden',
                padding: 10, display: 'flex', flexWrap: 'wrap',
                gap: 8, alignContent: 'flex-start',
            }}>
                {REG_INFO.map(info => (
                    <RegCard
                        key={info.reg}
                        reg={info.reg}
                        info={info}
                        value={registers[info.reg]}
                        changed={changedRegs.has(info.reg)}
                        expandedReg={expandedReg}
                        onToggleExpand={handleToggleExpand}
                    />
                ))}
            </div>

            {/* Expand Popup (portal-like, fixed position) */}
            {expandedReg && anchorRect && expandedInfo && (
                <ExpandPopup
                    reg={expandedReg}
                    info={expandedInfo}
                    value={registers[expandedReg]}
                    anchorRect={anchorRect}
                    onClose={handleClosePopup}
                />
            )}
        </div>
    )
}
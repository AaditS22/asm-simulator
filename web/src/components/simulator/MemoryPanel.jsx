import React, { useState, useRef, useEffect } from 'react'
import { api } from '../../api/client'

// DISCLAIMER: This component was largely written with the help of LLMs
const BG_RAISED = '#3C3F41'
const BG_HOVER = '#4C5052'
const BORDER_SOFT = '#424547'
const CHANGED_BG = '#2B1E00'
const CHANGED_BDR = '#5A4010'

export default function MemoryPanel({ state }) {
    const [searchText, setSearchText] = useState('')
    const prevValues = useRef({})
    const [changedAddresses, setChangedAddresses] = useState(new Set())
    const [hoveredAddr, setHoveredAddr] = useState(null)

    const memory = state?.memory || []

    useEffect(() => {
        if (memory.length === 0) return
        if (state?.stepCount === 0) {
            setChangedAddresses(new Set())
            for (const entry of memory) {
                prevValues.current[entry.addressRaw] = entry.value
            }
            return
        }
        const changed = new Set()
        for (const entry of memory) {
            const prev = prevValues.current[entry.addressRaw]
            if (prev !== undefined && prev !== entry.value) {
                changed.add(entry.addressRaw)
            }
            prevValues.current[entry.addressRaw] = entry.value
        }
        setChangedAddresses(changed)
    }, [memory, state?.stepCount])

    const sorted = [...memory].sort((a, b) => {
        const ca = changedAddresses.has(a.addressRaw)
        const cb = changedAddresses.has(b.addressRaw)
        if (ca && !cb) return -1
        if (!ca && cb) return 1
        return a.addressRaw - b.addressRaw
    })

    async function handleTrack() {
        const text = searchText.trim()
        if (!text) return
        try {
            const res = await api.trackMemory(text)
            if (res && window.__setSimState) window.__setSimState(res)
            setSearchText('')
        } catch {
            setSearchText('Invalid format')
        }
    }

    function rowStyle(addr, isHovered) {
        const changed = changedAddresses.has(addr)
        const bg = changed ? CHANGED_BG : (isHovered ? BG_HOVER : BG_RAISED)
        const border = changed ? CHANGED_BDR : BORDER_SOFT
        return {
            backgroundColor: bg,
            border: `1px solid ${border}`,
            borderRadius: 5,
        }
    }

    return (
        <div className="flex flex-col flex-1 min-h-0">
            {/* Search Bar */}
            <div className="flex items-center gap-2 px-2.5 pt-2.5 pb-0">
                <input
                    type="text"
                    value={searchText}
                    onChange={e => setSearchText(e.target.value)}
                    onKeyDown={e => { if (e.key === 'Enter') handleTrack() }}
                    placeholder="Enter a memory address you want to track, in decimal/hex format (e.g. 0x4000)"
                    className="flex-1 font-mono text-[11px] px-2 py-1 rounded outline-none"
                    style={{
                        backgroundColor: BG_RAISED,
                        color: '#BBBBBB',
                        border: `1px solid ${BORDER_SOFT}`,
                    }}
                />
                <button
                    onClick={handleTrack}
                    className="font-sans text-[11px] px-2.5 py-1 rounded cursor-pointer transition-colors"
                    style={{ backgroundColor: BG_RAISED, color: '#BBBBBB', border: `1px solid ${BORDER_SOFT}` }}
                    onMouseEnter={e => { e.currentTarget.style.backgroundColor = BG_HOVER }}
                    onMouseLeave={e => { e.currentTarget.style.backgroundColor = BG_RAISED }}
                >
                    Track
                </button>
            </div>

            {/* Memory Rows */}
            <div className="flex-1 overflow-y-auto overflow-x-hidden px-2.5 py-2.5" style={{ gap: 6, display: 'flex', flexDirection: 'column' }}>
                {sorted.length === 0 && (
                    <div className="flex items-center justify-center flex-1">
                        <span className="font-sans text-[11px] text-text-muted">No memory addresses tracked yet</span>
                    </div>
                )}
                {sorted.map(entry => {
                    const isHovered = hoveredAddr === entry.addressRaw
                    const hasLabel = !!entry.label
                    const subtitle = hasLabel
                        ? entry.address
                        : (entry.reason || 'Tracked Address')

                    return (
                        <div
                            key={entry.addressRaw}
                            className="flex items-center gap-2 px-2.5 py-2"
                            style={rowStyle(entry.addressRaw, isHovered)}
                            onMouseEnter={() => setHoveredAddr(entry.addressRaw)}
                            onMouseLeave={() => setHoveredAddr(null)}
                        >
                            <div className="flex flex-col gap-0.5">
                                <span className="font-mono text-[11px] font-bold" style={{ color: '#E8A845' }}>
                                    {hasLabel ? entry.label : entry.address}
                                </span>
                                <span className="font-mono text-[10.5px] text-text-muted">{subtitle}</span>
                            </div>
                            <div className="flex-1" />
                            <span className="font-mono text-[11px] text-text-primary">{entry.valueHex}</span>
                        </div>
                    )
                })}
            </div>
        </div>
    )
}
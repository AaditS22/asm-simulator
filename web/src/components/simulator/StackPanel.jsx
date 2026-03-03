import React, { useState, useRef, useLayoutEffect } from 'react'
import { api } from '../../api/client'

// DISCLAIMER: This component was largely written with the help of LLMs
const ROWS = 10

const ROW_EVEN = 'transparent'
const ROW_ODD = 'rgba(255,255,255,0.025)'
const ROW_RSP = '#0A1E38'
const ROW_RBP = '#0A2014'
const ROW_BOTH = '#1A0D30'
const ROW_CHANGED = '#2B1E00'

const RSP_FG = '#56A6E8', RSP_BG = '#0D2540', RSP_BDR = '#1B4F80'
const RBP_FG = '#6A8759', RBP_BG = '#0D2A14', RBP_BDR = '#1D5C30'
const BTH_FG = '#C09AFF', BTH_BG = '#2A1040', BTH_BDR = '#5A3090'

function toHex(v) {
    if (v === 0 || v === 0n) return '0x0'
    const n = typeof v === 'bigint' ? v : BigInt(v)
    if (n < 0n) return '-0x' + (-n).toString(16).toUpperCase()
    let hex = n.toString(16).toUpperCase()
    if (hex.length > 8) return '…' + hex.slice(-7)
    return '0x' + hex
}

function formatRawAddr(addrRaw) {
    if (addrRaw === 0 || addrRaw === 0n) return '0x0'
    const n = typeof addrRaw === 'bigint' ? addrRaw : BigInt(addrRaw)
    let s = n.toString(16).toUpperCase()
    if (s.length > 8) return '…' + s.slice(-7)
    return '0x' + s
}

function toDecSigned(v) {
    const n = BigInt(v)
    if (n > 0x7FFFFFFFFFFFFFFFn) return (n - 0x10000000000000000n).toString()
    return n.toString()
}

function relAddr(addr, rbp) {
    const diff = Number(BigInt(addr) - BigInt(rbp))
    if (diff === 0) return 'rbp'
    if (diff > 0) return `rbp+${diff}`
    return `rbp${diff}`
}

function Badge({ text, fg, bg, bdr }) {
    return (
        <span
            className="font-mono text-[8px] font-bold px-1 py-[1px] rounded"
            style={{ color: fg, backgroundColor: bg, border: `1px solid ${bdr}` }}
        >
            {text}
        </span>
    )
}

export default function StackPanel({ state }) {
    const [reversed, setReversed] = useState(false)
    const [addrRelative, setAddrRelative] = useState(true)
    const [valHex, setValHex] = useState(true)
    const prevValues = useRef({})
    const prevStepCount = useRef(-1)
    const [changedAddrs, setChangedAddrs] = useState(new Set())

    const stack = state?.stack || []
    const rbp = state?.registers?.rbp ?? 0

    useLayoutEffect(() => {
        if (stack.length === 0) return

        if (state?.stepCount === 0) {
            setChangedAddrs(new Set())
            const m = {}
            for (const r of stack) { if (r) m[r.addressRaw] = r.value }
            prevValues.current = m
            prevStepCount.current = 0
            return
        }

        const stepChanged = state?.stepCount !== prevStepCount.current
        prevStepCount.current = state?.stepCount ?? prevStepCount.current

        const changed = new Set()
        for (const r of stack) {
            if (!r) continue
            const prev = prevValues.current[r.addressRaw]
            if (prev === undefined) {
                if (stepChanged) changed.add(r.addressRaw)
            } else if (prev !== r.value) {
                changed.add(r.addressRaw)
            }
            prevValues.current[r.addressRaw] = r.value
        }
        setChangedAddrs(changed)
    }, [stack, state?.stepCount])

    async function scrollStack(direction) {
        try {
            const res = await api.scrollStack(direction)
            if (res && window.__setSimState) window.__setSimState(res)
        } catch {}
    }

    function getRowBg(dr, isRsp, isRbp, isChanged) {
        if (isRsp && isRbp) return ROW_BOTH
        if (isRsp) return ROW_RSP
        if (isRbp) return ROW_RBP
        if (isChanged) return ROW_CHANGED
        return dr % 2 === 0 ? ROW_EVEN : ROW_ODD
    }

    function renderBadges(isRsp, isRbp) {
        if (isRsp && isRbp) return <Badge text="RSP+RBP" fg={BTH_FG} bg={BTH_BG} bdr={BTH_BDR} />
        if (isRsp) return <Badge text="RSP" fg={RSP_FG} bg={RSP_BG} bdr={RSP_BDR} />
        if (isRbp) return <Badge text="RBP" fg={RBP_FG} bg={RBP_BG} bdr={RBP_BDR} />
        return null
    }

    function BtnSmall({ children, onClick }) {
        const [hov, setHov] = useState(false)
        return (
            <button
                onClick={onClick}
                onMouseEnter={() => setHov(true)}
                onMouseLeave={() => setHov(false)}
                className="flex-1 font-sans text-[10px] font-bold py-[3px] rounded text-center transition-colors"
                style={{
                    backgroundColor: hov ? '#4C5052' : '#3C3F41',
                    color: hov ? '#E8E8E8' : '#BBBBBB',
                    border: '1px solid #424547',
                }}
            >
                {children}
            </button>
        )
    }

    function ToggleLbl({ children, onClick }) {
        const [hov, setHov] = useState(false)
        return (
            <span
                onClick={onClick}
                onMouseEnter={() => setHov(true)}
                onMouseLeave={() => setHov(false)}
                className="flex-1 font-sans text-[10px] font-bold py-[3px] rounded text-center cursor-pointer transition-colors"
                style={{
                    backgroundColor: hov ? '#4C5052' : '#3C3F41',
                    color: hov ? '#E8E8E8' : '#BBBBBB',
                    border: '1px solid #424547',
                }}
            >
                {children}
            </span>
        )
    }

    return (
        <div className="flex flex-col flex-1 h-full min-h-0">
            <div className="flex gap-1 px-2.5 pt-2 pb-1" style={{ backgroundColor: '#3C3F41' }}>
                <BtnSmall onClick={() => scrollStack(1)}>↑&nbsp;&nbsp;Up</BtnSmall>
                <ToggleLbl onClick={() => setReversed(!reversed)}>
                    ⇅&nbsp;&nbsp;{reversed ? 'Normal' : 'Flip'}
                </ToggleLbl>
                <BtnSmall onClick={() => scrollStack(-1)}>↓&nbsp;&nbsp;Down</BtnSmall>
            </div>

            <div className="flex gap-1 px-2.5 pb-2" style={{ backgroundColor: '#3C3F41' }}>
                <ToggleLbl onClick={() => setAddrRelative(!addrRelative)}>
                    Addr: {addrRelative ? 'Relative' : 'Raw'}
                </ToggleLbl>
                <ToggleLbl onClick={() => setValHex(!valHex)}>
                    Value: {valHex ? 'Hex' : 'Dec'}
                </ToggleLbl>
            </div>

            <div
                className="flex items-center gap-1.5 px-2.5 py-[3px]"
                style={{ backgroundColor: '#272829', borderBottom: '1px solid #424547' }}
            >
                <span className="font-sans text-[9px] font-bold uppercase tracking-wider text-text-muted" style={{ width: 62 }}>PTR</span>
                <span className="font-sans text-[9px] font-bold uppercase tracking-wider text-text-muted" style={{ width: 72 }}>ADDRESS</span>
                <span className="font-sans text-[9px] font-bold uppercase tracking-wider text-text-muted">&nbsp;&nbsp;</span>
                <span className="font-sans text-[9px] font-bold uppercase tracking-wider text-text-muted flex-1">
                    VALUE ({valHex ? 'hex' : 'dec'})
                </span>
            </div>

            <div className="flex-1 overflow-hidden flex flex-col min-h-0">
                {Array.from({ length: ROWS }).map((_, dr) => {
                    const di = reversed ? (ROWS - 1 - dr) : dr
                    const row = stack[di]

                    if (!row) return (
                        <div key={dr} className="flex flex-1 items-center px-2.5 min-h-0" style={{ backgroundColor: dr % 2 === 0 ? ROW_EVEN : ROW_ODD }}>
                            <span className="font-mono text-[10px] text-text-muted">—</span>
                        </div>
                    )

                    const isChanged = changedAddrs.has(row.addressRaw)
                    const bg = getRowBg(dr, row.isRsp, row.isRbp, isChanged)
                    const addrText = addrRelative ? relAddr(row.addressRaw, rbp) : formatRawAddr(row.addressRaw)
                    const valText = valHex ? toHex(row.value) : toDecSigned(row.value)

                    return (
                        <div key={dr} className="flex flex-1 items-center gap-1.5 px-2.5 min-h-0" style={{ backgroundColor: bg }}>
                            <span style={{ width: 62 }}>{renderBadges(row.isRsp, row.isRbp)}</span>
                            <span className="font-mono text-[10px]" style={{ width: 72, color: '#777777' }}>{addrText}</span>
                            <span className="font-mono text-[10px]">&nbsp;&nbsp;</span>
                            <span
                                className="font-mono text-[10.5px] flex-1"
                                style={{ color: isChanged ? '#E8A845' : '#BBBBBB', fontWeight: isChanged ? 'bold' : 'normal' }}
                            >
                                {valText}
                            </span>
                        </div>
                    )
                })}
            </div>

            <div className="flex items-center gap-2.5 px-2.5 py-1.5" style={{ backgroundColor: '#272829' }}>
                <div className="flex items-center gap-1">
                    <Badge text="RSP" fg={RSP_FG} bg={RSP_BG} bdr={RSP_BDR} />
                    <span className="font-sans text-[9px] text-text-muted">= RSP</span>
                </div>
                <div className="flex items-center gap-1">
                    <Badge text="RBP" fg={RBP_FG} bg={RBP_BG} bdr={RBP_BDR} />
                    <span className="font-sans text-[9px] text-text-muted">= RBP</span>
                </div>
                <div className="flex items-center gap-1">
                    <Badge text="RSP+RBP" fg={BTH_FG} bg={BTH_BG} bdr={BTH_BDR} />
                    <span className="font-sans text-[9px] text-text-muted">= RSP+RBP</span>
                </div>
            </div>
        </div>
    )
}
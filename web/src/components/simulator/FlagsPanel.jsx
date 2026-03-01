import React, { useState, useRef, useEffect } from 'react'

const AMBER = '#E8A845'
const TEXT_MUTED = '#606468'
const TEXT_DIM = '#4A4D50'
const BORDER_SOFT = '#333537'
const CHIP_OFF_BG = '#252729'
const CHIP_OFF_BORDER = '#353739'
const CHIP_ON_BG = '#1A3820'
const CHIP_ON_BORDER = '#3A7A3A'
const CHIP_ON_TEXT = '#6ADA6A'
const HIGHLIGHT_BLUE = '#6AADDA'
const HIGHLIGHT_AMBER = '#D4924A'
const DESC_BASE = '#8A8F94'

const FLAG_DEFS = [
    { key: 'zf', acronym: 'ZF', fullName: 'Zero' },
    { key: 'cf', acronym: 'CF', fullName: 'Carry' },
    { key: 'sf', acronym: 'SF', fullName: 'Sign' },
    { key: 'of', acronym: 'OF', fullName: 'Overflow' },
]

function buildDescription(zf, cf, sf, of) {
    const segs = []
    function plain(s) { segs.push({ text: s, type: 'plain' }) }
    function hi(s) { segs.push({ text: s, type: 'hi' }) }
    function flag(s) { segs.push({ text: s, type: 'flag' }) }

    if (!zf && !cf && !sf && !of) {
        plain('All flags are clear. The last result was a ')
        hi('non-zero positive number')
        plain(' with no carry or overflow.')
        return segs
    }

    if (sf && of) {
        plain('The result looks ')
        hi('negative')
        plain(', but ')
        hi('signed overflow')
        plain(' occurred — the true result was ')
        hi('too large to fit')
        plain(' and wrapped past the boundary, flipping the sign.')
        return segs
    }

    if (zf && !cf && !sf && !of) {
        plain('The last result was ')
        hi('exactly zero')
        plain('. Often set after a ')
        flag('CMP')
        plain(' when both operands are equal, or an arithmetic operation that yielded 0.')
        return segs
    }

    if (cf && !zf && !sf && !of) {
        plain('An ')
        hi('unsigned overflow/borrow')
        plain(' happened — the result did not fit in the destination width when treated as an unsigned value.')
        return segs
    }

    if (sf && !zf && !cf && !of) {
        plain('The result is ')
        hi('negative')
        plain(' (its most-significant bit is 1). For unsigned math this simply means the high bit is set.')
        return segs
    }

    if (of && !zf && !cf && !sf) {
        hi('Signed overflow')
        plain(' occurred — the true result was ')
        hi('outside the signed range')
        plain(' for the operand size, so the stored value wrapped around.')
        return segs
    }

    if (zf && cf) {
        plain('Result is ')
        hi('zero')
        plain(' and an ')
        hi('unsigned carry')
        plain(' occurred. After ')
        flag('CMP a, b')
        plain(': b ≤ a (unsigned).')
        return segs
    }

    if (sf && cf) {
        plain('Result is ')
        hi('negative')
        plain(' with an ')
        hi('unsigned carry')
        plain('. The unsigned result exceeded the register width and the signed result is negative.')
        return segs
    }

    const active = []
    if (zf) active.push('ZF')
    if (cf) active.push('CF')
    if (sf) active.push('SF')
    if (of) active.push('OF')

    plain('Active: ')
    active.forEach((f, i) => {
        flag(f)
        if (i < active.length - 1) plain(', ')
    })
    plain('. This is an uncommon combination — check each flag individually.')
    return segs
}

function DescSegment({ seg }) {
    if (seg.type === 'hi') {
        return <span className="font-sans text-[9.5px] font-bold" style={{ color: HIGHLIGHT_BLUE }}>{seg.text}</span>
    }
    if (seg.type === 'flag') {
        return <span className="font-mono text-[9px] font-bold" style={{ color: HIGHLIGHT_AMBER }}>{seg.text}</span>
    }
    return <span className="font-sans text-[9.5px]" style={{ color: DESC_BASE }}>{seg.text}</span>
}

export default function FlagsPanel({ state }) {
    const flags = state?.flags || { zf: false, cf: false, sf: false, of: false }
    const prevRef = useRef({})
    const [changedFlags, setChangedFlags] = useState(new Set())

    useEffect(() => {
        const changed = new Set()
        if (state?.stepCount === 0) {
            setChangedFlags(new Set())
            prevRef.current = { ...flags }
            return
        }
        for (const def of FLAG_DEFS) {
            const prev = prevRef.current[def.key]
            if (prev !== undefined && prev !== flags[def.key]) {
                changed.add(def.key)
            }
        }
        setChangedFlags(changed)
        prevRef.current = { ...flags }
    }, [flags.zf, flags.cf, flags.sf, flags.of, state?.stepCount])

    const descSegs = buildDescription(flags.zf, flags.cf, flags.sf, flags.of)

    return (
        <div className="flex flex-col pt-2 px-3 pb-2 overflow-y-auto" style={{ gap: 0, height: '100%' }}>
            {FLAG_DEFS.map((def, i) => (
                <React.Fragment key={def.key}>
                    <div className="flex items-center gap-1.5 py-[6px]">
                        <span className="font-mono text-[10.5px] font-bold" style={{ color: AMBER }}>{def.acronym}</span>
                        <span className="font-sans text-[9.5px]" style={{ color: TEXT_MUTED, paddingTop: 1 }}>{def.fullName}</span>
                        <div className="flex-1" />
                        <span
                            className="font-mono text-[10.5px] font-bold px-[8px] py-[1px] rounded"
                            style={{
                                backgroundColor: changedFlags.has(def.key) ? CHIP_ON_BG : CHIP_OFF_BG,
                                border: `1px solid ${changedFlags.has(def.key) ? CHIP_ON_BORDER : CHIP_OFF_BORDER}`,
                                color: changedFlags.has(def.key) ? CHIP_ON_TEXT : TEXT_DIM,
                            }}
                        >
                            {flags[def.key] ? '1' : '0'}
                        </span>
                    </div>
                    {i < FLAG_DEFS.length - 1 && (
                        <div style={{ height: 1, backgroundColor: BORDER_SOFT }} />
                    )}
                </React.Fragment>
            ))}

            <div style={{ height: 10 }} />

            <div
                className="pt-2 leading-relaxed"
                style={{ borderTop: `1px solid ${BORDER_SOFT}`, lineHeight: 1.6 }}
            >
                {descSegs.map((seg, i) => (
                    <DescSegment key={i} seg={seg} />
                ))}
            </div>
        </div>
    )
}
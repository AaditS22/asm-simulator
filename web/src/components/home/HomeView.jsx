import React from 'react'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import TopBar from '../shared/TopBar'

function AboutModal({ onClose }) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
            <div
                className="w-[340px] flex flex-col border border-border-soft"
                style={{ boxShadow: '0 8px 32px rgba(0,0,0,0.6)' }}
            >
                <div className="flex items-center px-4 h-9 min-h-[36px] bg-bg-panel border-b border-border-soft">
                    <span className="font-sans text-[11.5px] text-text-muted">About</span>
                    <div className="ml-auto">
                        <button
                            onClick={onClose}
                            className="px-2 py-1 rounded text-text-muted bg-transparent hover:bg-red-600 hover:text-white transition-colors text-sm"
                        >
                            ✕
                        </button>
                    </div>
                </div>

                <div className="bg-bg-base flex flex-col items-center gap-5 py-9 px-10">
                    <span className="font-sans text-[20px] font-bold text-amber">Version 1.0</span>

                    <div className="h-px w-[200px] bg-border-soft" />

                    <span className="font-sans text-[13px] text-text-primary">Created by AaditS22</span>

                    <a
                        href="https://github.com/AaditS22/asm-simulator"
                        target="_blank"
                        rel="noreferrer"
                        className="font-sans text-[12px] text-amber hover:text-text-bright hover:underline transition-colors"
                    >
                        View project on GitHub →
                    </a>
                </div>
            </div>
        </div>
    )
}

export default function HomeView() {
    const navigate = useNavigate()
    const [showAbout, setShowAbout] = useState(false)

    return (
        <div className="flex flex-col w-full h-full bg-bg-base">
            <TopBar
                left={
                    <div className="flex items-center">
                        <span className="font-sans text-[12px] font-bold text-amber">ASM SIM</span>
                        <span className="font-sans text-[12px] mx-2" style={{ color: '#777777' }}>/</span>
                        <span className="font-sans text-[12px] text-text-muted">Home</span>
                    </div>
                }
                right={
                    <button
                        onClick={() => setShowAbout(true)}
                        className="font-sans text-[12px] text-text-primary bg-bg-raised border border-border-soft
                                   px-3 py-1 rounded hover:bg-bg-hover transition-colors"
                    >
                        About
                    </button>
                }
            />

            <div className="flex-1 flex items-center justify-center bg-bg-base">
                <div className="w-full max-w-[560px] flex flex-col">
                    <div
                        className="flex flex-col gap-7 p-10 bg-bg-panel border border-border-soft rounded-md"
                        style={{ boxShadow: '0 6px 20px rgba(0,0,0,0.4)' }}
                    >
                        <div className="flex flex-col gap-1.5">
                            <h1 className="font-sans text-[48px] font-bold leading-none text-text-bright">
                                ASM Sim
                            </h1>
                            <span className="font-mono text-[12px] text-amber">
                                Assembly Simulator &amp; Debugger
                            </span>
                        </div>

                        <div className="h-px bg-border-soft" />

                        <p className="font-sans text-[13px] text-text-primary leading-relaxed">
                            Write and/or upload custom assembly code, then watch it run in the simulator!
                            Visualize how the CPU&apos;s state is changing with each instruction, and get
                            detailed descriptions of your code!
                        </p>

                        <button
                            onClick={() => navigate('/editor')}
                            className="self-start font-sans text-[12.5px] font-bold text-amber bg-bg-raised
                                       border border-amber px-7 py-2.5 rounded
                                       hover:bg-amber hover:text-[#1E1E1E] transition-colors"
                        >
                            Open Editor
                        </button>
                    </div>

                    <div
                        className="mt-4 px-5 py-3 bg-[#442726] border border-[#913B36] rounded-md"
                    >
                        <p className="font-mono text-[10.5px] text-[#FF9B94] leading-relaxed">
                            DISCLAIMER: This is only a teaching tool and does not perfectly mimic real CPU
                            behaviour. It is made as a learning and experimentation tool, not to be used
                            for production.
                        </p>
                    </div>
                </div>
            </div>

            {showAbout && <AboutModal onClose={() => setShowAbout(false)} />}
        </div>
    )
}
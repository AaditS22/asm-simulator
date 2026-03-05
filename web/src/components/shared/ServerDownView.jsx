import React from 'react';
import { useNavigate } from 'react-router-dom';
import TopBar from './TopBar'; // Since they are in the same 'shared' folder

export default function ServerDownView() {
    const navigate = useNavigate();

    return (
        <div className="flex flex-col w-full h-screen bg-bg-base">
            <TopBar
                left={
                    <div className="flex items-center">
                        <span className="font-sans text-[12px] font-bold text-amber">ASM SIM</span>
                        <span className="font-sans text-[12px] mx-2" style={{ color: '#777777' }}>/</span>
                        <span className="font-sans text-[12px] text-text-muted">Status</span>
                    </div>
                }
            />

            <div className="flex flex-1 items-center justify-center p-6 min-h-0">
                <div
                    className="flex flex-col items-center bg-bg-panel border border-border-soft rounded-md p-8 max-w-sm w-full"
                    style={{ boxShadow: '0 8px 24px rgba(0,0,0,0.5)' }}
                >
                    <svg
                        className="w-12 h-12 text-amber mb-5 opacity-90"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        xmlns="http://www.w3.org/2000/svg"
                    >
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>

                    <h2 className="font-sans text-[15px] font-bold text-text-primary mb-3 tracking-wide">
                        Server Unreachable
                    </h2>

                    <p className="font-sans text-[11.5px] text-text-muted text-center leading-relaxed w-full mb-8">
                        Unable to connect to the backend. The server might be down, or there is a network issue. You can still
                        download the local application (from the homepage) and run it offline, however.
                    </p>

                    <button
                        onClick={() => navigate('/editor')}
                        className="w-full font-sans text-[12.5px] font-bold text-amber bg-bg-raised
                                   border border-amber py-2.5 rounded
                                   hover:bg-amber hover:text-[#1E1E1E]
                                   transition-colors"
                    >
                        Return to Editor
                    </button>
                </div>
            </div>
        </div>
    );
}
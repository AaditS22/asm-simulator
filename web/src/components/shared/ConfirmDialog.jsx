import React from 'react'
export default function ConfirmDialog({ message, onConfirm, onCancel }) {
    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
            <div className="bg-bg-base border border-border-soft rounded-md w-[340px] p-7 flex flex-col gap-5">
                <p className="font-sans text-text-bright text-[13px] text-center leading-relaxed">
                    {message}
                </p>
                <div className="flex gap-3 justify-center">
                    <button
                        onClick={onCancel}
                        className="w-[110px] py-2 rounded font-bold text-amber bg-bg-raised border border-border-soft
                       hover:bg-bg-hover transition-colors text-sm"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={onConfirm}
                        className="w-[110px] py-2 rounded font-bold text-white bg-red-700
                       hover:bg-red-500 transition-colors text-sm"
                    >
                        Yes, close
                    </button>
                </div>
            </div>
        </div>
    )
}
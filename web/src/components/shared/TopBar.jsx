import { useState } from 'react'
import ConfirmDialog from './ConfirmDialog'

export default function TopBar({ left, center, right, onClose, closeConfirmMsg }) {
    const [showConfirm, setShowConfirm] = useState(false)

    function handleClose() {
        if (closeConfirmMsg) setShowConfirm(true)
    }

    return (
        <>
            <div className="flex items-center px-4 h-10 min-h-[40px] bg-bg-panel border-b border-border-soft">
                <div className="flex items-center gap-2 flex-1">{left}</div>
                {center && <div className="flex items-center">{center}</div>}
                <div className="flex items-center gap-2 ml-auto">
                    {right}
                    {onClose && (
                        <button
                            onClick={handleClose}
                            className="px-2.5 py-1 rounded text-text-muted bg-bg-raised border border-border-soft
                         hover:bg-red-600 hover:text-white hover:border-red-600 transition-colors text-sm"
                        >
                            ✕
                        </button>
                    )}
                </div>
            </div>
            {showConfirm && (
                <ConfirmDialog
                    message={closeConfirmMsg}
                    onConfirm={() => { setShowConfirm(false); onClose() }}
                    onCancel={() => setShowConfirm(false)}
                />
            )}
        </>
    )
}
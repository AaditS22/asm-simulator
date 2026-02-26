import React from 'react'
export function PrimaryBtn({ children, onClick, disabled }) {
    return (
        <button
            onClick={onClick}
            disabled={disabled}
            className={`
        font-sans text-[11px] font-bold px-3 py-1.5 rounded
        border border-amber text-amber bg-bg-raised
        hover:bg-amber hover:text-bg-editor
        disabled:opacity-40 disabled:cursor-not-allowed
        transition-colors duration-100
      `}
        >
            {children}
        </button>
    )
}

export function SecondaryBtn({ children, onClick, disabled }) {
    return (
        <button
            onClick={onClick}
            disabled={disabled}
            className={`
        font-sans text-[11px] px-3 py-1.5 rounded
        border border-border-soft text-text-primary bg-bg-raised
        hover:bg-bg-hover hover:text-text-bright
        disabled:opacity-40 disabled:cursor-not-allowed
        transition-colors duration-100
      `}
        >
            {children}
        </button>
    )
}
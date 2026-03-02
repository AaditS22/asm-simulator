import React from 'react'
export default function PaneCard({ title, children, style, headerRight }) {
    return (
        <div
            className="flex flex-col rounded-md border border-border-soft bg-bg-panel overflow-hidden"
            style={{ boxShadow: '0 5px 16px rgba(0,0,0,0.45)', ...style }}
        >
            <div className="flex items-center px-3.5 h-[34px] min-h-[34px] bg-bg-raised">
        <span className="font-sans text-[10px] font-bold tracking-widest uppercase text-text-muted">
          {title}
        </span>
                {headerRight && <div className="ml-auto">{headerRight}</div>}
            </div>
            <div className="h-px bg-border-soft" />
            <div className="flex flex-col flex-1 min-h-0">
                {children}
            </div>
        </div>
    )
}
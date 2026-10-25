type BrandMarkProps = {
  compact?: boolean
}

export function BrandMark({ compact = false }: BrandMarkProps) {
  return (
    <span className={`brand-lockup${compact ? ' brand-lockup--compact' : ''}`}>
      <span className="brand-mark" aria-hidden="true">
        <svg viewBox="0 0 48 48" role="presentation">
          <path d="M9 22h7l4-5 4 5h15v11a5 5 0 0 1-5 5H14a5 5 0 0 1-5-5V22Z" />
          <path d="M15 27v6m-3-3h6M32 27h.01M36 31h.01" />
          <path d="M23 22v9m4-9v9" className="brand-mark__pause" />
        </svg>
      </span>
      {!compact && <span className="brand-wordmark"><b>ADBREAK</b><i>/</i><strong>ARCADE</strong></span>}
    </span>
  )
}

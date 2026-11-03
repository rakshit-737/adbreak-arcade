import { useEffect, useRef, useState, type CSSProperties, type MouseEvent, type ReactNode, type RefObject } from 'react'

export function CursorLayer() {
  const [position, setPosition] = useState({ x: -100, y: -100 })
  const [label, setLabel] = useState('')
  const [active, setActive] = useState(false)

  useEffect(() => {
    const finePointer = window.matchMedia('(pointer: fine) and (min-width: 800px)')
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
    if (!finePointer.matches || reducedMotion.matches) return
    document.documentElement.classList.add('custom-cursor-ready')
    const move = (event: PointerEvent) => {
      setPosition({ x: event.clientX, y: event.clientY })
      const target = (event.target as HTMLElement).closest<HTMLElement>('[data-cursor]')
      setLabel(target?.dataset.cursor ?? '')
      setActive(Boolean(target))
    }
    window.addEventListener('pointermove', move)
    return () => {
      window.removeEventListener('pointermove', move)
      document.documentElement.classList.remove('custom-cursor-ready')
    }
  }, [])

  return <div className={`cursor-layer ${active ? 'is-active' : ''}`} style={{ '--cursor-x': `${position.x}px`, '--cursor-y': `${position.y}px` } as CSSProperties} aria-hidden="true"><span className="cursor-dot" /><span className="cursor-ring" />{label && <span className="cursor-label">{label}</span>}</div>
}

type MagneticProps = { children: ReactNode; className?: string; href?: string; onClick?: () => void; cursor?: string }

export function Magnetic({ children, className = '', href, onClick, cursor = 'open' }: MagneticProps) {
  const ref = useRef<HTMLElement>(null)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const move = (event: MouseEvent<HTMLElement>) => {
    const element = ref.current
    if (!element) return
    const box = element.getBoundingClientRect()
    setOffset({ x: (event.clientX - box.left - box.width / 2) * 0.08, y: (event.clientY - box.top - box.height / 2) * 0.08 })
  }
  const reset = () => setOffset({ x: 0, y: 0 })
  const style = { transform: `translate3d(${offset.x}px, ${offset.y}px, 0)` }
  if (href) return <a ref={ref as RefObject<HTMLAnchorElement>} className={className} href={href} onMouseMove={move} onMouseLeave={reset} onClick={onClick} data-cursor={cursor} style={style}>{children}</a>
  return <button ref={ref as RefObject<HTMLButtonElement>} className={className} type="button" onMouseMove={move} onMouseLeave={reset} onClick={onClick} data-cursor={cursor} style={style}>{children}</button>
}

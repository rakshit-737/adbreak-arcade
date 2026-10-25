import type { SVGProps } from 'react'

export type IconName =
  | 'arrow'
  | 'board'
  | 'chart'
  | 'check'
  | 'chevron'
  | 'columns'
  | 'external'
  | 'grid'
  | 'hand'
  | 'letters'
  | 'lock'
  | 'menu'
  | 'pause'
  | 'play'
  | 'plus'
  | 'question'
  | 'search'
  | 'smile'
  | 'spark'
  | 'stones'
  | 'sun'
  | 'target'
  | 'type'
  | 'up'
  | 'x'

type IconProps = SVGProps<SVGSVGElement> & { name: IconName }

export function Icon({ name, ...props }: IconProps) {
  const shared = { fill: 'none', stroke: 'currentColor', strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, strokeWidth: 1.8 }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" {...shared} {...props}>
      {name === 'arrow' && <><path d="M5 12h13" /><path d="m13 6 6 6-6 6" /></>}
      {name === 'board' && <><rect x="4" y="4" width="16" height="16" rx="2" /><path d="M10 4v16M16 4v16M4 10h16M4 16h16" /></>}
      {name === 'chart' && <><path d="M4 18V6M4 18h16" /><path d="m7 14 3-3 3 2 5-6" /></>}
      {name === 'check' && <path d="m5 12 4 4L19 6" />}
      {name === 'chevron' && <path d="m6 9 6 6 6-6" />}
      {name === 'columns' && <><path d="M6 5v14M12 5v14M18 5v14" /><path d="M4 19h16" /><path d="M5 9h2m4 4h2m4-5h2" /></>}
      {name === 'external' && <><path d="M14 5h5v5" /><path d="m19 5-8 8" /><path d="M19 13v5a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h5" /></>}
      {name === 'grid' && <><rect x="4" y="4" width="6" height="6" rx="1" /><rect x="14" y="4" width="6" height="6" rx="1" /><rect x="4" y="14" width="6" height="6" rx="1" /><rect x="14" y="14" width="6" height="6" rx="1" /></>}
      {name === 'hand' && <><path d="M7 12V7a1.5 1.5 0 0 1 3 0v4" /><path d="M10 11V5.5a1.5 1.5 0 0 1 3 0V11" /><path d="M13 11V7a1.5 1.5 0 0 1 3 0v5" /><path d="M16 12V9.5a1.5 1.5 0 0 1 3 0V15c0 3-2 5-5 5h-2c-2 0-3-1-4-3l-2-3a1.5 1.5 0 0 1 2.5-1.5L10 14" /></>}
      {name === 'letters' && <><path d="M5 18 10 6l5 12" /><path d="M7 14h6M14 6h5M14 10h4M14 14h5M14 18h4" /></>}
      {name === 'lock' && <><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /><path d="M12 14v2" /></>}
      {name === 'menu' && <><path d="M4 7h16M4 12h16M4 17h16" /></>}
      {name === 'pause' && <><path d="M8 5v14M16 5v14" /></>}
      {name === 'play' && <path d="m9 6 9 6-9 6V6Z" />}
      {name === 'plus' && <><circle cx="12" cy="12" r="8" /><path d="M12 8v8M8 12h8" /></>}
      {name === 'question' && <><circle cx="12" cy="12" r="8" /><path d="M9.8 9a2.3 2.3 0 1 1 3.6 1.9c-.9.6-1.4 1-1.4 2.1M12 16h.01" /></>}
      {name === 'search' && <><circle cx="10.8" cy="10.8" r="5.8" /><path d="m15 15 4 4" /></>}
      {name === 'smile' && <><circle cx="12" cy="12" r="8" /><path d="M9 14s1 2 3 2 3-2 3-2M9 10h.01M15 10h.01" /></>}
      {name === 'spark' && <><path d="m12 3 1.5 6.5L20 11l-6.5 1.5L12 19l-1.5-6.5L4 11l6.5-1.5L12 3Z" /><path d="m19 4 .5 2.5L22 7l-2.5.5L19 10l-.5-2.5L16 7l2.5-.5L19 4Z" /></>}
      {name === 'stones' && <><circle cx="8" cy="8" r="3" /><circle cx="16" cy="8" r="3" /><circle cx="8" cy="16" r="3" /><circle cx="16" cy="16" r="3" /></>}
      {name === 'sun' && <><circle cx="12" cy="12" r="3" /><path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6 7 7M17 17l1.4 1.4M18.4 5.6 17 7M7 17l-1.4 1.4" /></>}
      {name === 'target' && <><circle cx="12" cy="12" r="8" /><circle cx="12" cy="12" r="4" /><circle cx="12" cy="12" r="1" /></>}
      {name === 'type' && <><path d="M5 6h14M12 6v12M8 18h8" /><path d="M7 10h10" /></>}
      {name === 'up' && <><path d="M12 19V5M6 11l6-6 6 6" /><path d="M4 19h16" /></>}
      {name === 'x' && <><path d="m6 6 12 12M18 6 6 18" /></>}
    </svg>
  )
}

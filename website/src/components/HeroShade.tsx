import { useEffect, useState } from 'react'
import { Icon } from './Icon'

export function HeroShade() {
  const [expanded, setExpanded] = useState(false)
  const [score, setScore] = useState(8)
  const [arrived, setArrived] = useState(() => window.matchMedia('(prefers-reduced-motion: reduce)').matches)

  useEffect(() => {
    const enter = window.setTimeout(() => setArrived(true), 120)
    const tick = window.setInterval(() => setScore((value) => value >= 12 ? 8 : value + 1), 3200)
    return () => { window.clearTimeout(enter); window.clearInterval(tick) }
  }, [])

  return <div className={`hero-shade ${arrived ? 'is-arrived' : ''}`} aria-label="Conceptual Android notification shade visualization">
    <div className="hero-shade__status"><span>9:41</span><span>▮▮▮ ◇ 100%</span></div>
    <div className="hero-shade__quick"><span>Wi-Fi</span><span>Sound</span><span>Auto</span><span>⌁</span></div>
    <div className="hero-shade__rule" />
    <div className="hero-notification hero-notification--other"><span className="hero-notification__icon">N</span><div><b>Now playing</b><small>Something else can stay in front.</small></div><time>now</time></div>
    <div className={`hero-notification hero-notification--arcade ${expanded ? 'is-expanded' : ''}`}>
      <div className="hero-notification__head"><span className="hero-notification__icon hero-notification__icon--arcade"><Icon name="plus" /></span><div><b>AdBreak Arcade</b><small>Tap Counter · quiet channel</small></div><time>now</time></div>
      <div className="hero-notification__content"><div className="hero-notification__meta"><span>ROUND 04 / TARGET SPRINT</span><strong>{String(score).padStart(2, '0')} SCORE</strong></div><h3>Keep the streak alive.</h3><div className="hero-notification__meter"><span style={{ width: `${score * 7}%` }} /></div>{expanded && <div className="hero-notification__actions"><button type="button" onClick={() => setScore((value) => value + 1)}>TAP</button><button type="button" onClick={() => setScore(8)}>RESET</button><button type="button" onClick={() => setExpanded(false)}>COLLAPSE</button></div>}</div>
      <button className="hero-notification__expand" type="button" onClick={() => setExpanded((value) => !value)} aria-expanded={expanded}>{expanded ? 'Collapse notification' : 'Expand notification'} <Icon name="chevron" /></button>
    </div>
    <p className="hero-shade__caption"><span>CONCEPTUAL ANDROID UI</span> Outer shell and device presentation belong to Android System UI.</p>
  </div>
}

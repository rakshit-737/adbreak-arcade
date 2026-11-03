import { useReducer, useState } from 'react'
import { demoReducer, initialDemoState, mathRounds, type DemoMode } from '../demoState'
import { Icon } from './Icon'

type PreviewMode = 'compact' | 'expanded'
const modes: Array<{ id: DemoMode; label: string }> = [{ id: 'tap', label: 'Tap Counter' }, { id: 'math', label: 'Quick Math' }, { id: 'higher', label: 'Higher / Lower' }]

export function NotificationDemo() {
  const [state, dispatch] = useReducer(demoReducer, initialDemoState)
  const [previewMode, setPreviewMode] = useState<PreviewMode>('expanded')
  const [paused, setPaused] = useState(false)
  const math = mathRounds[state.mathIndex % mathRounds.length]
  const title = state.mode === 'tap' ? 'Tap Counter' : state.mode === 'math' ? 'Quick Math' : 'Higher / Lower'
  const objective = state.mode === 'tap' ? 'Build a ten-tap streak.' : state.mode === 'math' ? math.expression : `${state.higherLow} — ${state.higherHigh}`

  const action = (next: Parameters<typeof dispatch>[0]) => { if (!paused) dispatch(next) }

  return <section className="demo-shell" aria-label="Interactive conceptual website simulation of an AdBreak Arcade notification">
    <div className="demo-label"><span className="eyebrow">Website simulation</span><span><i /> Android notification concept</span></div>
    <div className="demo-toolbar"><div className="demo-tabs" role="tablist" aria-label="Choose demonstration game">{modes.map((mode) => <button key={mode.id} type="button" role="tab" aria-selected={state.mode === mode.id} className={state.mode === mode.id ? 'is-active' : ''} onClick={() => { setPaused(false); dispatch({ type: 'setMode', mode: mode.id }) }}>{mode.label}</button>)}</div><div className="demo-view"><button type="button" aria-pressed={previewMode === 'compact'} className={previewMode === 'compact' ? 'is-active' : ''} onClick={() => setPreviewMode('compact')}>Collapsed</button><button type="button" aria-pressed={previewMode === 'expanded'} className={previewMode === 'expanded' ? 'is-active' : ''} onClick={() => setPreviewMode('expanded')}>Expanded</button><button type="button" className="demo-pause" onClick={() => setPaused((value) => !value)}>{paused ? 'Resume' : 'Pause'}</button></div></div>
    <div className={`notification-card notification-card--${previewMode}`}>
      <div className="notification-card__head"><span className="notification-app-icon"><Icon name="plus" /></span><div><strong>AdBreak Arcade</strong><span>now · quiet channel</span></div><b>{state.feedback === 'Ready' ? 'READY' : 'PLAYING'}</b></div>
      <div className="notification-card__body"><div className="notification-card__meta"><span>{title}</span><strong>{String(state.score).padStart(2, '0')} SCORE</strong></div><h3>{objective}</h3><p>{paused ? 'Session paused. Resume when you are ready.' : state.feedback === 'Ready' ? 'Make one move from the notification.' : state.feedback}</p>{previewMode === 'expanded' && <><div className="demo-actions">{state.mode === 'tap' && <button type="button" className="demo-action demo-action--primary" disabled={paused} onClick={() => action({ type: 'tap' })}>TAP <small>{state.tapCount}</small></button>}{state.mode === 'math' && math.choices.map((choice) => <button type="button" className="demo-action" disabled={paused} key={choice} onClick={() => action({ type: 'mathAnswer', value: choice })}>{choice}</button>)}{state.mode === 'higher' && <><button type="button" className="demo-action" disabled={paused} onClick={() => action({ type: 'higherGuess', value: 'low' })}>LOW {state.higherLow}</button><button type="button" className="demo-action demo-action--primary" disabled={paused} onClick={() => action({ type: 'higherGuess', value: 'mid' })}>MID {Math.floor((state.higherLow + state.higherHigh) / 2)}</button><button type="button" className="demo-action" disabled={paused} onClick={() => action({ type: 'higherGuess', value: 'high' })}>HIGH {state.higherHigh}</button></>}</div><button className="demo-reset" type="button" onClick={() => { setPaused(false); dispatch({ type: 'reset' }) }}><Icon name="arrow" /> New round</button></>}</div>
    </div>
    <p className="demo-caption"><span>◎</span> Conceptual preview. Android controls the outer notification shell and device-specific presentation.</p>
  </section>
}

import { useReducer, useState } from 'react'
import { demoReducer, initialDemoState, mathRounds, type DemoMode } from '../demoState'
import { Icon } from './Icon'

type PreviewMode = 'compact' | 'expanded'

const modeLabels: Array<{ id: DemoMode; label: string }> = [
  { id: 'tap', label: 'Tap Counter' },
  { id: 'math', label: 'Quick Math' },
  { id: 'higher', label: 'Higher or Lower' },
]

export function NotificationDemo() {
  const [state, dispatch] = useReducer(demoReducer, initialDemoState)
  const [previewMode, setPreviewMode] = useState<PreviewMode>('expanded')
  const currentMath = mathRounds[state.mathIndex % mathRounds.length]

  return (
    <div className="demo-shell" aria-label="Interactive website demo of an AdBreak Arcade notification">
      <div className="demo-shell__topline">
        <span className="eyebrow eyebrow--quiet">WEBSITE DEMO</span>
        <span className="demo-live"><span className="live-dot" /> INTERACTIVE PREVIEW</span>
      </div>
      <div className="demo-toolbar">
        <div className="segmented-control" aria-label="Notification preview size">
          {(['compact', 'expanded'] as PreviewMode[]).map((value) => (
            <button
              key={value}
              type="button"
              className={previewMode === value ? 'is-active' : ''}
              aria-pressed={previewMode === value}
              onClick={() => setPreviewMode(value)}
            >
              {value}
            </button>
          ))}
        </div>
        <span className="mono-label">ANDROID NOTIFICATION</span>
      </div>

      <div className={`notification-preview notification-preview--${previewMode}`}>
        <div className="notification-preview__header">
          <span className="notification-app-icon"><Icon name="plus" /></span>
          <div>
            <strong>AdBreak Arcade</strong>
            <span>now · quiet channel</span>
          </div>
          <span className="notification-status">{state.feedback === 'Ready' ? 'READY' : 'PLAYING'}</span>
        </div>

        <div className="notification-preview__body">
          <div className="notification-preview__meta">
            <span className="mono-label">{state.mode === 'tap' ? 'TAP COUNTER' : state.mode === 'math' ? 'QUICK MATH' : 'HIGHER / LOWER'}</span>
            <span className="score-readout">{String(state.score).padStart(2, '0')} SCORE</span>
          </div>
          <h3>{state.mode === 'tap' ? 'Make one move.' : state.mode === 'math' ? currentMath.expression : `${state.higherLow} — ${state.higherHigh}`}</h3>
          <p>{state.feedback === 'Ready' ? 'The website version is independent from the Android notification.' : state.feedback}</p>

          {previewMode === 'expanded' && (
            <div className="demo-controls">
              <div className="demo-mode-tabs" role="tablist" aria-label="Choose website demo game">
                {modeLabels.map((mode) => (
                  <button
                    type="button"
                    role="tab"
                    aria-selected={state.mode === mode.id}
                    className={state.mode === mode.id ? 'is-selected' : ''}
                    key={mode.id}
                    onClick={() => dispatch({ type: 'setMode', mode: mode.id })}
                  >
                    {mode.label}
                  </button>
                ))}
              </div>
              <div className="demo-action-grid">
                {state.mode === 'tap' && (
                  <button className="demo-action demo-action--primary" type="button" onClick={() => dispatch({ type: 'tap' })}>
                    <Icon name="plus" /> Tap · {state.tapCount}
                  </button>
                )}
                {state.mode === 'math' && currentMath.choices.map((choice) => (
                  <button className="demo-action" type="button" key={choice} onClick={() => dispatch({ type: 'mathAnswer', value: choice })}>
                    {choice}
                  </button>
                ))}
                {state.mode === 'higher' && (
                  <>
                    <button className="demo-action" type="button" onClick={() => dispatch({ type: 'higherGuess', value: 'low' })}>LOW {state.higherLow}</button>
                    <button className="demo-action demo-action--primary" type="button" onClick={() => dispatch({ type: 'higherGuess', value: 'mid' })}>MID {Math.floor((state.higherLow + state.higherHigh) / 2)}</button>
                    <button className="demo-action" type="button" onClick={() => dispatch({ type: 'higherGuess', value: 'high' })}>HIGH {state.higherHigh}</button>
                  </>
                )}
              </div>
              <div className="demo-footer-actions">
                <button className="text-action" type="button" onClick={() => dispatch({ type: 'reset' })}><Icon name="arrow" /> New round</button>
                <span className="demo-limit">No app connection · no data leaves this page</span>
              </div>
            </div>
          )}
        </div>
      </div>
      <p className="demo-footnote"><span className="calibration-mark" /> Conceptual preview — Android controls the outer notification shell and device-specific presentation.</p>
    </div>
  )
}

import { describe, expect, it } from 'vitest'
import { demoReducer, initialDemoState, mathRounds } from './demoState'

describe('website demo reducer', () => {
  it('increments the tap counter and score', () => {
    const next = demoReducer(initialDemoState, { type: 'tap' })
    expect(next.tapCount).toBe(1)
    expect(next.score).toBe(1)
    expect(next.feedback).toContain('Nice')
  })

  it('advances math only after evaluating a valid answer', () => {
    const round = mathRounds[0]
    const correct = demoReducer({ ...initialDemoState, mode: 'math' }, { type: 'mathAnswer', value: round.answer })
    const incorrect = demoReducer({ ...initialDemoState, mode: 'math' }, { type: 'mathAnswer', value: round.choices[0] })
    expect(correct.score).toBe(1)
    expect(correct.mathIndex).toBe(1)
    expect(incorrect.score).toBe(0)
    expect(incorrect.mathIndex).toBe(1)
  })

  it('keeps higher-or-lower bounds meaningful after a guess', () => {
    const next = demoReducer({ ...initialDemoState, mode: 'higher' }, { type: 'higherGuess', value: 'mid' })
    expect(next.higherLow).toBeGreaterThanOrEqual(1)
    expect(next.higherHigh).toBeLessThanOrEqual(10)
    expect(next.higherLow).toBeLessThanOrEqual(next.higherHigh)
    expect(next.feedback).toMatch(/higher|lower|found/i)
  })

  it('reset preserves the selected demo mode', () => {
    const state = { ...initialDemoState, mode: 'math' as const, score: 4 }
    const next = demoReducer(state, { type: 'reset' })
    expect(next.mode).toBe('math')
    expect(next.score).toBe(0)
    expect(next.feedback).toBe('Ready')
  })
})

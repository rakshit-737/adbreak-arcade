import { describe, expect, it } from 'vitest'
import { arcadeReducer, createArcadeState } from './arcadeGames'

describe('browser arcade games', () => {
  it('scores a tap and records a milestone', () => {
    let state = createArcadeState('tap')
    for (let index = 0; index < 10; index += 1) state = arcadeReducer(state, { type: 'tap' })
    expect(state.score).toBe(10)
    expect(state.status).toContain('Streak')
  })

  it('blocks a tic-tac-toe move after the board is occupied', () => {
    const state = createArcadeState('tic-tac-toe')
    const next = arcadeReducer(state, { type: 'tic', index: 0 })
    expect(next.board.filter(Boolean).length).toBe(2)
    expect(arcadeReducer(next, { type: 'tic', index: 0 })).toEqual(next)
  })

  it('narrows higher or lower around the fixed secret', () => {
    const next = arcadeReducer(createArcadeState('higher'), { type: 'higher', guess: 5 })
    expect(next.low).toBe(6)
    expect(next.high).toBe(10)
  })

  it('requires the memory sequence in order', () => {
    let state = createArcadeState('memory')
    state = arcadeReducer(state, { type: 'memory', value: 1 })
    state = arcadeReducer(state, { type: 'memory', value: 3 })
    state = arcadeReducer(state, { type: 'memory', value: 2 })
    expect(state.score).toBe(1)
    expect(state.memoryRound).toBe(2)
  })

  it('solves a lights-out board with the final valid move', () => {
    let state = createArcadeState('lights-out')
    for (const index of [0, 1, 2, 3, 4, 5, 6, 7, 8]) state = arcadeReducer(state, { type: 'lights', index })
    expect(state.lights.every((light) => !light)).toBe(true)
    expect(state.score).toBe(1)
  })
})

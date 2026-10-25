export type DemoMode = 'tap' | 'math' | 'higher'

export type DemoState = {
  mode: DemoMode
  score: number
  feedback: string
  tapCount: number
  mathIndex: number
  higherLow: number
  higherHigh: number
  higherSecret: number
}

export type DemoAction =
  | { type: 'setMode'; mode: DemoMode }
  | { type: 'tap' }
  | { type: 'mathAnswer'; value: number }
  | { type: 'higherGuess'; value: 'mid' | 'low' | 'high' }
  | { type: 'reset' }

export const mathRounds = [
  { expression: '7 + 5', choices: [10, 11, 12, 13], answer: 12 },
  { expression: '8 × 4', choices: [24, 28, 32, 36], answer: 32 },
  { expression: '18 − 9', choices: [7, 8, 9, 10], answer: 9 },
]

export const initialDemoState: DemoState = {
  mode: 'tap',
  score: 0,
  feedback: 'Ready',
  tapCount: 0,
  mathIndex: 0,
  higherLow: 1,
  higherHigh: 10,
  higherSecret: 7,
}

export function demoReducer(state: DemoState, action: DemoAction): DemoState {
  switch (action.type) {
    case 'setMode':
      return { ...initialDemoState, mode: action.mode }
    case 'reset':
      return { ...initialDemoState, mode: state.mode }
    case 'tap':
      return {
        ...state,
        tapCount: state.tapCount + 1,
        score: state.score + 1,
        feedback: state.tapCount + 1 === 10 ? 'Milestone reached' : 'Nice. Keep going.',
      }
    case 'mathAnswer': {
      const round = mathRounds[state.mathIndex % mathRounds.length]
      const correct = action.value === round.answer
      return {
        ...state,
        score: correct ? state.score + 1 : state.score,
        mathIndex: state.mathIndex + 1,
        feedback: correct ? 'Correct — next prompt' : `Not quite — answer was ${round.answer}`,
      }
    }
    case 'higherGuess': {
      const midpoint = Math.floor((state.higherLow + state.higherHigh) / 2)
      const guess = action.value === 'mid' ? midpoint : action.value === 'low' ? state.higherLow : state.higherHigh
      if (guess === state.higherSecret) {
        return { ...state, score: state.score + 1, feedback: 'You found it — new round ready', higherLow: 1, higherHigh: 10 }
      }
      const nextLow = guess < state.higherSecret ? Math.max(state.higherLow, guess + 1) : state.higherLow
      const nextHigh = guess > state.higherSecret ? Math.min(state.higherHigh, guess - 1) : state.higherHigh
      return {
        ...state,
        higherLow: nextLow,
        higherHigh: nextHigh,
        feedback: guess < state.higherSecret ? 'Go higher' : 'Go lower',
      }
    }
  }
}

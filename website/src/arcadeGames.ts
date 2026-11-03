export type ArcadeGameId =
  | 'tap'
  | 'math'
  | 'tic-tac-toe'
  | 'higher'
  | 'memory'
  | 'rps'
  | 'lights-out'
  | 'sequence'

export type ArcadeState = {
  id: ArcadeGameId
  score: number
  status: string
  tapCount: number
  mathRound: number
  mathChoice: number | null
  board: Array<'X' | 'O' | null>
  turn: 'player' | 'computer'
  winner: 'X' | 'O' | 'draw' | null
  low: number
  high: number
  secret: number
  memorySequence: number[]
  memoryInput: number[]
  memoryRound: number
  rpsPlayer: string | null
  rpsComputer: string | null
  lights: boolean[]
  sequenceRound: number
}

export type ArcadeAction =
  | { type: 'reset'; id?: ArcadeGameId }
  | { type: 'tap' }
  | { type: 'math'; choice: number }
  | { type: 'tic'; index: number }
  | { type: 'higher'; guess: number }
  | { type: 'memory'; value: number }
  | { type: 'rps'; choice: string }
  | { type: 'lights'; index: number }
  | { type: 'sequence'; choice: number }

export const mathRounds = [
  { prompt: '7 + 5', choices: [10, 11, 12, 13], answer: 12 },
  { prompt: '8 × 4', choices: [24, 28, 32, 36], answer: 32 },
  { prompt: '18 − 9', choices: [7, 8, 9, 10], answer: 9 },
]

export const sequenceRounds = [
  { shown: [2, 4, 6, 8], choices: [9, 10, 11, 12], answer: 10 },
  { shown: [3, 6, 9, 12], choices: [14, 15, 16, 18], answer: 15 },
  { shown: [1, 4, 9, 16], choices: [20, 24, 25, 27], answer: 25 },
]

const winningLines = [[0, 1, 2], [3, 4, 5], [6, 7, 8], [0, 3, 6], [1, 4, 7], [2, 5, 8], [0, 4, 8], [2, 4, 6]]

export function createArcadeState(id: ArcadeGameId = 'tap'): ArcadeState {
  return {
    id, score: 0, status: 'Ready when you are.', tapCount: 0, mathRound: 0, mathChoice: null,
    board: Array(9).fill(null), turn: 'player', winner: null, low: 1, high: 10, secret: 7,
    memorySequence: [1, 3, 2], memoryInput: [], memoryRound: 1, rpsPlayer: null, rpsComputer: null,
    lights: [true, false, true, false, true, false, true, false, true], sequenceRound: 0,
  }
}

function boardWinner(board: ArcadeState['board']): 'X' | 'O' | 'draw' | null {
  for (const [a, b, c] of winningLines) if (board[a] && board[a] === board[b] && board[a] === board[c]) return board[a]
  return board.every(Boolean) ? 'draw' : null
}

function computerMove(board: ArcadeState['board']): number | null {
  const open = board.map((cell, index) => cell ? -1 : index).filter((index) => index >= 0)
  if (!open.length) return null
  for (const index of open) {
    const test = [...board]; test[index] = 'O'
    if (boardWinner(test) === 'O') return index
  }
  for (const index of open) {
    const test = [...board]; test[index] = 'X'
    if (boardWinner(test) === 'X') return index
  }
  return open.includes(4) ? 4 : open[0]
}

export function arcadeReducer(state: ArcadeState, action: ArcadeAction): ArcadeState {
  if (action.type === 'reset') return createArcadeState(action.id ?? state.id)
  switch (action.type) {
    case 'tap': {
      const count = state.tapCount + 1
      return { ...state, tapCount: count, score: state.score + 1, status: count % 10 === 0 ? 'Streak marker reached.' : 'Good. Keep the rhythm.' }
    }
    case 'math': {
      const round = mathRounds[state.mathRound % mathRounds.length]
      const correct = action.choice === round.answer
      return { ...state, score: state.score + (correct ? 1 : 0), mathRound: state.mathRound + 1, mathChoice: action.choice, status: correct ? 'Correct. Next prompt.' : `The answer was ${round.answer}.` }
    }
    case 'tic': {
      if (state.winner || state.turn !== 'player' || state.board[action.index]) return state
      const playerBoard = [...state.board]; playerBoard[action.index] = 'X'
      const playerWinner = boardWinner(playerBoard)
      if (playerWinner) return { ...state, board: playerBoard, winner: playerWinner, score: playerWinner === 'X' ? state.score + 1 : state.score, status: playerWinner === 'draw' ? 'Board locked. Draw.' : 'You got three.' }
      const move = computerMove(playerBoard)
      if (move === null) return { ...state, board: playerBoard, winner: 'draw', status: 'Board locked. Draw.' }
      const computerBoard = [...playerBoard]; computerBoard[move] = 'O'
      const computerWinner = boardWinner(computerBoard)
      return { ...state, board: computerBoard, turn: 'player', winner: computerWinner, status: computerWinner === 'O' ? 'The arcade found a line.' : 'Your move. Find the line.' }
    }
    case 'higher': {
      if (action.guess === state.secret) return { ...state, score: state.score + 1, low: 1, high: 10, status: 'Found it. New range ready.' }
      const nextLow = action.guess < state.secret ? Math.max(state.low, action.guess + 1) : state.low
      const nextHigh = action.guess > state.secret ? Math.min(state.high, action.guess - 1) : state.high
      return { ...state, low: nextLow, high: nextHigh, status: action.guess < state.secret ? 'Go higher.' : 'Go lower.' }
    }
    case 'memory': {
      const input = [...state.memoryInput, action.value]
      const expected = state.memorySequence[input.length - 1]
      if (action.value !== expected) return { ...state, memoryInput: [], status: 'Sequence broken. Reset and try again.' }
      if (input.length === state.memorySequence.length) return { ...state, score: state.score + 1, memoryInput: [], memoryRound: state.memoryRound + 1, memorySequence: [...state.memorySequence, (state.memorySequence.length % 4) + 1], status: 'Sequence complete. It grows.' }
      return { ...state, memoryInput: input, status: `${input.length} of ${state.memorySequence.length} matched.` }
    }
    case 'rps': {
      const options = ['rock', 'paper', 'scissors']; const computer = options[(state.score + 1) % options.length]
      const win = action.choice !== computer && ((action.choice === 'rock' && computer === 'scissors') || (action.choice === 'paper' && computer === 'rock') || (action.choice === 'scissors' && computer === 'paper'))
      return { ...state, rpsPlayer: action.choice, rpsComputer: computer, score: state.score + (win ? 1 : 0), status: action.choice === computer ? 'Draw. Again?' : win ? 'You win the round.' : 'The arcade wins this one.' }
    }
    case 'lights': {
      const next = [...state.lights]; const row = Math.floor(action.index / 3); const col = action.index % 3
      for (const [r, c] of [[row, col], [row - 1, col], [row + 1, col], [row, col - 1], [row, col + 1]]) if (r >= 0 && r < 3 && c >= 0 && c < 3) next[r * 3 + c] = !next[r * 3 + c]
      const won = next.every((light) => !light)
      return { ...state, lights: next, score: state.score + (won ? 1 : 0), status: won ? 'All dark. You solved it.' : 'The board shifts with you.' }
    }
    case 'sequence': {
      const round = sequenceRounds[state.sequenceRound % sequenceRounds.length]; const correct = action.choice === round.answer
      return { ...state, score: state.score + (correct ? 1 : 0), sequenceRound: state.sequenceRound + 1, status: correct ? 'Pattern confirmed.' : `Not this time. It was ${round.answer}.` }
    }
  }
}

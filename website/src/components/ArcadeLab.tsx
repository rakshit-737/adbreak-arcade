import { useReducer, useState, type Dispatch } from 'react'
import { arcadeReducer, createArcadeState, mathRounds, sequenceRounds, type ArcadeGameId } from '../arcadeGames'
import { Icon } from './Icon'

type LabGame = { id: ArcadeGameId; title: string; family: string; signal: string }
const labGames: LabGame[] = [
  { id: 'tap', title: 'Tap Counter', family: 'Pulse', signal: 'ONE CONTROL' },
  { id: 'math', title: 'Quick Math', family: 'Logic', signal: 'FOUR ANSWERS' },
  { id: 'tic-tac-toe', title: 'Tic-Tac-Toe', family: 'Board', signal: '3 × 3 BOARD' },
  { id: 'higher', title: 'Higher / Lower', family: 'Logic', signal: 'FIVE ATTEMPTS' },
  { id: 'memory', title: 'Memory Challenge', family: 'Focus', signal: 'SEQUENCE' },
  { id: 'rps', title: 'Rock Paper Scissors', family: 'Pulse', signal: 'THREE MOVES' },
  { id: 'lights-out', title: 'Lights Out Mini', family: 'Board', signal: '3 × 3 GRID' },
  { id: 'sequence', title: 'Number Sequence', family: 'Logic', signal: 'PATTERN' },
]

export function ArcadeLab() {
  const [game, setGame] = useState<ArcadeGameId>('tap')
  const [state, dispatch] = useReducer(arcadeReducer, game, createArcadeState)
  const selected = labGames.find((item) => item.id === game) ?? labGames[0]

  const choose = (id: ArcadeGameId) => { setGame(id); dispatch({ type: 'reset', id }) }
  return <section className="arcade-lab" aria-label="Playable browser arcade">
    <div className="arcade-lab__rail"><div><span className="eyebrow">Browser cabinet / live logic</span><h3>Pick a cabinet.</h3></div><div className="arcade-score"><span>RUN SCORE</span><strong>{String(state.score).padStart(2, '0')}</strong></div></div>
    <div className="arcade-lab__body">
      <aside className="cabinet-list" aria-label="Browser games">{labGames.map((item, index) => <button key={item.id} type="button" className={game === item.id ? 'is-selected' : ''} aria-pressed={game === item.id} onClick={() => choose(item.id)} data-cursor="play"><span>{String(index + 1).padStart(2, '0')}</span><strong>{item.title}</strong><small>{item.family}</small></button>)}</aside>
      <div className="cabinet-stage"><div className="cabinet-stage__top"><span>{selected.family} / {selected.signal}</span><button type="button" onClick={() => dispatch({ type: 'reset', id: game })} data-cursor="reset"><Icon name="arrow" /> New run</button></div><div className="cabinet-screen"><GameBoard state={state} dispatch={dispatch} /></div><div className="cabinet-stage__status"><span className="status-dot" /> {state.status}<span className="cabinet-stage__note">Browser simulation · Android version uses notification controls</span></div></div>
    </div>
  </section>
}

function GameBoard({ state, dispatch }: { state: ReturnType<typeof createArcadeState>; dispatch: Dispatch<Parameters<typeof arcadeReducer>[1]> }) {
  switch (state.id) {
    case 'tap': return <div className="game-board game-board--tap"><div className="game-board__eyebrow">TAP COUNTER / TARGET 10</div><strong className="tap-count">{state.tapCount}</strong><button type="button" className="cabinet-action cabinet-action--large" onClick={() => dispatch({ type: 'tap' })} data-cursor="tap">TAP <small>+1 SCORE</small></button></div>
    case 'math': { const round = mathRounds[state.mathRound % mathRounds.length]; return <div className="game-board"><div className="game-board__eyebrow">QUICK MATH / SOLVE</div><strong className="prompt">{round.prompt}</strong><div className="choice-grid">{round.choices.map((choice) => <button type="button" className="cabinet-action" key={choice} onClick={() => dispatch({ type: 'math', choice })}>{choice}</button>)}</div></div> }
    case 'tic-tac-toe': return <div className="game-board"><div className="game-board__eyebrow">TIC-TAC-TOE / YOU ARE X</div><div className="tic-board">{state.board.map((cell, index) => <button type="button" key={index} className="tic-cell" disabled={Boolean(cell) || Boolean(state.winner)} onClick={() => dispatch({ type: 'tic', index })}>{cell ?? ''}</button>)}</div><p className="game-board__hint">{state.winner ? 'Start a new run to play again.' : 'Place an X. The arcade answers with O.'}</p></div>
    case 'higher': return <div className="game-board"><div className="game-board__eyebrow">HIGHER / LOWER / FIND THE SIGNAL</div><strong className="prompt">{state.low} — {state.high}</strong><div className="choice-grid choice-grid--three">{[state.low, Math.floor((state.low + state.high) / 2), state.high].filter((value, index, values) => values.indexOf(value) === index).map((guess) => <button type="button" className="cabinet-action" key={guess} onClick={() => dispatch({ type: 'higher', guess })}>{guess}</button>)}</div></div>
    case 'memory': return <div className="game-board"><div className="game-board__eyebrow">MEMORY / ROUND {state.memoryRound}</div><strong className="prompt prompt--sequence">{state.memorySequence.map((value, index) => index < state.memoryInput.length ? value : '·').join('  ')}</strong><div className="choice-grid choice-grid--four">{[1, 2, 3, 4].map((value) => <button type="button" className="cabinet-action" key={value} onClick={() => dispatch({ type: 'memory', value })}>{value}</button>)}</div><p className="game-board__hint">Repeat the sequence in order.</p></div>
    case 'rps': return <div className="game-board"><div className="game-board__eyebrow">ROCK / PAPER / SCISSORS</div><strong className="prompt">{state.rpsPlayer ? `${state.rpsPlayer} vs ${state.rpsComputer}` : 'Choose your move.'}</strong><div className="choice-grid choice-grid--three">{['rock', 'paper', 'scissors'].map((choice) => <button type="button" className="cabinet-action" key={choice} onClick={() => dispatch({ type: 'rps', choice })}>{choice}</button>)}</div></div>
    case 'lights-out': return <div className="game-board"><div className="game-board__eyebrow">LIGHTS OUT / MAKE IT DARK</div><div className="lights-board">{state.lights.map((light, index) => <button type="button" key={index} className={`light-cell ${light ? 'is-on' : ''}`} aria-label={`Light ${index + 1} ${light ? 'on' : 'off'}`} onClick={() => dispatch({ type: 'lights', index })} />)}</div><p className="game-board__hint">Tap a cell to flip it and its neighbours.</p></div>
    case 'sequence': { const round = sequenceRounds[state.sequenceRound % sequenceRounds.length]; return <div className="game-board"><div className="game-board__eyebrow">NUMBER SEQUENCE / WHAT NEXT?</div><strong className="prompt prompt--sequence">{round.shown.join('  ')}  ?</strong><div className="choice-grid">{round.choices.map((choice) => <button type="button" className="cabinet-action" key={choice} onClick={() => dispatch({ type: 'sequence', choice })}>{choice}</button>)}</div></div> }
  }
}

import { useMemo, useState } from 'react'
import { games, type GameCategory } from '../content'
import { Icon, type IconName } from './Icon'

const categories: Array<'All' | GameCategory> = ['All', 'Action', 'Logic', 'Words & knowledge', 'Board']

export function GameShelf() {
  const [category, setCategory] = useState<'All' | GameCategory>('All')
  const [query, setQuery] = useState('')
  const visibleGames = useMemo(() => games.filter((game) => {
    const matchesCategory = category === 'All' || game.category === category
    return matchesCategory && `${game.title} ${game.description} ${game.signal}`.toLowerCase().includes(query.toLowerCase().trim())
  }), [category, query])

  return <div className="shelf-wrap"><div className="shelf-tools"><div className="filter-pills" aria-label="Filter game catalogue">{categories.map((item) => <button key={item} type="button" className={category === item ? 'is-active' : ''} aria-pressed={category === item} onClick={() => setCategory(item)}>{item}</button>)}</div><label className="search-field"><Icon name="search" /><span className="sr-only">Search games</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by name or mechanic" /></label></div><p className="shelf-count" aria-live="polite">Showing <strong>{visibleGames.length}</strong> of {games.length} games</p><div className="game-shelf" aria-live="polite">{visibleGames.map((game, index) => <GameTile key={game.id} {...game} index={index} />)}</div>{visibleGames.length === 0 && <div className="empty-state"><Icon name="search" /><strong>No game matches that search.</strong><button type="button" onClick={() => { setQuery(''); setCategory('All') }}>Clear filters</button></div>}<p className="shelf-note"><span className="eyebrow">Inventory</span> Sourced from the 25 factories registered in <code>GameRegistry</code>.</p></div>
}

type GameTileProps = { id: string; title: string; category: GameCategory; description: string; signal: string; icon: string; index: number }
function GameTile({ title, category, description, signal, icon, index }: GameTileProps) {
  return <article className={`game-tile game-tile--${index % 4}`}><div className="game-tile__topline"><span className="game-tile__number">{String(index + 1).padStart(2, '0')}</span><span className="game-tile__icon"><Icon name={icon as IconName} /></span><span className="game-tile__signal">{signal}</span></div><div className="game-tile__body"><p className="game-tile__category">{category}</p><h3>{title}</h3><p>{description}</p></div><div className="game-tile__footer"><span>OFFLINE / READY</span><span>↗</span></div></article>
}

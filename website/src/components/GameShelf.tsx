import { useMemo, useState } from 'react'
import { games, type GameCategory } from '../content'
import { Icon, type IconName } from './Icon'

const categories: Array<'All' | GameCategory> = ['All', 'Action', 'Logic', 'Words & knowledge', 'Board']

export function GameShelf() {
  const [category, setCategory] = useState<'All' | GameCategory>('All')
  const [query, setQuery] = useState('')
  const visibleGames = useMemo(() => games.filter((game) => {
    const matchesCategory = category === 'All' || game.category === category
    const haystack = `${game.title} ${game.description} ${game.signal}`.toLowerCase()
    return matchesCategory && haystack.includes(query.toLowerCase().trim())
  }), [category, query])

  return (
    <div className="shelf-wrap">
      <div className="shelf-tools">
        <div className="filter-pills" aria-label="Filter game shelf">
          {categories.map((item) => (
            <button key={item} type="button" className={category === item ? 'is-active' : ''} onClick={() => setCategory(item)} aria-pressed={category === item}>
              {item}
            </button>
          ))}
        </div>
        <label className="search-field">
          <Icon name="search" />
          <span className="sr-only">Search games</span>
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search the shelf" />
        </label>
      </div>
      <div className="game-shelf" aria-live="polite">
        {visibleGames.map((game, index) => <GameTile key={game.id} {...game} index={index} />)}
      </div>
      {visibleGames.length === 0 && (
        <div className="empty-state">
          <Icon name="search" />
          <strong>No game matches that signal.</strong>
          <button type="button" onClick={() => { setQuery(''); setCategory('All') }}>Clear filters</button>
        </div>
      )}
      <p className="shelf-note"><span className="mono-label">INVENTORY</span> The shelf reflects the 17 factories registered in <code>GameRegistry</code>.</p>
    </div>
  )
}

type GameTileProps = {
  id: string
  title: string
  category: GameCategory
  description: string
  signal: string
  icon: string
  index: number
}

function GameTile({ title, category, description, signal, icon, index }: GameTileProps) {
  const iconName = icon as IconName
  return (
    <article className={`game-tile game-tile--${index % 5}`}>
      <div className="game-tile__topline">
        <span className="game-tile__icon"><Icon name={iconName} /></span>
        <span className="mono-label">{String(index + 1).padStart(2, '0')}</span>
      </div>
      <div className="game-tile__body">
        <div className="game-tile__title-row">
          <h3>{title}</h3>
          <span className="game-signal">{signal}</span>
        </div>
        <p>{description}</p>
      </div>
      <div className="game-tile__footer"><span>{category}</span><span className="ready-mark"><span /> READY</span></div>
    </article>
  )
}

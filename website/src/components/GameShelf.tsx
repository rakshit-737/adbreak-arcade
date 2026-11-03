import { useEffect, useMemo, useState } from 'react'
import { games, type Game, type GameCategory } from '../content'
import { Icon, type IconName } from './Icon'

const categories: Array<'All' | GameCategory> = ['All', 'Action', 'Logic', 'Words & knowledge', 'Board']
const familyFor = (game: Game) => game.category === 'Action' ? 'Pulse' : game.category === 'Board' ? 'Board' : game.category === 'Words & knowledge' ? 'Words' : 'Logic'

export function GameShelf() {
  const [category, setCategory] = useState<'All' | GameCategory>('All')
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<Game | null>(null)
  const [favorites, setFavorites] = useState<string[]>([])
  const visibleGames = useMemo(() => games.filter((game) => {
    const matchesCategory = category === 'All' || game.category === category
    return matchesCategory && `${game.title} ${game.description} ${game.signal} ${familyFor(game)}`.toLowerCase().includes(query.toLowerCase().trim())
  }), [category, query])

  useEffect(() => {
    if (!selected) return
    const close = (event: KeyboardEvent) => { if (event.key === 'Escape') setSelected(null) }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [selected])

  const toggleFavorite = (id: string) => setFavorites((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id])
  return <div className="shelf-wrap"><div className="shelf-tools"><div className="filter-pills" aria-label="Filter game catalogue">{categories.map((item) => <button key={item} type="button" className={category === item ? 'is-active' : ''} aria-pressed={category === item} onClick={() => setCategory(item)}>{item}</button>)}</div><label className="search-field"><Icon name="search" /><span className="sr-only">Search games</span><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search by name or mechanic" /></label></div><div className="shelf-count" aria-live="polite"><strong>{visibleGames.length}</strong> visible / {games.length} registered <span>· {favorites.length} local favourites in this demo</span></div><div className="game-shelf" aria-live="polite">{visibleGames.map((game, index) => <GameTile key={game.id} {...game} index={index} favorite={favorites.includes(game.id)} onPreview={() => setSelected(game)} onFavorite={() => toggleFavorite(game.id)} />)}</div>{visibleGames.length === 0 && <div className="empty-state"><Icon name="search" /><strong>No game matches that search.</strong><button type="button" onClick={() => { setQuery(''); setCategory('All') }}>Clear filters</button></div>}<p className="shelf-note"><span className="eyebrow">Inventory</span> Sourced from the 25 factories registered in <code>GameRegistry</code>. Select a tile for a playable-style preview.</p>{selected && <GamePreview game={selected} favorite={favorites.includes(selected.id)} onClose={() => setSelected(null)} onFavorite={() => toggleFavorite(selected.id)} />}</div>
}

type GameTileProps = Game & { index: number; favorite: boolean; onPreview: () => void; onFavorite: () => void }
function GameTile({ title, category, description, signal, icon, index, favorite, onPreview, onFavorite }: GameTileProps) {
  return <article className={`game-tile game-tile--${index % 4}`}><button className="game-tile__open" type="button" onClick={onPreview} data-cursor="preview"><span className="game-tile__topline"><span className="game-tile__number">{String(index + 1).padStart(2, '0')}</span><span className="game-tile__icon"><Icon name={icon as IconName} /></span><span className="game-tile__signal">{signal}</span></span><span className="game-tile__body"><span className="game-tile__category">{category} / {familyFor({ title, category, description, signal, icon, id: '' })}</span><strong>{title}</strong><span>{description}</span></span><span className="game-tile__footer"><span>OFFLINE / READY</span><span>VIEW ↗</span></span></button><button className={`game-tile__favorite ${favorite ? 'is-favorite' : ''}`} type="button" aria-label={`${favorite ? 'Remove' : 'Add'} ${title} ${favorite ? 'from' : 'to'} demo favourites`} onClick={onFavorite}>{favorite ? '★' : '☆'}</button></article>
}

function GamePreview({ game, favorite, onClose, onFavorite }: { game: Game; favorite: boolean; onClose: () => void; onFavorite: () => void }) {
  return <div className="preview-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><section className="game-preview" role="dialog" aria-modal="true" aria-labelledby="game-preview-title"><button className="preview-close" type="button" onClick={onClose} aria-label="Close game preview"><Icon name="x" /></button><div className="preview-art"><span className="preview-art__grid" /><span className="preview-art__glyph">{game.icon === 'plus' ? '+' : game.icon === 'board' ? '▦' : game.icon === 'lock' ? '◇' : '◌'}</span><span className="preview-art__label">{game.signal}</span></div><div className="preview-copy"><p className="eyebrow">{game.category} / {familyFor(game)} family</p><h2 id="game-preview-title">{game.title}</h2><p>{game.description}</p><div className="preview-meta"><span>OBJECTIVE</span><strong>{game.signal}</strong></div><div className="preview-meta"><span>INPUT</span><strong>NOTIFICATION BUTTONS</strong></div><div className="preview-actions"><button className={`preview-favorite ${favorite ? 'is-favorite' : ''}`} type="button" onClick={onFavorite}>{favorite ? '★ Saved in demo' : '☆ Add to demo shelf'}</button><button className="button button--primary" type="button" onClick={onClose}>Back to shelf <Icon name="arrow" /></button></div></div></section></div>
}

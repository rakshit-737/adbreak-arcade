import { useEffect, useState } from 'react'
import { architectureSteps, faq, games, stats } from './content'
import { BrandMark } from './components/BrandMark'
import { FaqList } from './components/FaqList'
import { GameShelf } from './components/GameShelf'
import { Icon } from './components/Icon'
import { NotificationDemo } from './components/NotificationDemo'

const repositoryUrl = 'https://github.com/rakshit-737/adbreak-arcade'
const releaseUrl = 'https://github.com/rakshit-737/adbreak-arcade/releases/tag/v1.6.0'
const apkUrl = 'https://github.com/rakshit-737/adbreak-arcade/releases/download/v1.6.0/adbreak-arcade-v1.6.0-debug.apk'

const navItems = [
  { label: 'Product', href: '#product' },
  { label: 'Games', href: '#games' },
  { label: 'How it works', href: '#how-it-works' },
  { label: 'Download', href: '#download' },
]

export default function App() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [theme, setTheme] = useState<'light' | 'dark'>(() => {
    const saved = window.localStorage.getItem('adbreak-theme')
    if (saved === 'light' || saved === 'dark') return saved
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  })

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    window.localStorage.setItem('adbreak-theme', theme)
  }, [theme])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => { if (event.key === 'Escape') setMenuOpen(false) }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  const closeMenu = () => setMenuOpen(false)

  return (
    <div className="site-shell">
      <a className="skip-link" href="#main-content">Skip to content</a>
      <header className="site-header">
        <div className="site-container site-header__inner">
          <a className="brand-link" href="#top" onClick={closeMenu} aria-label="AdBreak Arcade home"><BrandMark /></a>
          <nav id="primary-navigation" className={`site-nav ${menuOpen ? 'is-open' : ''}`} aria-label="Primary navigation">
            {navItems.map((item) => <a key={item.href} href={item.href} onClick={closeMenu}>{item.label}</a>)}
            <a href={repositoryUrl} target="_blank" rel="noreferrer"><Icon name="external" /> GitHub</a>
            <a className="nav-download" href={apkUrl}>Test APK <Icon name="arrow" /></a>
          </nav>
          <div className="header-actions">
            <button className="theme-toggle" type="button" onClick={() => setTheme((value) => value === 'dark' ? 'light' : 'dark')} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}>
              <span aria-hidden="true">{theme === 'dark' ? '☼' : '◐'}</span>
            </button>
            <button className="menu-toggle" type="button" aria-expanded={menuOpen} aria-controls="primary-navigation" onClick={() => setMenuOpen((value) => !value)}>
              <span className="sr-only">{menuOpen ? 'Close menu' : 'Open menu'}</span><Icon name={menuOpen ? 'x' : 'menu'} />
            </button>
          </div>
        </div>
      </header>

      <main id="main-content">
        <section id="top" className="hero-section site-container">
          <div className="hero-grid">
            <div className="hero-copy">
              <p className="eyebrow"><span className="eyebrow__mark" /> Native Android / 25 offline games</p>
              <h1>Your next game is already in the <em>notification shade.</em></h1>
              <p className="hero-lede">Twenty-five small offline games. One expanded Android notification. Play while the rest of your phone stays yours.</p>
              <div className="hero-actions"><a className="button button--primary" href="#games">Explore the games <Icon name="arrow" /></a><a className="button button--secondary" href={apkUrl}>Download testing APK <Icon name="arrow" /></a></div>
              <p className="release-status"><span className="status-dot" /> v1.6.0 testing release · <a href={releaseUrl}>view release notes</a></p>
            </div>
            <div className="hero-product"><NotificationDemo /></div>
          </div>
          <div className="hero-index" aria-hidden="true"><span>01</span><span>ADBREAK / 2026</span><span>ANDROID SYSTEM UI</span></div>
        </section>

        <section className="fact-rail" aria-label="Project facts"><div className="site-container fact-rail__inner">{stats.map((stat) => <div className="fact" key={stat.label}><strong>{stat.value}</strong><span>{stat.label}</span></div>)}<div className="fact fact--wide"><span className="eyebrow">No account · no backend · no network permission</span><span>Built for the two minutes between things.</span></div></div></section>

        <section id="product" className="section section--product site-container">
          <div className="section-intro section-intro--split"><div><p className="eyebrow">The product</p><h2>A handheld arcade that lives <em>between apps.</em></h2></div><p>AdBreak Arcade does one unusual thing carefully: it puts short game decisions inside the expanded notification you already know how to use.</p></div>
          <div className="product-grid"><div className="product-note"><span className="note-index">A / 01</span><h3>Small enough to keep open.</h3><p>Start a run, switch back to your other app, and return to the shade when you have a moment. The game state and score remain on the device.</p><span className="rule-line" /></div><div className="product-note product-note--dark"><span className="note-index">B / 02</span><h3>Native enough to feel honest.</h3><p>Android owns the outer shell. The app supplies RemoteViews-safe controls, clear state, and session-bound actions.</p><span className="rule-line" /></div><div className="product-note"><span className="note-index">C / 03</span><h3>Quiet by default.</h3><p>No account, analytics, ad detection, overlay, or network permission. Smart App Triggers stay off until you explicitly configure them.</p><span className="rule-line" /></div></div>
        </section>

        <section id="how-it-works" className="section section--how site-container"><div className="section-intro section-intro--split"><div><p className="eyebrow">The loop</p><h2>Three moves from idle to <em>occupied.</em></h2></div><p>The app stays out of the way. Choose once, start once, then make ordinary notification moves while your current screen remains in front.</p></div><div className="workflow"><WorkflowStep index="01" title="Choose a game" detail="Open the shelf and pick a one-minute mechanic." /><WorkflowStep index="02" title="Start the notification" detail="Tap Start notification, then switch back to what you were doing." /><WorkflowStep index="03" title="Expand and play" detail="Pull down the shade and use the visible controls. Reset or stop any time." /></div></section>

        <section id="demo" className="section section--demo site-container"><div className="section-intro section-intro--split"><div><p className="eyebrow">Try the rhythm</p><h2>The notification is the <em>game board.</em></h2></div><p>This is a working website simulation, not a live Android notification. Switch examples, collapse the card, make moves, and watch the state change.</p></div><NotificationDemo /></section>

        <section id="games" className="section section--games site-container"><div className="section-intro section-intro--split"><div><p className="eyebrow">The collection / {games.length} games</p><h2>Curated for the <em>small break.</em></h2></div><p>Every title in the catalogue is registered by the Android GameRegistry. Search by name or mechanic, then choose the kind of attention you have.</p></div><GameShelf /></section>

        <section className="section section--privacy site-container"><div className="privacy-layout"><div><p className="eyebrow">Privacy, plainly</p><h2>Your game state stays <em>on the device.</em></h2><p className="body-copy">Manual notification play is local and offline. Optional Smart App Triggers are a separate, disabled-by-default capability: they use Android Usage access to observe selected apps' foreground transitions, never their content.</p></div><div className="privacy-board"><div><span>ACCOUNT</span><strong>NONE</strong></div><div><span>NETWORK</span><strong>NOT DECLARED</strong></div><div><span>TRIGGERS</span><strong>OPT-IN</strong></div><div><span>AD DETECTION</span><strong>NO</strong></div><p>Best-effort lifecycle polling. No forced shade opening.</p></div></div></section>

        <section id="technology" className="section section--technology site-container"><div className="section-intro section-intro--split"><div><p className="eyebrow">Under the card</p><h2>Native all the way down to the <em>action.</em></h2></div><p>The architecture keeps game rules independent from the platform surface so every move can be checked, persisted, and rendered safely.</p></div><div className="architecture-map">{architectureSteps.map((step, index) => <div className="architecture-node" key={step.label}><span>{String(index + 1).padStart(2, '0')}</span><strong>{step.label}</strong><code>{step.detail}</code></div>)}</div></section>

        <section id="download" className="section section--download site-container"><div className="download-panel"><div><p className="eyebrow">Put it on a device</p><h2>Build it. Test it. <em>Then play.</em></h2><p>v1.6.0 is a testing release. The debug APK is installable and signed with the standard Android debug key; the minified release APK is unsigned for validation.</p></div><div className="download-actions"><a className="button button--primary" href={apkUrl}>Download debug APK <Icon name="arrow" /></a><a className="button button--secondary" href={releaseUrl} target="_blank" rel="noreferrer">Release assets <Icon name="external" /></a><span>Testing artifact · not Play Store distribution</span></div></div></section>

        <section className="section section--faq site-container"><div className="section-intro section-intro--split"><div><p className="eyebrow">Clear edges</p><h2>Questions before the <em>first tap.</em></h2></div><p>The short answers are the product boundary: notification controls, local state, opt-in triggers, and the things Android still owns.</p></div><FaqList items={faq} /></section>
      </main>

      <footer className="site-footer"><div className="site-container footer-grid"><div><BrandMark /><p>Short games for the spaces between things.</p></div><div><span className="eyebrow">Project</span><a href={repositoryUrl} target="_blank" rel="noreferrer">GitHub</a><a href={releaseUrl} target="_blank" rel="noreferrer">Releases</a></div><div><span className="eyebrow">Notes</span><a href={`${repositoryUrl}/blob/main/README.md`} target="_blank" rel="noreferrer">Documentation</a><a href={`${repositoryUrl}/blob/main/SECURITY.md`} target="_blank" rel="noreferrer">Security</a></div></div><div className="site-container footer-bottom"><span>ADBREAK ARCADE · APACHE 2.0 · v1.6.0</span><a href="#top">Back to top ↑</a></div></footer>
    </div>
  )
}

function WorkflowStep({ index, title, detail }: { index: string; title: string; detail: string }) {
  return <article className="workflow-step"><span>{index}</span><div><h3>{title}</h3><p>{detail}</p></div><Icon name="arrow" /></article>
}

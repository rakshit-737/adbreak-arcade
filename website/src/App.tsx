import { useEffect, useState } from 'react'
import { architectureSteps, games, stats } from './content'
import { BrandMark } from './components/BrandMark'
import { FaqList } from './components/FaqList'
import { GameShelf } from './components/GameShelf'
import { Icon } from './components/Icon'
import { NotificationDemo } from './components/NotificationDemo'
import { Reveal } from './components/Reveal'

const repositoryUrl = 'https://github.com/rakshit-737/adbreak-arcade'
const releaseUrl = 'https://github.com/rakshit-737/adbreak-arcade/releases/tag/v1.2.0'
const apkUrl = 'https://github.com/rakshit-737/adbreak-arcade/releases/download/v1.2.0/app-debug.apk'

const navItems = [
  { label: 'Features', href: '#features' },
  { label: 'Games', href: '#games' },
  { label: 'How it works', href: '#how-it-works' },
  { label: 'Technology', href: '#technology' },
  { label: 'FAQ', href: '#faq' },
]

export default function App() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [darkMode, setDarkMode] = useState(true)

  useEffect(() => {
    document.documentElement.dataset.theme = darkMode ? 'dark' : 'light'
  }, [darkMode])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setMenuOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  const closeMenu = () => setMenuOpen(false)

  return (
    <div className="site-shell">
      <header className="site-header">
        <div className="site-container site-header__inner">
          <a className="brand-link" href="#top" onClick={closeMenu} aria-label="AdBreak Arcade home"><BrandMark /></a>
          <nav id="primary-navigation" className={`site-nav ${menuOpen ? 'is-open' : ''}`} aria-label="Primary navigation">
            {navItems.map((item) => <a key={item.href} href={item.href} onClick={closeMenu}>{item.label}</a>)}
            <a className="nav-repo" href={repositoryUrl} target="_blank" rel="noreferrer"><Icon name="external" /> GitHub</a>
            <a className="nav-download" href={apkUrl}>Test APK <Icon name="arrow" /></a>
          </nav>
          <div className="header-actions">
            <button className="theme-toggle" type="button" onClick={() => setDarkMode((value) => !value)} aria-label={darkMode ? 'Switch to light theme' : 'Switch to dark theme'}>
              <span className="theme-toggle__sun">☼</span><span className="theme-toggle__moon">◐</span>
            </button>
            <button className="menu-toggle" type="button" aria-expanded={menuOpen} aria-controls="primary-navigation" onClick={() => setMenuOpen((value) => !value)}>
              <span className="sr-only">{menuOpen ? 'Close menu' : 'Open menu'}</span>
              <Icon name={menuOpen ? 'x' : 'menu'} />
            </button>
          </div>
        </div>
      </header>

      <main id="main-content">
        <section id="top" className="hero-section site-container">
          <div className="hero-grid">
            <Reveal className="hero-copy">
              <div className="signal-line"><span className="signal-line__mark" /> NATIVE ANDROID · 17 OFFLINE GAMES</div>
              <h1>Make the<br /><span>waiting part</span> yours.</h1>
              <p className="hero-lede">A small arcade shelf for the moments between everything else. Choose a game, expand the notification, and play with ordinary Android controls while another app stays in front.</p>
              <div className="hero-actions">
                <a className="button button--primary" href="#demo">See how it plays <Icon name="arrow" /></a>
                <a className="button button--secondary" href={repositoryUrl} target="_blank" rel="noreferrer">View source <Icon name="external" /></a>
              </div>
              <div className="hero-proof">
                <span><Icon name="check" /> Offline games</span>
                <span><Icon name="check" /> Local state</span>
                <span><Icon name="check" /> No network permission</span>
              </div>
              <p className="hero-release"><span className="release-dot" /> v1.2.0 debug APK is available for testing <a href={releaseUrl}>View release</a></p>
            </Reveal>
            <Reveal className="hero-preview" delay={80}>
              <NotificationDemo />
            </Reveal>
          </div>
          <div className="hero-gridline hero-gridline--one" aria-hidden="true" />
          <div className="hero-gridline hero-gridline--two" aria-hidden="true" />
        </section>

        <section className="signal-strip" aria-label="Project facts">
          <div className="site-container signal-strip__inner">
            {stats.map((stat) => <div className="stat-block" key={stat.label}><strong>{stat.value}</strong><span>{stat.label}</span></div>)}
            <div className="stat-block stat-block--note"><span className="mono-label">A SMALL, DELIBERATE ARCADE</span><span>Native Kotlin · Android Views/XML · RemoteViews</span></div>
          </div>
        </section>

        <section id="how-it-works" className="section section--how site-container">
          <Reveal className="section-heading section-heading--split">
            <div>
              <span className="section-kicker">The loop</span>
              <h2>Three moves from idle<br /><em>to occupied.</em></h2>
            </div>
            <p>AdBreak Arcade keeps the interaction small on purpose. Android owns the notification shell; the app supplies a compact game state and clear next actions.</p>
          </Reveal>
          <div className="how-flow">
            <FlowStep number="01" title="Choose" detail="Pick one of the 17 games from Library." icon="grid" />
            <div className="flow-connector" aria-hidden="true"><span /></div>
            <FlowStep number="02" title="Expand" detail="Start the notification, then open it from the shade." icon="up" />
            <div className="flow-connector" aria-hidden="true"><span /></div>
            <FlowStep number="03" title="Play" detail="Make moves with normal notification buttons." icon="play" />
          </div>
        </section>

        <section id="features" className="section section--features site-container">
          <Reveal className="section-heading">
            <span className="section-kicker">The mechanism</span>
            <h2>Built around the thing<br /><em>only Android can own.</em></h2>
            <p>Not a fake game window. Not a second app pretending to be a notification. A compact state machine rendered inside the boundaries of Android’s System UI.</p>
          </Reveal>
          <div className="anatomy-grid">
            <Reveal className="anatomy-copy" delay={80}>
              <div className="anatomy-list">
                <div className="anatomy-item is-selected"><span className="anatomy-index">A</span><div><strong>Collapsed state</strong><p>Title, status, score, and a hint invite the next move.</p></div></div>
                <div className="anatomy-item"><span className="anatomy-index">B</span><div><strong>Expanded controls</strong><p>Up to four action buttons or a compact nine-cell board.</p></div></div>
                <div className="anatomy-item"><span className="anatomy-index">C</span><div><strong>Quiet persistence</strong><p>Game state, scores, and session safety stay local.</p></div></div>
              </div>
              <p className="constraint-note"><span className="mono-label">BOUNDARY</span> Android controls sizing, decoration, dismissal, and media focus. The site does not promise a universal notification appearance.</p>
            </Reveal>
            <Reveal className="anatomy-visual" delay={140}>
              <div className="anatomy-visual__header"><span className="mono-label">NOTIFICATION ANATOMY</span><span className="mono-label">COMPACT → EXPANDED</span></div>
              <div className="anatomy-notification anatomy-notification--compact">
                <span className="notification-app-icon"><Icon name="plus" /></span>
                <div><strong>AdBreak Arcade</strong><span>Tap Counter · READY · 00 SCORE</span></div>
                <Icon name="chevron" />
              </div>
              <div className="anatomy-notch" aria-hidden="true" />
              <div className="anatomy-notification anatomy-notification--expanded">
                <div className="anatomy-expanded-top"><span className="mono-label">TAP COUNTER</span><span className="score-readout">00 SCORE</span></div>
                <h3>Make one move.</h3>
                <p>One compact control. One small break.</p>
                <div className="anatomy-button-row"><span className="is-hot">TAP</span><span>RESET</span><span>PAUSE</span></div>
                <div className="anatomy-hint"><Icon name="check" /> Session-bound actions. No deliberate Activity launch per move.</div>
              </div>
            </Reveal>
          </div>
        </section>

        <section id="demo" className="section section--demo site-container">
          <Reveal className="demo-heading">
            <div><span className="section-kicker">Try the rhythm</span><h2>One tiny game,<br /><em>right here.</em></h2></div>
            <p>This is an independent website demo, not a live Android notification. Tap around, change the game, and reset the round.</p>
          </Reveal>
          <Reveal className="demo-stage" delay={80}><NotificationDemo /></Reveal>
        </section>

        <section id="games" className="section section--games site-container">
          <Reveal className="section-heading section-heading--shelf">
            <div><span className="section-kicker">The shelf</span><h2>Seventeen ways<br /><em>to spend a minute.</em></h2></div>
            <div className="shelf-intro"><strong>17</strong><p>registered offline games, each shaped around notification-sized controls. Some use fixed built-in puzzle sets; all stay small enough to read at a glance.</p></div>
          </Reveal>
          <Reveal delay={80}><GameShelf /></Reveal>
        </section>

        <section className="section section--local site-container">
          <Reveal className="local-panel">
            <div className="local-panel__copy"><span className="section-kicker">Small by design</span><h2>Small games.<br /><em>Kept local.</em></h2><p>There is no account to create, no backend to maintain, and no network permission in the manifest. Scores and saved state live in local preferences on the device.</p><a className="text-link" href="#install">Read the installation notes <Icon name="arrow" /></a></div>
            <div className="local-readout"><div className="readout-line"><span>ACCOUNT</span><strong>NONE</strong></div><div className="readout-line"><span>NETWORK</span><strong>NOT DECLARED</strong></div><div className="readout-line"><span>STATE</span><strong>LOCAL</strong></div><div className="readout-line"><span>TRIGGERS</span><strong>OFF BY DEFAULT</strong></div><div className="readout-seal"><Icon name="lock" /><span>NO UPLOAD PATH<br /><small>as implemented</small></span></div></div>
          </Reveal>
        </section>

        <section className="section section--triggers site-container">
          <Reveal className="section-heading section-heading--split">
            <div><span className="section-kicker">Optional preparation</span><h2>Under your<br /><em>control.</em></h2></div>
            <p>Smart App Triggers are disabled by default. If you opt in, the app observes package-level foreground transitions for apps you explicitly choose.</p>
          </Reveal>
          <div className="trigger-layout">
            <Reveal className="trigger-terminal" delay={80}>
              <div className="terminal-top"><span /><span /><span /><b>SMART_APP_TRIGGERS</b></div>
              <div className="terminal-body"><p><span className="terminal-prompt">$</span> status --feature</p><p className="terminal-output">ENABLED <b>FALSE</b></p><p><span className="terminal-prompt">$</span> scope --selected</p><p className="terminal-output">PACKAGE FOREGROUND EVENTS ONLY</p><p><span className="terminal-prompt">$</span> timing --guarantee</p><p className="terminal-output terminal-output--muted">BEST EFFORT · LIFECYCLE POLL</p></div>
            </Reveal>
            <Reveal className="trigger-copy" delay={140}>
              <div className="boundary-list"><Boundary title="Explicit opt-in" detail="Grant Usage access, select the apps, choose the game, and set a cooldown." icon="check" /><Boundary title="No content inspection" detail="The feature does not read another app’s screen, video, text, or advertisements." icon="check" /><Boundary title="No forced shade" detail="It prepares a notification where Android permits a lifecycle poll; you still expand it yourself." icon="check" /></div>
              <p className="trigger-footnote"><span className="mono-label">HONEST STATUS</span> Device and manufacturer background restrictions may delay or prevent a check.</p>
            </Reveal>
          </div>
        </section>

        <section id="technology" className="section section--technology site-container">
          <Reveal className="section-heading">
            <span className="section-kicker">Under the card</span>
            <h2>Native all the way<br /><em>down to the action.</em></h2>
            <p>Kotlin, Android Views/XML, and a deliberately narrow notification surface. The architecture keeps game rules separate from the platform shell.</p>
          </Reveal>
          <Reveal className="architecture-map" delay={80}>
            <div className="architecture-line" aria-hidden="true" />
            {architectureSteps.map((step, index) => <div className={`architecture-node architecture-node--${step.tone}`} key={step.label}><span className="architecture-node__index">{String(index + 1).padStart(2, '0')}</span><strong>{step.label}</strong><code>{step.detail}</code></div>)}
          </Reveal>
          <div className="tech-lower-grid">
            <Reveal className="code-panel">
              <div className="code-panel__top"><span className="mono-label">REAL CLASS NAMES</span><span className="mono-label">KOTLIN / ANDROID</span></div>
              <pre><code><span className="code-muted">object</span> <span className="code-violet">GameRegistry</span> {'{'}{`\n`}  <span className="code-muted">val</span> definitions = factories.values.map {'{'} it() {'}'}{`\n`}  <span className="code-muted">fun</span> create(id: String): ArcadeGame{`\n`} {'}'}{`\n\n`}<span className="code-muted">class</span> <span className="code-violet">NotificationController</span> {'{'}{`\n`}  RemoteViews(compact, expanded){`\n`}  PendingIntent.getBroadcast(...){`\n`}  NotificationManager.notify(...){`\n`} {'}'}</code></pre>
            </Reveal>
            <Reveal className="tech-callouts" delay={80}>
              <TechCallout title="Shared game abstraction" detail="Every registered game produces a notification-sized GameViewState and responds to a shared action model." />
              <TechCallout title="Session-safe actions" detail="Immutable PendingIntent envelopes carry game and session IDs so stale taps cannot mutate a newer run." />
              <TechCallout title="RemoteViews on purpose" detail="The notification avoids WebView, Canvas, Unity, and unrestricted custom views by design." />
            </Reveal>
          </div>
        </section>

        <section id="install" className="section section--install site-container">
          <Reveal className="section-heading section-heading--split">
            <div><span className="section-kicker">Put it on a device</span><h2>Build it. Test it.<br /><em>Then play.</em></h2></div>
            <p>The repository currently ships a verified debug APK for testing. Production release signing is intentionally not configured in the public project.</p>
          </Reveal>
          <div className="install-grid">
            <Reveal className="install-card install-card--primary" delay={80}><span className="install-card__number">01</span><h3>Build locally</h3><p>Use the Gradle wrapper with JDK 17 and Android SDK API 36.</p><code>./gradlew testDebugUnitTest<br />./gradlew assembleDebug</code><a className="text-link" href={repositoryUrl + '#readme'} target="_blank" rel="noreferrer">Read the setup notes <Icon name="external" /></a></Reveal>
            <Reveal className="install-card" delay={140}><span className="install-card__number">02</span><h3>Install the test build</h3><p>Version 1.2.0 debug APK, signed with the standard Android debug key.</p><a className="button button--primary button--small" href={apkUrl}>Download debug APK <Icon name="arrow" /></a><span className="install-note">Testing artifact · not Play Store distribution</span></Reveal>
            <Reveal className="install-card" delay={200}><span className="install-card__number">03</span><h3>Grant what you need</h3><p>Android 13/API 33+ needs notification permission. Smart App Triggers separately need Usage access.</p><a className="text-link" href={releaseUrl} target="_blank" rel="noreferrer">View the v1.2.0 release <Icon name="external" /></a></Reveal>
          </div>
        </section>

        <section id="faq" className="section section--faq site-container">
          <Reveal className="section-heading section-heading--split"><div><span className="section-kicker">Clear edges</span><h2>Questions before<br /><em>the first tap.</em></h2></div><p>The short answers are the product boundary: notification controls, local state, opt-in triggers, and the things Android still owns.</p></Reveal>
          <Reveal className="faq-layout" delay={80}><FaqList /></Reveal>
        </section>

        <section className="section section--contribute site-container">
          <Reveal className="contribute-panel">
            <div><span className="section-kicker">Bring a good idea</span><h2>Make the next<br /><em>small break better.</em></h2><p>The project is public to inspect and build. There is currently no project-level LICENSE file, so review the repository terms before reusing or redistributing code.</p></div>
            <div className="contribute-actions"><a className="button button--primary" href={repositoryUrl} target="_blank" rel="noreferrer">View the repository <Icon name="external" /></a><a className="button button--secondary" href={repositoryUrl + '/issues'} target="_blank" rel="noreferrer">Report an issue <Icon name="arrow" /></a><span className="mono-label">ANDROID CI · BUILDS ON MAIN + PRS</span></div>
          </Reveal>
        </section>
      </main>

      <footer className="site-footer">
        <div className="site-container site-footer__top"><BrandMark compact /><p>Short games for the small spaces between things.</p><a href="#top">Back to top <Icon name="up" /></a></div>
        <div className="site-container site-footer__bottom"><span>ADBREAK ARCADE · NATIVE ANDROID PROJECT</span><div><a href={repositoryUrl} target="_blank" rel="noreferrer">GitHub</a><a href={releaseUrl} target="_blank" rel="noreferrer">Releases</a><a href="#faq">FAQ</a></div></div>
      </footer>
    </div>
  )
}

function FlowStep({ number, title, detail, icon }: { number: string; title: string; detail: string; icon: 'grid' | 'up' | 'play' }) {
  return <article className="flow-step"><span className="flow-step__number">{number}</span><span className="flow-step__icon"><Icon name={icon} /></span><h3>{title}</h3><p>{detail}</p></article>
}

function Boundary({ title, detail, icon }: { title: string; detail: string; icon: 'check' }) {
  return <div className="boundary"><span className="boundary__icon"><Icon name={icon} /></span><div><strong>{title}</strong><p>{detail}</p></div></div>
}

function TechCallout({ title, detail }: { title: string; detail: string }) {
  return <div className="tech-callout"><span className="calibration-mark" /><div><strong>{title}</strong><p>{detail}</p></div></div>
}

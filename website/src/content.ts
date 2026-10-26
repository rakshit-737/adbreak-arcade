export type GameCategory = 'Action' | 'Logic' | 'Words & knowledge' | 'Board'

export type Game = {
  id: string
  title: string
  category: GameCategory
  description: string
  signal: string
  icon: string
}

export const games: Game[] = [
  { id: 'tap_counter', title: 'Tap Counter', category: 'Action', description: 'Tap one compact control to build a running score.', signal: 'ONE CONTROL', icon: 'plus' },
  { id: 'quick_math', title: 'Quick Math', category: 'Words & knowledge', description: 'Solve short arithmetic prompts with four answers.', signal: 'FOUR ANSWERS', icon: 'grid' },
  { id: 'tic_tac_toe', title: 'Tic-Tac-Toe', category: 'Board', description: 'Place X in numbered cells against a defensive arcade opponent.', signal: '3 × 3 BOARD', icon: 'board' },
  { id: 'higher_lower', title: 'Higher or Lower', category: 'Logic', description: 'Use the current range to find a hidden number.', signal: 'FIVE ATTEMPTS', icon: 'up' },
  { id: 'memory_challenge', title: 'Memory Challenge', category: 'Logic', description: 'Memorize a sequence, then repeat it in order.', signal: 'SEQUENCE', icon: 'spark' },
  { id: 'word_scramble', title: 'Word Scramble', category: 'Words & knowledge', description: 'Choose the word that matches the scrambled letters.', signal: 'WORD CHOICE', icon: 'type' },
  { id: 'number_sequence', title: 'Number Sequence', category: 'Logic', description: 'Find the missing term in a short sequence.', signal: 'PATTERN', icon: 'chart' },
  { id: 'rock_paper_scissors', title: 'Rock Paper Scissors', category: 'Action', description: 'Pick a move for a quick round against the arcade.', signal: 'THREE MOVES', icon: 'hand' },
  { id: 'odd_one_out', title: 'Odd One Out', category: 'Logic', description: 'Find the one item that does not belong.', signal: 'FIVE PUZZLES', icon: 'target' },
  { id: 'emoji_decode', title: 'Emoji Decode', category: 'Words & knowledge', description: 'Translate a compact emoji clue into a phrase.', signal: 'FOUR CLUES', icon: 'smile' },
  { id: 'trivia_blitz', title: 'Trivia Blitz', category: 'Words & knowledge', description: 'Answer short built-in general-knowledge questions.', signal: 'QUICK QUIZ', icon: 'question' },
  { id: 'mastermind_mini', title: 'Mastermind Mini', category: 'Logic', description: 'Use exact and misplaced digits to crack a three-digit code.', signal: 'CODE BREAK', icon: 'lock' },
  { id: 'nim', title: 'Nim', category: 'Board', description: 'Take one to three stones and try to take the last.', signal: '13 STONES', icon: 'stones' },
  { id: 'connect_three', title: 'Connect Three', category: 'Board', description: 'Drop tokens into three columns and connect a line.', signal: 'GRAVITY BOARD', icon: 'columns' },
  { id: 'guess_the_pattern', title: 'Guess the Pattern', category: 'Logic', description: 'Spot the rule in a short sequence and choose what comes next.', signal: 'FOUR CHOICES', icon: 'pattern' },
  { id: 'anagram_challenge', title: 'Anagram Challenge', category: 'Words & knowledge', description: 'Build a hidden word one letter at a time.', signal: 'LETTER BY LETTER', icon: 'letters' },
  { id: 'lights_out_mini', title: 'Lights Out Mini', category: 'Board', description: 'Toggle a compact board until every light is off.', signal: '3 × 3 GRID', icon: 'sun' },
  { id: 'cyber_heist', title: 'Cyber Heist', category: 'Logic', description: 'Route a fictional breach through three escalating missions.', signal: 'SECURITY METER', icon: 'lock' },
  { id: 'pocket_dungeon', title: 'Pocket Dungeon', category: 'Action', description: 'Fight, scout, heal, and loot a five-room turn-based run.', signal: 'FIVE ROOMS', icon: 'stones' },
  { id: 'neon_tactics', title: 'Neon Tactics', category: 'Board', description: 'Read the drone intent and reach the core on a compact grid.', signal: 'TACTICAL GRID', icon: 'board' },
  { id: 'cipher_vault', title: 'Cipher Vault', category: 'Logic', description: 'Stage digit guesses and use exact or misplaced clues to crack the vault.', signal: 'CODE BREAK', icon: 'lock' },
  { id: 'prism_protocol', title: 'Prism Protocol', category: 'Board', description: 'Rotate mirrors to route a deterministic beam to its target.', signal: 'BEAM ROUTING', icon: 'chart' },
  { id: 'starship_survival', title: 'Starship Survival', category: 'Action', description: 'Manage hull, shields, fuel, energy, and supplies across six hazards.', signal: 'RESOURCE RUN', icon: 'up' },
  { id: 'micro_tower_command', title: 'Micro Tower Command', category: 'Board', description: 'Place dart and frost towers against deterministic enemy waves.', signal: 'THREE WAVES', icon: 'columns' },
  { id: 'pulse_reactor', title: 'Pulse Reactor', category: 'Logic', description: 'Memorize escalating pulse patterns before three mistakes overload the reactor.', signal: 'PATTERN MEMORY', icon: 'spark' },
]

export const stats = [
  { value: '25', label: 'registered games' },
  { value: 'LOCAL', label: 'scores & state' },
  { value: '0', label: 'network permissions' },
]

export const faq = [
  {
    question: 'What is AdBreak Arcade?',
    answer: 'A native Android app with 25 short offline mini-games that can be played from an expanded notification. Choose a game, start the notification, expand it, and play with ordinary notification controls.',
  },
  {
    question: 'Does it detect or skip advertisements?',
    answer: 'No. AdBreak Arcade does not detect, identify, skip, click, block, or manipulate ads, and it does not inspect the content of other apps.',
  },
  {
    question: 'What permissions are required?',
    answer: 'Notification permission is required for notification gameplay on Android 13/API 33 and newer. Smart App Triggers are optional, disabled by default, and require Android Usage access.',
  },
  {
    question: 'Do I have to open the app for every move?',
    answer: 'No. Once the notification is active, game moves are handled through its controls. The main app is still used for choosing games, settings, permissions, and trigger configuration.',
  },
  {
    question: 'Will Smart App Triggers react instantly?',
    answer: 'No guarantee is provided. They are best effort and lifecycle-driven: selected package foreground events are checked when the app resumes or when you use the manual test action. They do not run a continuously active watcher or force the notification shade open.',
  },
  {
    question: 'Will my video keep playing while I play?',
    answer: 'Android controls notification shade behavior and media focus, so playback can vary by app, Android version, and device manufacturer. The project does not promise uninterrupted playback.',
  },
  {
    question: 'Are scores synced across devices?',
    answer: 'No. Scores and game state are stored locally through SharedPreferences. The app does not use accounts, a backend, analytics, or a declared network permission.',
  },
  {
    question: 'Is the APK ready for Google Play?',
    answer: 'Not yet. The current v1.4.0 debug APK is available for testing. Production signing is not configured in the repository, and the release build is not a distributable production artifact.',
  },
]

export const architectureSteps = [
  { label: 'Selection UI', detail: 'MainActivity', tone: 'violet' },
  { label: 'Game Registry', detail: '25 fresh ArcadeGame instances', tone: 'blue' },
  { label: 'State & Actions', detail: 'versioned JSON + SharedPreferences', tone: 'lilac' },
  { label: 'Notification Controller', detail: 'RemoteViews + immutable PendingIntent', tone: 'violet' },
  { label: 'Android System UI', detail: 'compact + expanded notification', tone: 'blue' },
]

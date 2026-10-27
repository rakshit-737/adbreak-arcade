package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * A deterministic beam-routing puzzle.  A turn is deliberately split into
 * two small actions: select a mirror cell, then rotate it clockwise or
 * counter-clockwise.  This keeps the game usable by the notification-sized
 * game surface without putting board-editing state in shared UI code.
 */
class PrismProtocolGame(private val initialLevel: Int = 0) : ArcadeGame {
    enum class Direction {
        UP,
        RIGHT,
        DOWN,
        LEFT;

        companion object {
            val NORTH = UP
            val EAST = RIGHT
            val SOUTH = DOWN
            val WEST = LEFT
        }
    }

    enum class MirrorOrientation {
        SLASH,
        BACKSLASH;

        fun rotatedQuarterTurn(): MirrorOrientation = when (this) {
            SLASH -> BACKSLASH
            BACKSLASH -> SLASH
        }

        fun symbol(): String = when (this) {
            SLASH -> "/"
            BACKSLASH -> "\\"
        }
    }

    data class Cell(val row: Int, val column: Int)

    data class Mirror(
        val cell: Cell,
        val orientation: MirrorOrientation,
    )

    data class Level(
        val name: String,
        val width: Int,
        val height: Int,
        val source: Cell,
        val sourceDirection: Direction,
        val target: Cell,
        val mirrors: List<Mirror>,
        val blockers: Set<Cell> = emptySet(),
        val moveLimit: Int = 8,
        val solution: List<Int> = emptyList(),
    ) {
        val initialOrientations: Map<Cell, MirrorOrientation>
            get() = mirrors.associate { it.cell to it.orientation }
    }

    enum class BeamStatus {
        TARGET,
        EXITED,
        BLOCKED,
        LOOP,
        INVALID;

        companion object {
            val HIT_TARGET = TARGET
            val EXIT = EXITED
        }
    }

    data class BeamState(val cell: Cell, val direction: Direction)

    data class BeamResult(
        val status: BeamStatus,
        val path: List<Cell> = emptyList(),
        val states: List<BeamState> = emptyList(),
        val terminal: Cell? = null,
    ) {
        val reachedTarget: Boolean get() = status == BeamStatus.TARGET
    }

    companion object {
        const val VERSION = 1
        const val MAX_MIRRORS = 9

        /** Hand-authored levels; their solutions are checked when the class loads. */
        val levels: List<Level> = listOf(
            Level(
                name = "First Refraction",
                width = 5,
                height = 5,
                source = Cell(2, 0),
                sourceDirection = Direction.RIGHT,
                target = Cell(0, 4),
                mirrors = listOf(
                    Mirror(Cell(2, 2), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(0, 2), MirrorOrientation.SLASH),
                    Mirror(Cell(4, 2), MirrorOrientation.SLASH),
                ),
                blockers = setOf(Cell(3, 2)),
                moveLimit = 3,
                solution = listOf(0),
            ),
            Level(
                name = "Shielded Relay",
                width = 5,
                height = 5,
                source = Cell(4, 0),
                sourceDirection = Direction.RIGHT,
                target = Cell(0, 4),
                mirrors = listOf(
                    Mirror(Cell(4, 2), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(2, 2), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(2, 4), MirrorOrientation.SLASH),
                ),
                blockers = setOf(Cell(4, 4), Cell(3, 3)),
                moveLimit = 4,
                solution = listOf(0, 1),
            ),
            Level(
                name = "Closed Circuit",
                width = 5,
                height = 5,
                source = Cell(0, 2),
                sourceDirection = Direction.LEFT,
                target = Cell(2, 3),
                mirrors = listOf(
                    Mirror(Cell(1, 3), MirrorOrientation.SLASH),
                    Mirror(Cell(0, 3), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(0, 1), MirrorOrientation.SLASH),
                    Mirror(Cell(1, 1), MirrorOrientation.BACKSLASH),
                ),
                blockers = setOf(Cell(3, 3)),
                moveLimit = 3,
                solution = listOf(0),
            ),
            Level(
                name = "Deep Relay",
                width = 7,
                height = 7,
                source = Cell(6, 0),
                sourceDirection = Direction.RIGHT,
                target = Cell(0, 6),
                mirrors = listOf(
                    Mirror(Cell(6, 2), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(4, 2), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(4, 4), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(2, 4), MirrorOrientation.SLASH),
                    Mirror(Cell(2, 1), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(0, 1), MirrorOrientation.SLASH),
                ),
                moveLimit = 6,
                solution = listOf(0, 1, 2, 3),
            ),
            Level(
                name = "Return Transmission",
                width = 7,
                height = 7,
                source = Cell(0, 6),
                sourceDirection = Direction.LEFT,
                target = Cell(6, 0),
                mirrors = listOf(
                    Mirror(Cell(0, 4), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(2, 4), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(2, 2), MirrorOrientation.BACKSLASH),
                    Mirror(Cell(4, 2), MirrorOrientation.SLASH),
                    Mirror(Cell(4, 5), MirrorOrientation.SLASH),
                    Mirror(Cell(6, 5), MirrorOrientation.BACKSLASH),
                ),
                moveLimit = 8,
                solution = listOf(0, 1, 2, 3, 4, 5),
            ),
        ).also { configured ->
            require(configured.all(::validateLevel)) { "Invalid Prism Protocol level set" }
        }

        /**
         * Traces a beam from just outside [level.source] until it reaches the
         * target, exits the board, hits a blocker, or repeats a cell/direction.
         * A mirror's two physical orientations are selected by the parity of
         * its quarter-turn count, so clockwise and counter-clockwise rotations
         * remain meaningful in saved state even though they have the same
         * optical result for a flat mirror.
         */
        fun propagate(
            level: Level,
            orientations: List<MirrorOrientation> = level.mirrors.map { it.orientation },
        ): BeamResult {
            if (!isStructurallyValid(level) || orientations.size != level.mirrors.size) {
                return BeamResult(BeamStatus.INVALID)
            }

            val mirrorMap = level.mirrors.mapIndexed { index, mirror ->
                mirror.cell to orientations[index]
            }.toMap()
            val seen = mutableSetOf<BeamState>()
            val states = mutableListOf<BeamState>()
            var cell = level.source
            var direction = level.sourceDirection

            while (true) {
                cell = cell.step(direction)
                if (cell.row !in 0 until level.height || cell.column !in 0 until level.width) {
                    return BeamResult(
                        status = BeamStatus.EXITED,
                        path = states.map { it.cell },
                        states = states.toList(),
                    )
                }

                val state = BeamState(cell, direction)
                if (!seen.add(state)) {
                    return BeamResult(
                        status = BeamStatus.LOOP,
                        path = states.map { it.cell },
                        states = states.toList(),
                        terminal = cell,
                    )
                }
                states += state

                if (cell == level.target) {
                    return BeamResult(
                        status = BeamStatus.TARGET,
                        path = states.map { it.cell },
                        states = states.toList(),
                        terminal = cell,
                    )
                }
                if (cell in level.blockers) {
                    return BeamResult(
                        status = BeamStatus.BLOCKED,
                        path = states.map { it.cell },
                        states = states.toList(),
                        terminal = cell,
                    )
                }

                mirrorMap[cell]?.let { orientation ->
                    direction = reflect(direction, orientation)
                }
            }
        }

        /** Convenience form for callers that keep mirror state keyed by board cell. */
        fun propagate(
            level: Level,
            orientations: Map<Cell, MirrorOrientation>,
        ): BeamResult = propagate(
            level,
            level.mirrors.map { orientations[it.cell] ?: it.orientation },
        )

        /** Returns false for malformed geometry or a solution that does not exactly route the beam. */
        fun validateLevel(level: Level): Boolean {
            if (!isStructurallyValid(level) || level.solution.isEmpty()) return false
            var orientations = level.mirrors.map { it.orientation }
            if (propagate(level, orientations).status == BeamStatus.TARGET) return false

            level.solution.forEachIndexed { step, mirrorIndex ->
                if (mirrorIndex !in level.mirrors.indices) return false
                orientations = orientations.toMutableList().also {
                    it[mirrorIndex] = it[mirrorIndex].rotatedQuarterTurn()
                }
                if (step != level.solution.lastIndex &&
                    propagate(level, orientations).status == BeamStatus.TARGET
                ) return false
            }
            return propagate(level, orientations).status == BeamStatus.TARGET &&
                level.solution.size <= level.moveLimit
        }

        private fun isStructurallyValid(level: Level): Boolean {
            if (level.name.isBlank() || level.width !in 3..20 || level.height !in 3..20 ||
                level.moveLimit !in 1..100 ||
                level.mirrors.size > MAX_MIRRORS || level.source == level.target
            ) return false

            fun inside(cell: Cell): Boolean =
                cell.row in 0 until level.height && cell.column in 0 until level.width

            if (!inside(level.source) || !inside(level.target) ||
                level.mirrors.any { !inside(it.cell) } ||
                level.mirrors.map { it.cell }.toSet().size != level.mirrors.size ||
                level.blockers.any { !inside(it) }
            ) return false
            val mirrorCells = level.mirrors.map { it.cell }.toSet()
            if (level.source in mirrorCells || level.target in mirrorCells ||
                level.source in level.blockers || level.target in level.blockers ||
                mirrorCells.any { it in level.blockers }
            ) return false
            return true
        }

        private fun reflect(direction: Direction, orientation: MirrorOrientation): Direction = when (orientation) {
            MirrorOrientation.SLASH -> when (direction) {
                Direction.UP -> Direction.RIGHT
                Direction.RIGHT -> Direction.UP
                Direction.DOWN -> Direction.LEFT
                Direction.LEFT -> Direction.DOWN
            }
            MirrorOrientation.BACKSLASH -> when (direction) {
                Direction.UP -> Direction.LEFT
                Direction.RIGHT -> Direction.DOWN
                Direction.DOWN -> Direction.RIGHT
                Direction.LEFT -> Direction.UP
            }
        }

        private fun Cell.step(direction: Direction): Cell = when (direction) {
            Direction.UP -> copy(row = row - 1)
            Direction.RIGHT -> copy(column = column + 1)
            Direction.DOWN -> copy(row = row + 1)
            Direction.LEFT -> copy(column = column - 1)
        }
    }

    override val id: String = "prism_protocol"
    override val title: String = "Prism Protocol"
    override val description: String = "Rotate mirrors to route a deterministic beam through the target prism."

    var levelIndex: Int = initialLevel
        private set
    var moves: Int = 0
        private set
    var wins: Int = 0
        private set
    var bestMoves: Int = 0
        private set
    var selectedMirror: Int? = null
        private set

    private var rotations = IntArray(0)
    private var paused = false
    private var won = false
    private var lost = false
    private var feedback = "Select a mirror"

    val level: Level
        get() = levels[levelIndex]

    val beam: BeamResult
        get() = currentBeam()

    private val terminal: Boolean
        get() = won || lost

    init {
        require(initialLevel in levels.indices) { "Unknown Prism Protocol level: $initialLevel" }
        reset()
    }

    override fun reset() {
        levelIndex = initialLevel
        wins = 0
        bestMoves = 0
        startRound()
    }

    override fun apply(action: GameAction): Boolean {
        when (action) {
            GameAction.RESET -> {
                reset()
                return true
            }
            GameAction.NEW_GAME -> {
                if (won) levelIndex = (levelIndex + 1) % levels.size
                startRound()
                return true
            }
            GameAction.PAUSE -> {
                if (terminal || paused) return false
                paused = true
                feedback = "Protocol paused"
                return true
            }
            GameAction.RESUME -> {
                if (terminal || !paused) return false
                paused = false
                feedback = "Protocol resumed"
                return true
            }
            else -> Unit
        }

        if (terminal || paused) return false

        val cellIndex = action.ordinal - GameAction.CELL_1.ordinal
        if (cellIndex in level.mirrors.indices) {
            selectedMirror = cellIndex
            feedback = "Mirror ${cellIndex + 1} selected — choose a rotation"
            return true
        }
        if (action != GameAction.PRIMARY && action != GameAction.SECONDARY) return false

        val mirrorIndex = selectedMirror ?: return false
        rotate(mirrorIndex, clockwise = action == GameAction.PRIMARY)
        selectedMirror = null
        moves++

        val result = currentBeam()
        when {
            result.status == BeamStatus.TARGET -> {
                won = true
                wins++
                bestMoves = if (bestMoves == 0) moves else minOf(bestMoves, moves)
                feedback = "Protocol solved in $moves move${if (moves == 1) "" else "s"}"
            }
            moves >= level.moveLimit -> {
                lost = true
                feedback = "Signal lost — move limit reached"
            }
            else -> {
                feedback = when (result.status) {
                    BeamStatus.BLOCKED -> "Beam blocked — select another mirror"
                    BeamStatus.LOOP -> "Beam loop detected — select another mirror"
                    BeamStatus.EXITED -> "Beam escaped — select another mirror"
                    BeamStatus.INVALID -> "Invalid beam geometry"
                    BeamStatus.TARGET -> "Target acquired"
                }
            }
        }
        return true
    }

    private fun startRound() {
        rotations = IntArray(level.mirrors.size)
        moves = 0
        selectedMirror = null
        paused = false
        won = false
        lost = false
        feedback = "Select a mirror"
    }

    private fun rotate(index: Int, clockwise: Boolean) {
        rotations[index] = (rotations[index] + if (clockwise) 1 else 3) % 4
    }

    private fun currentOrientations(): List<MirrorOrientation> = level.mirrors.mapIndexed { index, mirror ->
        if (rotations[index] % 2 == 0) mirror.orientation else mirror.orientation.rotatedQuarterTurn()
    }

    private fun currentBeam(): BeamResult = propagate(level, currentOrientations())

    override fun render(): GameViewState {
        val result = currentBeam()
        fun coordinate(cell: Cell) = "${cell.row + 1},${cell.column + 1}"
        val route = result.path.mapNotNull { cell ->
            level.mirrors.indexOfFirst { it.cell == cell }.takeIf { it >= 0 }?.let { "M${it + 1}" }
        }.joinToString("→").ifEmpty { "none" }
        val geometry = "S(${coordinate(level.source)}) ${level.sourceDirection.name} → T(${coordinate(level.target)})"
        val subtitle = when {
            won -> "${level.name} solved — replay for the next protocol"
            lost -> "${level.name} failed — replay to try again"
            paused -> "Paused · ${level.name}"
            else -> "$geometry · ${if (level.moveLimit - moves == 1) "Last flip" else "${level.moveLimit - moves} flips left"}"
        }
        val cells = level.mirrors.mapIndexed { index, mirror ->
            val orientation = currentOrientations()[index].symbol()
            "${if (selectedMirror == index) ">" else ""}M${index + 1} $orientation (${mirror.cell.row + 1},${mirror.cell.column + 1})"
        }
        val buttons = when {
            won -> listOf("NEXT")
            lost -> listOf("RETRY")
            paused -> listOf("RESUME", "RESET")
            else -> listOf("FLIP ↻", "FLIP ↺")
        }
        val actions = when {
            terminal -> listOf(GameAction.NEW_GAME)
            paused -> listOf(GameAction.RESUME, GameAction.RESET)
            else -> listOf(GameAction.PRIMARY, GameAction.SECONDARY)
        }
        val boardStatus = when (result.status) {
            BeamStatus.TARGET -> "TARGET"
            BeamStatus.BLOCKED -> "BLOCKED"
            BeamStatus.LOOP -> "LOOP"
            BeamStatus.EXITED -> "EXIT"
            BeamStatus.INVALID -> "INVALID"
        }
        return GameViewState(
            title = title,
            subtitle = subtitle,
            scoreLine = "Moves $moves/${level.moveLimit} · Beam $route → $boardStatus" +
                if (level.blockers.isEmpty()) "" else " · Blocks ${level.blockers.joinToString(";") { coordinate(it) }}",
            buttons = buttons,
            buttonActions = actions,
            cellButtons = cells,
            cellActions = level.mirrors.indices.map { GameAction.values()[GameAction.CELL_1.ordinal + it] },
            feedback = feedback,
            isTerminal = terminal,
            progress = GameProgress(if (won) 1 else 0, 1, "Target linked"),
            record = wins.takeIf { it > 0 }?.let { GameRecord(it, "Protocols solved") },
            disabledActions = buildMap {
                if (!terminal && !paused && selectedMirror == null) {
                    put(GameAction.PRIMARY, "Select a mirror first")
                    put(GameAction.SECONDARY, "Select a mirror first")
                }
                if (terminal || paused) level.mirrors.indices.forEach {
                    put(GameAction.values()[GameAction.CELL_1.ordinal + it], if (paused) "Resume to select" else "Round complete")
                }
            },
            phase = when {
                won -> GamePhase.WON
                lost -> GamePhase.LOST
                paused -> GamePhase.PAUSED
                else -> GamePhase.PLAYING
            },
        )
    }

    override fun encode(): String = JSONObject().apply {
        put("v", VERSION)
        put("levelIndex", levelIndex)
        put("rotations", JSONArray(rotations.toList()))
        put("moves", moves)
        put("wins", wins)
        put("bestMoves", bestMoves)
        put("selectedMirror", selectedMirror ?: -1)
        put("paused", paused)
        put("won", won)
        put("lost", lost)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != VERSION) return false

        val restoredLevelIndex = json.optInt("levelIndex", -1)
        if (restoredLevelIndex !in levels.indices) return false
        val restoredLevel = levels[restoredLevelIndex]
        val rotationArray = json.optJSONArray("rotations") ?: return false
        if (rotationArray.length() != restoredLevel.mirrors.size) return false
        val restoredRotations = IntArray(rotationArray.length()) {
            rotationArray.optInt(it, -1)
        }
        if (restoredRotations.any { it !in 0..3 }) return false

        val restoredMoves = json.optInt("moves", -1)
        val restoredWins = json.optInt("wins", -1)
        val restoredBest = json.optInt("bestMoves", -1)
        val restoredSelected = json.optInt("selectedMirror", -2)
        val restoredPaused = json.optBoolean("paused", false)
        val restoredWon = json.optBoolean("won", false)
        val restoredLost = json.optBoolean("lost", false)
        val restoredFeedback = json.optString("feedback")
        val restoredTerminal = restoredWon || restoredLost
        if (restoredMoves !in 0..restoredLevel.moveLimit || restoredWins < 0 ||
            restoredBest < 0 || restoredBest > levels.maxOf { it.moveLimit } ||
            restoredSelected !in -1 until restoredLevel.mirrors.size ||
            restoredFeedback.isBlank() || restoredWon && restoredLost ||
            restoredPaused && (restoredWon || restoredLost) ||
            restoredTerminal && restoredSelected != -1
        ) return false

        val restoredOrientations = restoredLevel.mirrors.mapIndexed { index, mirror ->
            if (restoredRotations[index] % 2 == 0) mirror.orientation else mirror.orientation.rotatedQuarterTurn()
        }
        val restoredBeam = propagate(restoredLevel, restoredOrientations)
        if (restoredWon && (restoredMoves == 0 || restoredBeam.status != BeamStatus.TARGET) ||
            restoredLost && (restoredMoves != restoredLevel.moveLimit || restoredBeam.status == BeamStatus.TARGET) ||
            !restoredTerminal && (restoredMoves >= restoredLevel.moveLimit || restoredBeam.status == BeamStatus.TARGET) ||
            restoredWins == 0 && restoredBest != 0 ||
            restoredWins > 0 && restoredBest == 0
        ) return false

        levelIndex = restoredLevelIndex
        rotations = restoredRotations
        moves = restoredMoves
        wins = restoredWins
        bestMoves = restoredBest
        selectedMirror = restoredSelected.takeIf { it >= 0 }
        paused = restoredPaused
        won = restoredWon
        lost = restoredLost
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}

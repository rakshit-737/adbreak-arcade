package com.adbreak.arcade.core

import org.json.JSONObject

/**
 * A short, deterministic starship run designed to be playable from a notification.
 * Every turn resolves one fixed frontier event; no clock, coroutine, or random source
 * is needed to continue a run.
 */
class StarshipSurvivalGame : ArcadeGame {
    override val id = "starship_survival"
    override val title = "Starship Survival"
    override val description = "Guide a damaged starship through six fixed hazards and escape alive."

    companion object {
        const val MAX_HULL = 12
        const val MAX_SHIELDS = 6
        const val MAX_FUEL = 14
        const val MAX_ENERGY = 10
        const val MAX_SUPPLIES = 8
        const val TOTAL_EVENTS = 6

        const val NAVIGATE_FUEL_COST = 2
        const val NAVIGATE_SUPPLIES_COST = 1
        const val REPAIR_ENERGY_COST = 2
        const val REPAIR_SUPPLIES_COST = 1
        const val REPAIR_HULL_GAIN = 3
        const val REPAIR_SHIELDS_GAIN = 2
        const val SCAN_ENERGY_COST = 1
        const val SCAN_SUPPLIES_COST = 1
        const val EVADE_FUEL_COST = 1
        const val EVADE_ENERGY_COST = 1
        const val EVADE_SHIELDS_COST = 1

        private const val MAX_SCORE = 1_000_000
    }

    private data class Event(
        val name: String,
        val prompt: String,
        val navigationShieldDamage: Int,
        val navigationHullDamage: Int,
        val scanHullDamage: Int,
        val evadeHullDamage: Int,
    )

    private val events = listOf(
        Event("Asteroid field", "Evade the debris", 1, 1, 3, 0),
        Event("Radiation belt", "Scan a sheltered route", 2, 2, 0, 4),
        Event("Pirate patrol", "Scans reveal your position", 3, 1, 5, 0),
        Event("Ion nebula", "Scan the ion currents", 1, 3, 0, 4),
        Event("Gravity rift", "Evade the gravity well", 3, 2, 5, 0),
        Event("Solar flare", "Scan the flare cycle", 2, 4, 0, 5),
    )

    var hull: Int = MAX_HULL
        private set
    var shields: Int = MAX_SHIELDS
        private set
    var fuel: Int = MAX_FUEL
        private set
    var energy: Int = MAX_ENERGY
        private set
    var supplies: Int = MAX_SUPPLIES
        private set
    var eventIndex: Int = 0
        private set
    var score: Int = 0
        private set

    private var turns = 0
    private var escaped = false
    private var failed = false
    private var paused = false
    private var feedback = "Choose a course"

    private val terminal: Boolean
        get() = escaped || failed

    init {
        reset()
    }

    override fun reset() {
        hull = MAX_HULL
        shields = MAX_SHIELDS
        fuel = MAX_FUEL
        energy = MAX_ENERGY
        supplies = MAX_SUPPLIES
        eventIndex = 0
        score = 0
        turns = 0
        escaped = false
        failed = false
        paused = false
        feedback = "Choose a course"
    }

    override fun apply(action: GameAction): Boolean {
        when (action) {
            GameAction.RESET, GameAction.NEW_GAME -> {
                reset()
                return true
            }
            GameAction.PAUSE -> {
                if (terminal || paused) return false
                paused = true
                feedback = "Run paused"
                return true
            }
            GameAction.RESUME -> {
                if (terminal || !paused) return false
                paused = false
                feedback = "Course resumed"
                return true
            }
            else -> Unit
        }

        if (terminal || paused || eventIndex !in events.indices) return false

        return when (action) {
            GameAction.PRIMARY -> resolveNavigate()
            GameAction.SECONDARY -> resolveRepair()
            GameAction.CELL_1 -> resolveScan()
            GameAction.CELL_2 -> resolveEvade()
            else -> false
        }
    }

    private fun resolveNavigate(): Boolean {
        if (fuel < NAVIGATE_FUEL_COST || supplies < NAVIGATE_SUPPLIES_COST) return false
        val event = events[eventIndex]
        fuel -= NAVIGATE_FUEL_COST
        supplies -= NAVIGATE_SUPPLIES_COST
        applyDamage(event.navigationShieldDamage, event.navigationHullDamage)
        score += 10
        return finishTurn("Navigated ${event.name} · fuel -$NAVIGATE_FUEL_COST, supplies -$NAVIGATE_SUPPLIES_COST")
    }

    private fun resolveRepair(): Boolean {
        if (energy < REPAIR_ENERGY_COST || supplies < REPAIR_SUPPLIES_COST ||
            hull == MAX_HULL && shields == MAX_SHIELDS
        ) return false
        val event = events[eventIndex]
        val oldHull = hull
        val oldShields = shields
        energy -= REPAIR_ENERGY_COST
        supplies -= REPAIR_SUPPLIES_COST
        hull = (hull + REPAIR_HULL_GAIN).coerceAtMost(MAX_HULL)
        shields = (shields + REPAIR_SHIELDS_GAIN).coerceAtMost(MAX_SHIELDS)
        applyDamage(event.navigationShieldDamage, event.navigationHullDamage)
        score += 8
        val repaired = "net hull ${hull - oldHull}, shields ${shields - oldShields}; hazard still hits"
        return finishTurn("Repaired · energy -$REPAIR_ENERGY_COST, supplies -$REPAIR_SUPPLIES_COST · $repaired")
    }

    private fun resolveScan(): Boolean {
        if (energy < SCAN_ENERGY_COST || supplies < SCAN_SUPPLIES_COST) return false
        val event = events[eventIndex]
        energy -= SCAN_ENERGY_COST
        supplies -= SCAN_SUPPLIES_COST
        applyDamage(0, event.scanHullDamage)
        score += 6
        return finishTurn("Scanned ${event.name} · energy -$SCAN_ENERGY_COST, supplies -$SCAN_SUPPLIES_COST · hull -${event.scanHullDamage}")
    }

    private fun resolveEvade(): Boolean {
        if (fuel < EVADE_FUEL_COST || energy < EVADE_ENERGY_COST) return false
        val event = events[eventIndex]
        val oldHull = hull
        fuel -= EVADE_FUEL_COST
        energy -= EVADE_ENERGY_COST
        applyDamage(EVADE_SHIELDS_COST, event.evadeHullDamage)
        score += 9
        return finishTurn("Evaded ${event.name} · fuel -$EVADE_FUEL_COST, energy -$EVADE_ENERGY_COST · hull -${oldHull - hull}")
    }

    private fun applyDamage(shieldDamage: Int, hullDamage: Int) {
        val overflow = (shieldDamage - shields).coerceAtLeast(0)
        shields = (shields - shieldDamage).coerceAtLeast(0)
        hull = (hull - hullDamage - overflow).coerceAtLeast(0)
    }

    private fun finishTurn(actionFeedback: String): Boolean {
        turns++
        eventIndex++
        feedback = actionFeedback

        when {
            hull <= 0 -> {
                failed = true
                feedback = "Hull breach · escape failed at sector $eventIndex/$TOTAL_EVENTS"
            }
            eventIndex >= events.size -> {
                escaped = true
                feedback = "Escape complete · score $score"
            }
            !hasAffordableAction() -> {
                failed = true
                feedback = "Resources exhausted · escape failed at sector $eventIndex/$TOTAL_EVENTS"
            }
        }
        return true
    }

    private fun hasAffordableAction(): Boolean =
        fuel >= NAVIGATE_FUEL_COST && supplies >= NAVIGATE_SUPPLIES_COST ||
            energy >= REPAIR_ENERGY_COST && supplies >= REPAIR_SUPPLIES_COST ||
            energy >= SCAN_ENERGY_COST && supplies >= SCAN_SUPPLIES_COST ||
            fuel >= EVADE_FUEL_COST && energy >= EVADE_ENERGY_COST

    override fun render(): GameViewState {
        val currentEvent = events.getOrNull(eventIndex)
        val hullDeltas = currentEvent?.let {
            val navigate = it.navigationHullDamage + (it.navigationShieldDamage - shields).coerceAtLeast(0)
            val repairDamage = it.navigationHullDamage +
                (it.navigationShieldDamage - minOf(MAX_SHIELDS, shields + REPAIR_SHIELDS_GAIN)).coerceAtLeast(0)
            val repair = (minOf(MAX_HULL, hull + REPAIR_HULL_GAIN) - repairDamage).coerceAtLeast(0) - hull
            listOf(-minOf(hull, navigate), repair, -minOf(hull, it.scanHullDamage),
                -minOf(hull, it.evadeHullDamage + (EVADE_SHIELDS_COST - shields).coerceAtLeast(0)))
        } ?: List(4) { 0 }
        fun risk(index: Int): String {
            val delta = hullDeltas[index]
            return if (hull + delta <= 0) "BREACH" else "${if (delta >= 0) "+" else ""}${delta}HP"
        }
        val subtitle = when {
            escaped -> "Escaped the frontier · replay to fly again"
            failed -> "Run lost · replay to try a new route"
            paused -> "Paused · sector ${eventIndex + 1}/$TOTAL_EVENTS"
            currentEvent != null -> "Sector ${eventIndex + 1}/$TOTAL_EVENTS${if (eventIndex == TOTAL_EVENTS - 1) " · Last jump" else ""} · ${currentEvent.name}"
            else -> "Choose a course"
        }
        val buttons = when {
            terminal -> listOf("REPLAY")
            paused -> listOf("RESUME", "RESET")
            else -> listOf("NAV ${risk(0)}\n2F1S", "FIX ${risk(1)}\n2E1S", "SCAN ${risk(2)}\n1E1S", "EVADE ${risk(3)}\n1F1E")
        }
        val actions = when {
            terminal -> listOf(GameAction.NEW_GAME)
            paused -> listOf(GameAction.RESUME, GameAction.RESET)
            else -> listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
        }
        return GameViewState(
            title = title,
            subtitle = subtitle,
            scoreLine = "Hull $hull · Shields $shields · Fuel $fuel · Energy $energy · Supplies $supplies" + if (terminal) " · Score $score" else "",
            buttons = buttons,
            buttonActions = actions,
            feedback = feedback,
            isTerminal = terminal,
            progress = GameProgress(if (hull == 0) (eventIndex - 1).coerceAtLeast(0) else eventIndex, TOTAL_EVENTS, "Sectors crossed"),
            record = if (escaped) GameRecord(score, "Escape score") else null,
            disabledActions = if (terminal || paused) emptyMap() else buildMap {
                if (fuel < NAVIGATE_FUEL_COST || supplies < NAVIGATE_SUPPLIES_COST)
                    put(GameAction.PRIMARY, "Needs 2 fuel and 1 supply")
                if (energy < REPAIR_ENERGY_COST || supplies < REPAIR_SUPPLIES_COST)
                    put(GameAction.SECONDARY, "Needs 2 energy and 1 supply")
                else if (hull == MAX_HULL && shields == MAX_SHIELDS)
                    put(GameAction.SECONDARY, "Hull and shields already full")
                if (energy < SCAN_ENERGY_COST || supplies < SCAN_SUPPLIES_COST)
                    put(GameAction.CELL_1, "Needs 1 energy and 1 supply")
                if (fuel < EVADE_FUEL_COST || energy < EVADE_ENERGY_COST)
                    put(GameAction.CELL_2, "Needs 1 fuel and 1 energy")
            },
            phase = when {
                escaped -> GamePhase.WON
                failed -> GamePhase.LOST
                paused -> GamePhase.PAUSED
                else -> GamePhase.PLAYING
            },
        )
    }

    override fun encode(): String = JSONObject().apply {
        put("v", 1)
        put("hull", hull)
        put("shields", shields)
        put("fuel", fuel)
        put("energy", energy)
        put("supplies", supplies)
        put("eventIndex", eventIndex)
        put("score", score)
        put("turns", turns)
        put("escaped", escaped)
        put("failed", failed)
        put("paused", paused)
        put("feedback", feedback)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        if (json.optInt("v", -1) != 1) return false

        val restoredHull = json.optInt("hull", -1)
        val restoredShields = json.optInt("shields", -1)
        val restoredFuel = json.optInt("fuel", -1)
        val restoredEnergy = json.optInt("energy", -1)
        val restoredSupplies = json.optInt("supplies", -1)
        val restoredEventIndex = json.optInt("eventIndex", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredTurns = json.optInt("turns", -1)
        val restoredEscaped = json.optBoolean("escaped", false)
        val restoredFailed = json.optBoolean("failed", false)
        val restoredPaused = json.optBoolean("paused", false)
        val restoredFeedback = json.optString("feedback")
        val restoredTerminal = restoredEscaped || restoredFailed
        val restoredAffordable =
            restoredFuel >= NAVIGATE_FUEL_COST && restoredSupplies >= NAVIGATE_SUPPLIES_COST ||
                restoredEnergy >= REPAIR_ENERGY_COST && restoredSupplies >= REPAIR_SUPPLIES_COST ||
                restoredEnergy >= SCAN_ENERGY_COST && restoredSupplies >= SCAN_SUPPLIES_COST ||
                restoredFuel >= EVADE_FUEL_COST && restoredEnergy >= EVADE_ENERGY_COST

        if (restoredHull !in 0..MAX_HULL || restoredShields !in 0..MAX_SHIELDS ||
            restoredFuel !in 0..MAX_FUEL || restoredEnergy !in 0..MAX_ENERGY ||
            restoredSupplies !in 0..MAX_SUPPLIES || restoredEventIndex !in 0..events.size ||
            restoredScore !in 0..minOf(MAX_SCORE, restoredEventIndex * 10) || restoredTurns != restoredEventIndex ||
            restoredFeedback.isBlank() || restoredEscaped && restoredFailed ||
            restoredEscaped && (restoredEventIndex != events.size || restoredHull == 0) ||
            restoredFailed && !(!restoredAffordable || restoredHull == 0) ||
            !restoredTerminal && (restoredEventIndex == events.size || restoredHull == 0 || !restoredAffordable) ||
            restoredPaused && restoredTerminal
        ) return false

        hull = restoredHull
        shields = restoredShields
        fuel = restoredFuel
        energy = restoredEnergy
        supplies = restoredSupplies
        eventIndex = restoredEventIndex
        score = restoredScore
        turns = restoredTurns
        escaped = restoredEscaped
        failed = restoredFailed
        paused = restoredPaused
        feedback = restoredFeedback
        true
    }.getOrDefault(false)
}

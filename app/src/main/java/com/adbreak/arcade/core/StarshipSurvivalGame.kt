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
    )

    private val events = listOf(
        Event("Asteroid field", "Debris blocks the jump lane", 1, 1),
        Event("Radiation belt", "A bright storm crosses the route", 2, 2),
        Event("Pirate patrol", "Weapons lock on from the dark", 3, 1),
        Event("Ion nebula", "Charged gas scrambles the instruments", 1, 3),
        Event("Gravity rift", "Tidal forces pull the ship sideways", 3, 2),
        Event("Solar flare", "The escape vector is under fire", 2, 4),
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
        if (energy < REPAIR_ENERGY_COST || supplies < REPAIR_SUPPLIES_COST) return false
        val oldHull = hull
        val oldShields = shields
        energy -= REPAIR_ENERGY_COST
        supplies -= REPAIR_SUPPLIES_COST
        hull = (hull + REPAIR_HULL_GAIN).coerceAtMost(MAX_HULL)
        shields = (shields + REPAIR_SHIELDS_GAIN).coerceAtMost(MAX_SHIELDS)
        score += 8
        val repaired = "hull +${hull - oldHull}, shields +${shields - oldShields}"
        return finishTurn("Repaired · energy -$REPAIR_ENERGY_COST, supplies -$REPAIR_SUPPLIES_COST · $repaired")
    }

    private fun resolveScan(): Boolean {
        if (energy < SCAN_ENERGY_COST || supplies < SCAN_SUPPLIES_COST) return false
        val event = events[eventIndex]
        energy -= SCAN_ENERGY_COST
        supplies -= SCAN_SUPPLIES_COST
        score += 6
        return finishTurn("Scanned ${event.name} · energy -$SCAN_ENERGY_COST, supplies -$SCAN_SUPPLIES_COST · route clear")
    }

    private fun resolveEvade(): Boolean {
        if (fuel < EVADE_FUEL_COST || energy < EVADE_ENERGY_COST) return false
        val event = events[eventIndex]
        fuel -= EVADE_FUEL_COST
        energy -= EVADE_ENERGY_COST
        applyDamage(EVADE_SHIELDS_COST, 0)
        score += 9
        return finishTurn("Evaded ${event.name} · fuel -$EVADE_FUEL_COST, energy -$EVADE_ENERGY_COST, shields -$EVADE_SHIELDS_COST")
    }

    private fun applyDamage(shieldDamage: Int, hullDamage: Int) {
        shields = (shields - shieldDamage).coerceAtLeast(0)
        hull = (hull - hullDamage).coerceAtLeast(0)
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
        val subtitle = when {
            escaped -> "Escaped the frontier · replay to fly again"
            failed -> "Run lost · replay to try a new route"
            paused -> "Paused · sector ${eventIndex + 1}/$TOTAL_EVENTS"
            currentEvent != null -> "Sector ${eventIndex + 1}/$TOTAL_EVENTS · ${currentEvent.name}: ${currentEvent.prompt}"
            else -> "Choose a course"
        }
        val buttons = when {
            terminal -> listOf("REPLAY")
            paused -> listOf("RESUME", "RESET")
            else -> listOf("NAVIGATE", "REPAIR", "SCAN", "EVADE")
        }
        val actions = when {
            terminal -> listOf(GameAction.NEW_GAME)
            paused -> listOf(GameAction.RESUME, GameAction.RESET)
            else -> listOf(GameAction.PRIMARY, GameAction.SECONDARY, GameAction.CELL_1, GameAction.CELL_2)
        }
        return GameViewState(
            title = title,
            subtitle = subtitle,
            scoreLine = "Score $score · Hull $hull/$MAX_HULL · Shields $shields/$MAX_SHIELDS · Fuel $fuel · Energy $energy · Supplies $supplies",
            buttons = buttons,
            buttonActions = actions,
            feedback = feedback,
            isTerminal = terminal,
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
            restoredScore !in 0..MAX_SCORE || restoredTurns != restoredEventIndex ||
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

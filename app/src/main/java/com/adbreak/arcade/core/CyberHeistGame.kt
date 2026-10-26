package com.adbreak.arcade.core

import org.json.JSONArray
import org.json.JSONObject
import java.util.Random

/**
 * A finite, notification-friendly cyber heist campaign.
 *
 * Each accepted button press resolves exactly one stage of the current
 * mission.  There is no timer or background work, so a heist can safely be
 * paused between any two staged decisions and restored from a snapshot.
 */
class CyberHeistGame(private val random: Random = Random()) : ArcadeGame {
    override val id = "cyber_heist"
    override val title = "Cyber Heist"
    override val description = "Plan three quiet breaches, manage your resources, and escape with the data."

    companion object {
        const val TOTAL_MISSIONS = 3
        const val STAGES_PER_MISSION = 3
        const val MAX_SECURITY = 8
        const val MAX_STEALTH = 8
        const val MAX_TOOLS = 6
        const val MAX_INTEL = 6

        private const val VERSION = 2
        private const val STARTING_STEALTH = MAX_STEALTH
        private const val STARTING_TOOLS = MAX_TOOLS
        private const val STARTING_INTEL = MAX_INTEL
        private const val MAX_SCORE = 1_000_000_000
    }

    private enum class Stage(val label: String, val prompt: String) {
        RECON("RECON", "Map the target's defenses"),
        BREACH("BREACH", "Open a route into the target"),
        EXTRACT("EXTRACT", "Move the payload out cleanly"),
    }

    private data class Mission(
        val codename: String,
        val target: String,
        val securityLimit: Int,
        val difficulty: Int,
        val loot: Int,
    )

    private data class Choice(
        val label: String,
        val stealthCost: Int,
        val toolsCost: Int,
        val intelCost: Int,
        val securityRisk: Int,
        val points: Int,
    ) {
        fun canAfford(stealth: Int, tools: Int, intel: Int): Boolean =
            stealth >= stealthCost && tools >= toolsCost && intel >= intelCost
    }

    private data class Blueprint(val codename: String, val target: String)

    private val blueprints = listOf(
        Blueprint("GLASS KEY", "Helix Bank"),
        Blueprint("NIGHT MARKET", "Orbital Exchange"),
        Blueprint("BLACK ICE", "Crown Research"),
        Blueprint("SILENT CIRCUIT", "Atlas Grid"),
        Blueprint("GHOST PROTOCOL", "Axiom Vault"),
    )

    private var missions = emptyList<Mission>()

    var missionIndex: Int = 0
        private set
    var stageIndex: Int = 0
        private set
    var missionsCompleted: Int = 0
        private set
    var security: Int = 0
        private set
    var stealth: Int = STARTING_STEALTH
        private set
    var tools: Int = STARTING_TOOLS
        private set
    var intel: Int = STARTING_INTEL
        private set
    var score: Int = 0
        private set

    private var paused = false
    private var won = false
    private var failed = false
    private var feedback = "Choose an approach"
    private var cacheMapped = false
    private var cachesSecured = 0
    private var cleanMissions = 0

    private val terminal: Boolean
        get() = won || failed

    private val currentMission: Mission
        get() = missions[missionIndex.coerceIn(0, missions.lastIndex)]

    init {
        reset()
    }

    override fun reset() {
        missions = createCampaign()
        missionIndex = 0
        stageIndex = 0
        missionsCompleted = 0
        security = 0
        stealth = STARTING_STEALTH
        tools = STARTING_TOOLS
        intel = STARTING_INTEL
        score = 0
        paused = false
        won = false
        failed = false
        feedback = "Choose an approach"
        cacheMapped = false
        cachesSecured = 0
        cleanMissions = 0
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
                feedback = "Heist paused"
                return true
            }
            GameAction.RESUME -> {
                if (terminal || !paused) return false
                paused = false
                feedback = "Heist resumed"
                return true
            }
            else -> Unit
        }

        if (terminal || paused || stageIndex !in 0 until STAGES_PER_MISSION) return false
        val choice = choiceFor(action) ?: return false
        if (!choice.canAfford(stealth, tools, intel)) return false

        stealth -= choice.stealthCost
        tools -= choice.toolsCost
        intel -= choice.intelCost
        security = (security + choice.securityRisk).coerceIn(0, currentMission.securityLimit)
        score = safeAdd(score, choice.points * currentMission.difficulty)
        if (stageIndex == 0 && action == GameAction.CELL_1) cacheMapped = true

        if (security >= currentMission.securityLimit) {
            failed = true
            feedback = "Security lockdown — ${currentMission.codename} detected the breach"
            return true
        }

        val resolvedStage = stageIndex
        stageIndex++
        if (resolvedStage == STAGES_PER_MISSION - 1) {
            completeMission(cacheMapped && action == GameAction.CELL_2)
        } else if (!hasAffordableChoice(stageIndex, stealth, tools, intel)) {
            failed = true
            feedback = "Resources exhausted — no safe ${stageFor(stageIndex).label.lowercase()} route remains"
        } else {
            feedback = "${stageFor(stageIndex).label} ready · Security $security/${currentMission.securityLimit}${if (cacheMapped) " · side cache mapped" else ""}"
        }
        return true
    }

    private fun completeMission(securedCache: Boolean) {
        val completedMission = currentMission
        val clean = security == 0
        if (securedCache) cachesSecured++
        if (clean) cleanMissions++
        val bonus = (if (securedCache) 50 else 0) + (if (clean) 25 else 0)
        val reward = completedMission.loot + completedMission.difficulty * 10 + bonus
        score = safeAdd(score, reward)
        missionsCompleted++

        if (missionIndex == TOTAL_MISSIONS - 1) {
            stageIndex = STAGES_PER_MISSION
            won = true
            feedback = "Campaign clear — ${completedMission.target} payload secured · +$reward"
            return
        }

        missionIndex++
        stageIndex = 0
        cacheMapped = false
        security = 0
        stealth = (stealth + 2).coerceAtMost(MAX_STEALTH)
        tools = (tools + 1).coerceAtMost(MAX_TOOLS)
        intel = (intel + 1).coerceAtMost(MAX_INTEL)

        if (!hasAffordableChoice(stageIndex, stealth, tools, intel)) {
            failed = true
            feedback = "Resources exhausted — the next mission has no viable entry"
        } else {
            feedback = "Mission $missionsCompleted cleared · +$reward${if (bonus > 0) " (bonus $bonus)" else ""} · resupply S+2 T+1 I+1"
        }
    }

    override fun render(): GameViewState {
        val mission = currentMission
        val stage = stageFor(stageIndex)
        val subtitle = when {
            won -> "Campaign complete — replay to plan another heist"
            failed -> "Heist failed at mission ${missionIndex + 1}/$TOTAL_MISSIONS — replay to try again"
            paused -> "Paused · mission ${missionIndex + 1}/$TOTAL_MISSIONS · ${stage.label}"
            else -> "Mission ${missionIndex + 1}/$TOTAL_MISSIONS · ${mission.codename} · ${stage.label}: ${stage.prompt}"
        }
        val buttons = when {
            terminal -> listOf("REPLAY")
            paused -> listOf("RESUME", "RESET")
            else -> choicesFor(stageIndex).map { choice ->
                val costs = buildList {
                    if (choice.stealthCost > 0) add("S${choice.stealthCost}")
                    if (choice.toolsCost > 0) add("T${choice.toolsCost}")
                    if (choice.intelCost > 0) add("I${choice.intelCost}")
                    add("A${if (choice.securityRisk > 0) "+" else ""}${choice.securityRisk}")
                }
                "${choice.label} · ${costs.joinToString(" ")}${if (security + choice.securityRisk >= mission.securityLimit) " !LOCKDOWN" else ""}"
            }
        }
        val actions = when {
            terminal -> listOf(GameAction.NEW_GAME)
            paused -> listOf(GameAction.RESUME, GameAction.RESET)
            else -> listOf(
                GameAction.PRIMARY,
                GameAction.SECONDARY,
                GameAction.CELL_1,
                GameAction.CELL_2,
            )
        }
        return GameViewState(
            title = title,
            subtitle = subtitle,
            scoreLine = "Security $security/${mission.securityLimit} · Stealth(S) $stealth · Tools(T) $tools · Intel(I) $intel · Score $score",
            buttons = buttons,
            buttonActions = actions,
            feedback = feedback + if (terminal || paused) "" else {
                "\nOptional: ${if (cacheMapped) "DATA DUMP secures cache +50" else "DEEP SCAN → DATA DUMP +50"} · finish at alarm(A) 0 +25"
            },
            isTerminal = terminal,
            phase = when {
                won -> GamePhase.WON
                failed -> GamePhase.LOST
                paused -> GamePhase.PAUSED
                else -> GamePhase.PLAYING
            },
            progress = GameProgress(missionIndex * STAGES_PER_MISSION + stageIndex, TOTAL_MISSIONS * STAGES_PER_MISSION, "Secure 3 payloads · caches $cachesSecured · clean $cleanMissions"),
            record = score.takeIf { it > 0 }?.let { GameRecord(it, "Heist score") },
            disabledActions = if (terminal || paused) emptyMap() else buildMap {
                actions.zip(choicesFor(stageIndex)).forEach { (action, choice) ->
                    if (!choice.canAfford(stealth, tools, intel)) {
                        put(action, "Need S${choice.stealthCost} T${choice.toolsCost} I${choice.intelCost}")
                    }
                }
            },
        )
    }

    override fun encode(): String = JSONObject().apply {
        put("v", VERSION)
        put("missions", JSONArray().apply {
            missions.forEach { mission ->
                put(JSONObject().apply {
                    put("codename", mission.codename)
                    put("target", mission.target)
                    put("securityLimit", mission.securityLimit)
                    put("difficulty", mission.difficulty)
                    put("loot", mission.loot)
                })
            }
        })
        put("missionIndex", missionIndex)
        put("stageIndex", stageIndex)
        put("missionsCompleted", missionsCompleted)
        put("security", security)
        put("stealth", stealth)
        put("tools", tools)
        put("intel", intel)
        put("score", score)
        put("paused", paused)
        put("won", won)
        put("failed", failed)
        put("feedback", feedback)
        put("cacheMapped", cacheMapped)
        put("cachesSecured", cachesSecured)
        put("cleanMissions", cleanMissions)
    }.toString()

    override fun restore(encoded: String): Boolean = runCatching {
        val json = JSONObject(encoded)
        val version = json.optInt("v", -1)
        if (version !in 1..VERSION) return false

        val missionArray = json.optJSONArray("missions") ?: return false
        if (missionArray.length() != TOTAL_MISSIONS) return false
        val restoredMissions = List(missionArray.length()) { index ->
            val item = missionArray.optJSONObject(index) ?: return false
            val codename = item.optString("codename")
            val target = item.optString("target")
            val securityLimit = item.optInt("securityLimit", -1)
            val difficulty = item.optInt("difficulty", -1)
            val loot = item.optInt("loot", -1)
            if (codename.isBlank() || target.isBlank() ||
                securityLimit !in 1..MAX_SECURITY || difficulty !in 1..3 || loot !in 0..100_000
            ) return false
            Mission(codename, target, securityLimit, difficulty, loot)
        }

        val restoredMissionIndex = json.optInt("missionIndex", -1)
        val restoredStageIndex = json.optInt("stageIndex", -1)
        val restoredCompleted = json.optInt("missionsCompleted", -1)
        val restoredSecurity = json.optInt("security", -1)
        val restoredStealth = json.optInt("stealth", -1)
        val restoredTools = json.optInt("tools", -1)
        val restoredIntel = json.optInt("intel", -1)
        val restoredScore = json.optInt("score", -1)
        val restoredPaused = json.optBoolean("paused", false)
        val restoredWon = json.optBoolean("won", false)
        val restoredFailed = json.optBoolean("failed", false)
        val restoredFeedback = json.optString("feedback")
        val restoredCache = if (version >= 2) json.optBoolean("cacheMapped", false) else false
        val restoredCaches = if (version >= 2) json.optInt("cachesSecured", -1) else 0
        val restoredClean = if (version >= 2) json.optInt("cleanMissions", -1) else 0

        if (restoredMissionIndex !in 0 until TOTAL_MISSIONS ||
            restoredStageIndex !in 0..STAGES_PER_MISSION ||
            restoredSecurity !in 0..restoredMissions[restoredMissionIndex].securityLimit ||
            restoredStealth !in 0..MAX_STEALTH || restoredTools !in 0..MAX_TOOLS ||
            restoredIntel !in 0..MAX_INTEL || restoredScore !in 0..MAX_SCORE ||
            restoredCompleted !in 0..TOTAL_MISSIONS || restoredFeedback.isBlank() ||
            restoredWon && restoredFailed || restoredPaused && (restoredWon || restoredFailed)
        ) return false
        if (restoredCaches !in 0..restoredCompleted || restoredClean !in 0..restoredCompleted ||
            restoredCache && restoredStageIndex == 0
        ) return false

        val current = restoredMissions[restoredMissionIndex]
        val active = !restoredWon && !restoredFailed
        val expectedCompleted = if (restoredWon) TOTAL_MISSIONS else restoredMissionIndex
        if (restoredCompleted != expectedCompleted) return false

        if (restoredWon) {
            if (restoredMissionIndex != TOTAL_MISSIONS - 1 || restoredStageIndex != STAGES_PER_MISSION) return false
        } else if (restoredStageIndex == STAGES_PER_MISSION) {
            return false
        }

        if (active) {
            if (restoredStageIndex !in 0 until STAGES_PER_MISSION ||
                !hasAffordableChoice(
                    restoredStageIndex,
                    restoredStealth,
                    restoredTools,
                    restoredIntel,
                    current,
                    restoredSecurity,
                    restoredCache,
                )
            ) return false
        } else if (restoredFailed) {
            val securityFailure = restoredSecurity >= current.securityLimit
            val resourceFailure = restoredStageIndex in 0 until STAGES_PER_MISSION &&
                !hasAffordableChoice(
                    restoredStageIndex,
                    restoredStealth,
                    restoredTools,
                    restoredIntel,
                    current,
                    restoredSecurity,
                    restoredCache,
                )
            if (!securityFailure && !resourceFailure) return false
        }

        missions = restoredMissions
        missionIndex = restoredMissionIndex
        stageIndex = restoredStageIndex
        missionsCompleted = restoredCompleted
        security = restoredSecurity
        stealth = restoredStealth
        tools = restoredTools
        intel = restoredIntel
        score = restoredScore
        paused = restoredPaused
        won = restoredWon
        failed = restoredFailed
        feedback = restoredFeedback
        cacheMapped = restoredCache
        cachesSecured = restoredCaches
        cleanMissions = restoredClean
        true
    }.getOrDefault(false)

    private fun createCampaign(): List<Mission> = List(TOTAL_MISSIONS) { index ->
        val blueprint = blueprints[random.nextInt(blueprints.size)]
        Mission(
            codename = "${blueprint.codename}-${index + 1}",
            target = blueprint.target,
            securityLimit = 6 + index,
            difficulty = index + 1,
            loot = 80 + random.nextInt(121),
        )
    }

    private fun stageFor(index: Int): Stage = when (index) {
        0 -> Stage.RECON
        1 -> Stage.BREACH
        else -> Stage.EXTRACT
    }

    private fun choicesFor(index: Int, mapped: Boolean = cacheMapped): List<Choice> = when (index) {
        0 -> listOf(
            Choice("PASSIVE SCAN", 0, 0, 1, 0, 8),
            Choice("GHOST PING", 1, 0, 0, 1, 10),
            Choice("DEEP SCAN", 0, 1, 1, 0, 14),
            Choice("SPOOF ID", 1, 1, 0, 2, 16),
        )
        1 -> listOf(
            Choice("CREDENTIALS", 0, 1, 1, 1, 18),
            Choice("ZERO-DAY", 0, 2, 0, 2, 24),
            Choice("SOCIAL LOOP", 2, 0, 1, 0, 20),
            Choice("BRUTE FORCE", 0, 1, 0, 3, 27),
        )
        else -> listOf(
            Choice("CLEAN EXIT", 2, 1, 0, -1, 25),
            Choice("FAST EXFIL", 1, 0, 1, 2, 22),
            Choice("DECOY ROUTE", 0, 1, 1, 0, 20),
            Choice("DATA DUMP", 0, 1, if (mapped) 1 else 0, if (mapped) 3 else 1, 26),
        )
    }

    private fun choiceFor(action: GameAction): Choice? = when (action) {
        GameAction.PRIMARY -> choicesFor(stageIndex).getOrNull(0)
        GameAction.SECONDARY -> choicesFor(stageIndex).getOrNull(1)
        GameAction.CELL_1 -> choicesFor(stageIndex).getOrNull(2)
        GameAction.CELL_2 -> choicesFor(stageIndex).getOrNull(3)
        else -> null
    }

    private fun hasAffordableChoice(
        index: Int,
        currentStealth: Int,
        currentTools: Int,
        currentIntel: Int,
        mission: Mission = currentMission,
        currentSecurity: Int = security,
        mapped: Boolean = cacheMapped,
    ): Boolean = choicesFor(index, mapped).any { choice ->
        choice.canAfford(currentStealth, currentTools, currentIntel) &&
            currentSecurityForCheck(currentSecurity = currentSecurity, mission = mission, choice = choice)
    }

    private fun currentSecurityForCheck(currentSecurity: Int, mission: Mission, choice: Choice): Boolean =
        currentSecurity + choice.securityRisk < mission.securityLimit

    private fun safeAdd(left: Int, right: Int): Int =
        if (right > MAX_SCORE - left) MAX_SCORE else left + right
}

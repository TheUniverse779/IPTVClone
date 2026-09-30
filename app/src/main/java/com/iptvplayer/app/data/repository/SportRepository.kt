package com.iptvplayer.app.data.repository

import com.iptvplayer.app.data.database.AppDatabase
import com.iptvplayer.app.data.database.FavouriteMatchEntity
import com.iptvplayer.app.data.network.HttpClients
import com.iptvplayer.app.data.network.dto.EspnEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class League(val slug: String, val name: String, val sport: String, val hot: Boolean)

enum class MatchState { PRE, LIVE, POST }

data class MatchEvent(val minute: String, val text: String, val home: Boolean, val kind: String)

data class Match(
    val id: String,
    val league: League,
    val home: String,
    val away: String,
    val homeShort: String,
    val awayShort: String,
    val homeColor: Int?,
    val awayColor: Int?,
    val homeScore: String?,
    val awayScore: String?,
    val state: MatchState,
    val clock: String,
    val startTime: Long,
    val venue: String?,
    val events: List<MatchEvent>,
)

@Singleton
class SportRepository @Inject constructor(private val db: AppDatabase, private val http: HttpClients) {

    /** Default catalogue; the original loaded this from Remote Config `sport_data`. */
    val leagues = listOf(
        League("eng.1", "Premier League", "soccer", true),
        League("esp.1", "LaLiga", "soccer", true),
        League("ita.1", "Serie A", "soccer", true),
        League("uefa.champions", "Champions League", "soccer", true),
        League("ger.1", "Bundesliga", "soccer", false),
        League("fra.1", "Ligue 1", "soccer", false),
        League("nba", "NBA", "basketball", false),
    )

    fun observeFavourites() = db.matchDao().observeAll()
    suspend fun follow(m: Match) = db.matchDao().add(FavouriteMatchEntity(m.id, m.league.slug, m.league.sport, m.home, m.away, m.startTime))
    suspend fun unfollow(id: String) = db.matchDao().remove(id)

    /** league slug + yyyyMM → (fetchedAt, matches). Month pages are small and shared by every screen. */
    private val cache = ConcurrentHashMap<String, Pair<Long, List<Match>>>()

    /**
     * Matches with kick-off in [from, to). ESPN rejects `yyyyMMdd-yyyyMMdd` ranges (400), but accepts
     * a whole month `yyyyMM`, so we fetch the months the range touches and filter locally.
     * Live matches are always kept so a game that kicked off before [from] still shows.
     */
    suspend fun matches(slugs: Collection<String>, from: Long, to: Long, maxAgeMs: Long = 60_000): List<Match> = withContext(Dispatchers.IO) {
        val months = monthKeys(from, to)
        coroutineScope {
            leagues.filter { it.slug in slugs }.flatMap { lg -> months.map { lg to it } }.map { (lg, month) ->
                async { month(lg, month, maxAgeMs) }
            }.awaitAll().flatten()
        }.filter { it.state == MatchState.LIVE || it.startTime in from until to }.distinctBy { it.id }.sortedBy { it.startTime }
    }

    private suspend fun month(lg: League, month: String, maxAgeMs: Long): List<Match> {
        val key = "${lg.slug}/$month"
        cache[key]?.let { (t, list) -> if (System.currentTimeMillis() - t < maxAgeMs) return list }
        val list = runCatching { http.espn.scoreboard(lg.sport, lg.slug, month).events.orEmpty().mapNotNull { it.toMatch(lg) } }.getOrNull()
            ?: return cache[key]?.second.orEmpty()
        cache[key] = System.currentTimeMillis() to list
        return list
    }

    suspend fun match(slug: String, eventId: String, around: Long): Match? =
        matches(listOf(slug), around - 2 * DAY, around + 9 * DAY, maxAgeMs = 20_000).firstOrNull { it.id == eventId }

    private fun EspnEvent.toMatch(lg: League): Match? {
        val comp = competitions?.firstOrNull() ?: return null
        val home = comp.competitors?.firstOrNull { it.homeAway == "home" } ?: return null
        val away = comp.competitors.firstOrNull { it.homeAway == "away" } ?: return null
        val st = (comp.status ?: status)?.type
        val state = when (st?.state) { "in" -> MatchState.LIVE; "post" -> MatchState.POST; else -> MatchState.PRE }
        val events = comp.details.orEmpty().mapNotNull { d ->
            val kind = when { d.scoringPlay == true -> "goal"; d.redCard == true -> "red"; d.yellowCard == true -> "yellow"; else -> return@mapNotNull null }
            MatchEvent(d.clock?.displayValue.orEmpty(), d.athletes?.firstOrNull()?.displayName ?: d.type?.text.orEmpty(), d.team?.id == home.team?.id, kind)
        }
        return Match(
            id = id ?: return null, league = lg,
            home = home.team?.displayName.orEmpty(), away = away.team?.displayName.orEmpty(),
            homeShort = home.team?.abbreviation ?: home.team?.shortName.orEmpty().take(3), awayShort = away.team?.abbreviation ?: away.team?.shortName.orEmpty().take(3),
            homeColor = home.team?.color?.let { parseColor(it) }, awayColor = away.team?.color?.let { parseColor(it) },
            homeScore = home.score, awayScore = away.score, state = state,
            clock = if (state == MatchState.LIVE) (comp.status ?: status)?.displayClock.orEmpty() else st?.shortDetail.orEmpty(),
            startTime = parseDate(date), venue = comp.venue?.fullName, events = events,
        )
    }

    private fun parseColor(hex: String) = runCatching { (0xFF000000 or hex.toLong(16)).toInt() }.getOrNull()

    private fun parseDate(s: String?): Long = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(s!!)!!.time
    }.getOrDefault(0L)

    companion object {
        const val DAY = 86_400_000L

        /** Local midnight of the day containing [t], shifted by [days]. */
        fun startOfDay(t: Long = System.currentTimeMillis(), days: Int = 0): Long = Calendar.getInstance().run {
            timeInMillis = t; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, days); timeInMillis
        }

        /** ESPN month keys (UTC, like its `date` field) covering [from, to). */
        fun monthKeys(from: Long, to: Long): List<String> {
            val f = SimpleDateFormat("yyyyMM", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            return generateSequence(from) { it + DAY }.takeWhile { it < to }.plus(to - 1).map { f.format(Date(it)) }.distinct().toList()
        }
    }
}

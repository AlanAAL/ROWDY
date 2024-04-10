package com.RowdyAvocado

// import android.util.Log
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class CineZone(val plugin: CineZonePlugin) : MainAPI() {
    override var mainUrl = CineZone.mainUrl
    override var name = CineZone.name
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    companion object {
        val mainUrl = "https://cinezone.to"
        var name = "CineZone"
    }

    private fun searchResponseBuilder(webDocument: Document): List<SearchResponse> {
        val searchCollection =
                webDocument.select("div.item").mapNotNull { element ->
                    val title = element.selectFirst("a.title")?.text() ?: ""
                    val link = mainUrl + element.selectFirst("a.title")?.attr("href")
                    newMovieSearchResponse(title, link) {
                        this.posterUrl = element.selectFirst("img")?.attr("data-src")
                        this.quality = getQualityFromString(element.selectFirst("b")?.text())
                    }
                }
        return searchCollection
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/filter?keyword=$query"
        val res = app.get(url)
        return searchResponseBuilder(res.document)
    }

    override val mainPage =
            mainPageOf(
                    "$mainUrl/movie?page=" to "Recently updated movies",
                    "$mainUrl/tv?page=" to "Recently updated TV shows",
                    "$mainUrl/filter?type[]=movie&sort=trending&page=" to "Trending movies",
                    "$mainUrl/filter?type[]=tv&sort=trending&page=" to "Trending TV shows"
            )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val res = app.get(url)
        if (res.code != 200) throw ErrorLoadingException("Could not load data")
        val home = searchResponseBuilder(res.document)
        return newHomePageResponse(HomePageList(request.name, home), true)
    }

    override suspend fun load(url: String): LoadResponse {
        val res = app.get(url)

        if (res.code != 200) throw ErrorLoadingException("Could not load data" + url)
        res.document.selectFirst("section#playerDetail")?.let { details ->
            val contentId = res.document.select("body main > div.container").attr("data-id")
            val name = details.selectFirst("div.body > h1")?.ownText() ?: ""
            val releaseDate = details.select("div.meta div > div:contains(Release:) + span").text()
            val bgPosterData = res.document.selectFirst("div.playerBG")?.attr("style")
            val bgPoster = Regex("'(http.*)'").find(bgPosterData ?: "")?.destructured?.component1()

            if (url.contains("movie")) {
                val id =
                        apiCall("episode/list", contentId)
                                ?.selectFirst("ul.episodes > li > a")
                                ?.attr("data-id")
                return newMovieLoadResponse(name, url, TvType.Movie, id) {
                    this.plot = details.select("div.description").text()
                    this.year = releaseDate.split(",").get(1).trim().toInt()
                    this.posterUrl = res.document.select("div.poster > div > img").attr("src")
                    this.backgroundPosterUrl = bgPoster ?: posterUrl
                    this.rating =
                            details.selectFirst("span.imdb")?.text()?.trim()?.toFloat()?.toInt()
                    this.recommendations = searchResponseBuilder(res.document)
                }
            } else {
                val episodes = mutableListOf<Episode>()
                apiCall("episode/list", contentId)?.select("ul.episodes")?.forEach { season ->
                    season.select("li > a").forEach { episode ->
                        val epId = episode.attr("data-id")
                        episodes.add(
                                newEpisode(epId) {
                                    this.name = episode.select("span").text()
                                    this.season = season.attr("data-season").trim().toInt()
                                    this.episode = episode.attr("data-num").trim().toInt()
                                    this.data = epId
                                }
                        )
                    }
                }
                return newTvSeriesLoadResponse(name, url, TvType.TvSeries, episodes) {
                    this.plot = details.select("div.description").text()
                    this.year = releaseDate.split(",").get(1).trim().toInt()
                    this.posterUrl = res.document.select("div.poster > div > img").attr("src")
                    this.backgroundPosterUrl = bgPoster ?: posterUrl
                    this.rating =
                            details.selectFirst("span.imdb")?.text()?.trim()?.toFloat()?.toInt()
                    this.recommendations = searchResponseBuilder(res.document)
                }
            }
        }
        throw ErrorLoadingException("Could not load data" + url)
    }

    override suspend fun loadLinks(
            data: String,
            isCasting: Boolean,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ): Boolean {
        Log.d("rowdy", data)
        val serversRes = apiCall("server/list", data.replace(mainUrl + "/", ""))
        Log.d("rowdy", serversRes.toString())
        serversRes?.select("span.server")?.forEach { server ->
            val sName = serverName(server.attr("data-id"))
            val sId = server.attr("data-link-id")
            val url = getServerUrl(sId)
            CineZoneExtractor().getUrl(url, sName, subtitleCallback, callback)
        }
        return true
    }

    private suspend fun apiCall(prefix: String, data: String): Document? {
        val vrf = CineZoneUtils.vrfEncrypt(data)
        val res = app.get("$mainUrl/ajax/$prefix/$data?vrf=$vrf").parsedSafe<APIResponseHTML>()
        if (res?.status == 200) {
            return res.html
        }
        return null
    }

    private suspend fun getServerUrl(data: String): String {
        val vrf = CineZoneUtils.vrfEncrypt(data)
        val res = app.get("$mainUrl/ajax/server/$data?vrf=$vrf").parsedSafe<APIResponseJSON>()
        if (res?.status == 200) {
            return CineZoneUtils.vrfDecrypt(res.result.url)
        }
        return ""
    }

    private fun serverName(serverID: String?): String? {
        val sss =
                when (serverID) {
                    "41" -> "vidplay"
                    "45" -> "filemoon"
                    "40" -> "streamtape"
                    "35" -> "mp4upload"
                    "28" -> "mycloud"
                    else -> null
                }
        return sss
    }

    data class APIResponseHTML(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: String,
            val html: Document = Jsoup.parse(result)
    )

    data class APIResponseJSON(
            @JsonProperty("status") val status: Int,
            @JsonProperty("result") val result: ServerUrl,
    )

    data class ServerUrl(
            @JsonProperty("url") val url: String,
    )
}

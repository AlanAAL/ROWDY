package com.RowdyAvocado

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.extractors.FileMoon
import com.lagradost.cloudstream3.extractors.Vidplay
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class BfliXExtractor : ExtractorApi() {
    override val mainUrl = BfliX.mainUrl
    override val name = BfliX.name
    override val requiresReferer = false

    override suspend fun getUrl(
            url: String,
            referer: String?,
            subtitleCallback: (SubtitleFile) -> Unit,
            callback: (ExtractorLink) -> Unit
    ) {
        val serverName = referer
        if (url.isNotEmpty()) {
            when (serverName) {
                null -> {}
                "filemoon" -> FileMoon().getUrl(url, null, subtitleCallback, callback)
                "vidplay" -> Vidplay2().getUrl(url, referer, subtitleCallback, callback)
                else -> loadExtractor(url, subtitleCallback, callback)
            }
        }
    }
}

open class Vidplay2 : Vidplay() {
    override val name = "VidPlay"
    override val mainUrl = "https://vid41c.site"
    override val requiresReferer = true
}

package com.RowdyAvocado

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.app

class AllWishExtractor {
    suspend fun getStreamUrl(serverName: String?, dataId: String): List<String> {
        var links = emptySet<String>()
        val res =
                app.get("${AllWish.mainUrl}/ajax/server?get=$dataId", AllWish.header)
                        .parsedSafe<APIResponse>()
        val serverUrl = res?.result?.url
        if (!serverUrl.isNullOrEmpty()) {
            when (serverName) {
                "VidPlay" -> {}
                "Vidstreaming" -> {}
                "Gogo server" -> {}
                "Streamwish" -> {
                    val serverRes = app.get(serverUrl)
                    if (serverRes.code == 200) {
                        val doc = serverRes.document
                        val script =
                                doc.selectFirst("script:containsData(sources)")?.data().toString()
                        Regex("file:\"(.*?)\"").find(script)?.groupValues?.get(1)?.let { link ->
                            Log.d("rowdyLink", link)
                            links += link
                        }
                    }
                }
                "Mp4Upload" -> {
                    links += serverUrl
                }
                "Doodstream" -> {
                    links += serverUrl
                }
                "Filelions" -> {
                    links += serverUrl
                }
                else -> Log.d("rowdy", "no match")
            }
        }
        return links.toList()
    }

    data class APIResponse(
            @JsonProperty("status") val status: Int? = null,
            @JsonProperty("result") val result: ServerUrl? = null,
    )

    data class ServerUrl(
            @JsonProperty("url") val url: String? = null,
    )
}

// {
//     "status": 200,
//     "result": {
//         "url":
// "https:\/\/cdn.animixplay.tube\/player\/?id=6e696e6a612d6b616d75692d657069736f64652d37",
//         "skip_data": {
//             "intro": [
//                 0,
//                 0
//             ],
//             "outro": [
//                 0,
//                 0
//             ]
//         }
//     }
// }

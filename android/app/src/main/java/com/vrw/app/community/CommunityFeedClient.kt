package com.vrw.app.community

import com.vrw.app.settings.LayoutRepository
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Deliberately minimal Phase 6 stand-in: fetches a user-configured static JSON feed URL
 * (e.g. a file hosted free on GitHub) listing community layouts, and lets the user import
 * selected ones into the Controller Builder. No upload UI, accounts, ratings, or moderation -
 * see community/README.md for why this is a stopgap, not a real backend.
 */
object CommunityFeedClient {

    fun fetchFeed(feedUrl: String): List<LayoutRepository.NamedLayout> {
        return try {
            val connection = URL(feedUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseFeed(body)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseFeed(json: String): List<LayoutRepository.NamedLayout> {
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                LayoutRepository.importLayout(array.getJSONObject(i).toString())
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

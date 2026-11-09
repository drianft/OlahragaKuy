package com.example.olahragakuy.core.maps

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.maplibre.android.module.http.HttpRequestUtil
import java.io.File

object MapConfiguration {

    private var initialized = false

    @Synchronized
    fun configureNetwork(context: Context) {
        if (initialized) return

        val cache = Cache(
            directory = File(
                context.applicationContext.cacheDir,
                "map_http_cache"
            ),
            maxSize = 50L * 1024L * 1024L
        )

        val client = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header(
                        "User-Agent",
                        "OlahragaKuy/0.1 (com.example.olahragakuy)"
                    )
                    .build()

                chain.proceed(request)
            }
            .build()

        HttpRequestUtil.setOkHttpClient(client)
        initialized = true
    }

    val osmStyleJson: String = """
        {
          "version": 8,
          "name": "OlahragaKuy OSM",
          "sources": {
            "osm": {
              "type": "raster",
              "tiles": [
                "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
              ],
              "tileSize": 256,
              "minzoom": 0,
              "maxzoom": 19,
              "attribution": "© OpenStreetMap contributors"
            }
          },
          "layers": [
            {
              "id": "osm-tiles",
              "type": "raster",
              "source": "osm"
            }
          ]
        }
    """.trimIndent()
}
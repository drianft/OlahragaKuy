package com.example.olahragakuy.feature.venue.presentation

import com.example.olahragakuy.feature.venue.domain.Venue
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

object VenueMarkerRenderer {

    private const val SOURCE_ID = "venue-source"
    const val LAYER_ID = "venue-markers"

    fun render(style: Style, venues: List<Venue>) {
        val features = venues
            .filter { it.isActive }
            .map { venue ->
                Feature.fromGeometry(
                    Point.fromLngLat(
                        venue.longitude,
                        venue.latitude
                    )
                ).apply {
                    addStringProperty("venueId", venue.id)
                    addStringProperty("name", venue.name)
                }
            }

        val collection = FeatureCollection.fromFeatures(features)
        val source = style.getSourceAs<GeoJsonSource>(SOURCE_ID)

        if (source == null) {
            style.addSource(
                GeoJsonSource(SOURCE_ID, collection)
            )
        } else {
            source.setGeoJson(collection)
        }

        if (style.getLayer(LAYER_ID) == null) {
            style.addLayer(
                CircleLayer(LAYER_ID, SOURCE_ID)
                    .withProperties(
                        circleRadius(10f),
                        circleColor("#FF7A00"),
                        circleStrokeColor("#FFFFFF"),
                        circleStrokeWidth(3f)
                    )
            )
        }
    }
}
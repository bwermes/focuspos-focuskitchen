package com.example.focuskitchen

class VenueLicense(val key: String,
                   val name: String?,
                   val type: String,
                   val venueKey: Int,
                   val dealerKey: String,
                   val mac: String?,
                   val ip: String?,
                   val bumpToId: Int? = 0,
                   val printerId: Int? = 0,
                   val claimed: Boolean,
                   val active: Boolean,
                   val features: Map<String, Any>,
                   val venueName: String? = null,
                   val dealerName: String? = null) {
}
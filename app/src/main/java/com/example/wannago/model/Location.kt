package com.example.wannago.model

import java.io.Serializable

data class Location(
    val id: String,
    val name: String?,
    val latitude: Double,
    val longitude: Double
) : Serializable {
    constructor() : this("", "", 0.0, 0.0)
}


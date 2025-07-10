package com.abi.project.model

data class Courier(
    val id: Int? = null,
    val courierNumber: String,
    val status: String,
    val place: String,
    val deliveryPersonName: String?,
    val deliveryPersonId: String?,
    val userUsername: String,
    val locationUrl: String // Add this field
)

package com.abi.project.model

data class User(
    val id: Int,
    val username: String,
    val password: String,
    val phone: String,
    val isAdmin: Boolean
)

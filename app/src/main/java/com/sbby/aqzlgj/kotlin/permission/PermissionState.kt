package com.sbby.aqzlgj.kotlin.permission

data class PermissionState(
    val notification: Boolean = false,
    val overlay: Boolean = false,
) {
    val requiredGranted: Boolean
        get() = notification && overlay
}

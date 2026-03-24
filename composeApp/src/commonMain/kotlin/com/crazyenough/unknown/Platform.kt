package com.crazyenough.unknown

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
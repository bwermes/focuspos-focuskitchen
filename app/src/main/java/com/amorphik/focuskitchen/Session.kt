package com.amorphik.focuskitchen

import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

class Session {
    var menuItemRecords: List<MenuItemRecord>? = null
    var party =  Party(
        speed = 0f,
        maxSpeed = 30f,
        damping = 0.9f,
        spread = 360,
        colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
        emitter = Emitter(duration = 2, TimeUnit.SECONDS).max(100),
        position = Position.Relative(0.5, 0.3)
    )

    var allDayCountRecords: MutableList<AllDayCountRecord> = arrayListOf()
}
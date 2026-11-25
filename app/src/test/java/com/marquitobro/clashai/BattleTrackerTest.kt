package com.marquitobro.clashai

import org.junit.Assert.*
import org.junit.Test

class BattleTrackerTest {
    @Test fun aliasesMatchExactly() {
        assertEquals("Montapuercos", BattleTracker.recognize("Hog Rider")?.name)
        assertEquals("Cañón", BattleTracker.recognize("Cañón")?.name)
        assertEquals("Gigante noble", BattleTracker.recognize("Gigante noble")?.name)
        assertNull(BattleTracker.recognize("unknown"))
    }
    @Test fun spendAndRegenerate() {
        val t = BattleTracker()
        t.reset(1000)
        t.record(1000, BattleTracker.recognize("Hog Rider")!!)
        assertEquals("1.0 / 10 aprox.", t.estimate(1000))
        assertEquals("2.0 / 10 aprox.", t.estimate(3800))
        t.speed(3800, 2)
        assertEquals("4.0 / 10 aprox.", t.estimate(6600))
        assertEquals("Montapuercos", t.cycle())
        assertEquals("Montapuercos", t.observed())
    }
}

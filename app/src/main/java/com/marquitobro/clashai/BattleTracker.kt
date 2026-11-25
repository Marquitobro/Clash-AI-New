package com.marquitobro.clashai

import java.text.Normalizer
import java.util.Locale

/** Standard eight-card-cycle approximation. Not valid for every special mode. */
class BattleTracker {
    data class Card(val name: String, val cost: Int, val aliases: List<String>)
    companion object {
        val cards = listOf(
            Card("Montapuercos",4,listOf("montapuercos","hog rider")),
            Card("Caballero",3,listOf("caballero","knight")),
            Card("Gigante",5,listOf("gigante","giant")),
            Card("Gigante noble",6,listOf("gigante noble","royal giant")),
            Card("Gigante eléctrico",7,listOf("gigante electrico","electro giant")),
            Card("Mosquetera",4,listOf("mosquetera","musketeer")),
            Card("Valquiria",4,listOf("valquiria","valkyrie")),
            Card("Mini P.E.K.K.A",4,listOf("mini pekka","mini p e k k a")),
            Card("P.E.K.K.A",7,listOf("pekka","p e k k a")),
            Card("Mega caballero",7,listOf("mega caballero","megacaballero","mega knight")),
            Card("Esqueletos",1,listOf("esqueletos","skeletons")),
            Card("Ejército de esqueletos",3,listOf("ejercito de esqueletos","skeleton army")),
            Card("Duendes",2,listOf("duendes","goblins")),
            Card("Barril de duendes",3,listOf("barril de duendes","goblin barrel")),
            Card("Princesa",3,listOf("princesa","princess")),
            Card("Príncipe",5,listOf("principe","prince")),
            Card("Príncipe oscuro",4,listOf("principe oscuro","dark prince")),
            Card("Arqueras",3,listOf("arqueras","archers")),
            Card("Esbirros",3,listOf("esbirros","minions")),
            Card("Dragón bebé",4,listOf("dragon bebe","baby dragon")),
            Card("Dragón infernal",4,listOf("dragon infernal","inferno dragon")),
            Card("Mago",5,listOf("mago","wizard")),
            Card("Mago eléctrico",4,listOf("mago electrico","electro wizard")),
            Card("Bruja",5,listOf("bruja","witch")),
            Card("Bola de fuego",4,listOf("bola de fuego","fireball")),
            Card("Flechas",3,listOf("flechas","arrows")),
            Card("Descarga",2,listOf("descarga","zap")),
            Card("Tronco",2,listOf("tronco","the log")),
            Card("Cohete",6,listOf("cohete","rocket")),
            Card("Veneno",4,listOf("veneno","poison")),
            Card("Cañón",3,listOf("canon","cannon")),
            Card("Tesla",4,listOf("tesla")),
            Card("Torre infernal",5,listOf("torre infernal","inferno tower")),
            Card("Gólem",8,listOf("golem")),
            Card("Globo bombástico",5,listOf("globo bombastico","balloon")),
            Card("Minero",3,listOf("minero","miner")),
            Card("Bandida",3,listOf("bandida","bandit")),
            Card("Leñador",4,listOf("lenador","lumberjack")),
            Card("Cementerio",5,listOf("cementerio","graveyard")),
            Card("Montacarneros",5,listOf("montacarneros","ram rider"))
        )
        private fun normalized(text: String) = Normalizer.normalize(text.lowercase(Locale.ROOT),
            Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").replace(Regex("[^a-z ]")," ")
            .replace(Regex(" +")," ").trim()
        fun recognize(line: String): Card? {
            val key = normalized(line)
            return cards.firstOrNull { card -> card.aliases.any { normalized(it) == key } }
        }
    }
    private var elixir = 5.0
    private var last = 0L
    private var rate = 1
    private val plays = mutableListOf<Card>()
    private val deck = linkedSetOf<Card>()
    val started get() = last != 0L
    fun reset(now: Long) {
        last = now; elixir = 5.0; rate = 1; plays.clear(); deck.clear()
    }
    fun tick(now: Long) {
        if (!started || now < last) return
        elixir = (elixir + (now-last)/2800.0*rate).coerceAtMost(10.0)
        last = now
    }
    fun speed(now: Long, value: Int) { tick(now); rate = value.coerceIn(1,3) }
    fun record(now: Long, card: Card) {
        if (!started) return
        tick(now)
        elixir = (elixir-card.cost).coerceAtLeast(0.0)
        plays.add(card); deck.add(card)
    }
    fun estimate(now: Long): String {
        tick(now)
        return if (started) String.format(Locale.ROOT,"%.1f / 10 aprox.",elixir) else "Sin iniciar"
    }
    fun observed() = deck.joinToString(", ") { it.name }.ifBlank { "Ninguna confirmada" }
    fun cycle() = plays.takeLast(4).joinToString(" → ") { it.name }.ifBlank { "Sin jugadas" }
    fun advice(): String {
        if (!started || plays.isEmpty()) return "Esperando observaciones"
        return if (elixir <= 3) "Rival estimado bajo: considera presionar"
        else if (elixir >= 8) "Rival estimado alto: conserva defensa"
        else "Conserva una respuesta y evita gastar todo"
    }
}

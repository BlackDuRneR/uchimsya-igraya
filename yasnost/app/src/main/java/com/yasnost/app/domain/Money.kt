package com.yasnost.app.domain

import com.yasnost.app.data.AppState
import com.yasnost.app.data.Goal

/** Примерный курс к тенге (Нацбанк РК, ноябрь 2026). */
object Rates {
    private val toKzt = mapOf("₸" to 1.0, "$" to 444.0, "€" to 488.0, "₽" to 5.38, "сом" to 5.07)
    fun toKzt(v: Double, cur: String) = v * (toKzt[cur] ?: 1.0)
    fun fromKzt(v: Double, cur: String) = v / (toKzt[cur] ?: 1.0)
}

data class Thing(val title: String, val priceKzt: Double, val emoji: String)

/** Цены ориентировочные: Алматы, ноябрь 2026. */
object Catalog {
    val things = listOf(
        Thing("Чашка хорошего кофе", 1_500.0, "☕"),
        Thing("Книга", 6_000.0, "📚"),
        Thing("Кино вдвоём", 7_000.0, "🎬"),
        Thing("Сеанс массажа", 15_000.0, "💆"),
        Thing("Полный бак бензина", 16_000.0, "⛽"),
        Thing("Месяц в спортзале", 22_000.0, "🏋️"),
        Thing("Продукты на неделю", 30_000.0, "🛒"),
        Thing("Ужин в ресторане вдвоём", 30_000.0, "🍽️"),
        Thing("Месяц курсов английского", 45_000.0, "🗣️"),
        Thing("Хорошие кроссовки", 50_000.0, "👟"),
        Thing("Беспроводные наушники", 90_000.0, "🎧"),
        Thing("Выходные на Иссык-Куле", 150_000.0, "🏞️"),
        Thing("Умные часы", 150_000.0, "⌚"),
        Thing("Билеты Алматы — Стамбул и обратно", 220_000.0, "✈️"),
        Thing("Ноутбук", 450_000.0, "💻"),
        Thing("Неделя на море", 500_000.0, "🏖️"),
        Thing("Флагманский смартфон", 650_000.0, "📱"),
        Thing("Подержанный автомобиль", 5_000_000.0, "🚗"),
    )

    data class Equivalent(val thing: Thing, val count: Int)

    /** Чем уже можно «заплатить» сэкономленными деньгами. */
    fun equivalents(moneyKzt: Double): List<Equivalent> =
        things.filter { moneyKzt >= it.priceKzt }.map { Equivalent(it, (moneyKzt / it.priceKzt).toInt()) }

    /** Подборка из 3–4 наглядных эквивалентов: самое дорогое + несколько «понятных». */
    fun highlights(moneyKzt: Double, seed: Long): List<Equivalent> {
        val all = equivalents(moneyKzt)
        if (all.isEmpty()) return emptyList()
        val top = all.last()
        val rest = all.dropLast(1).filter { it.count in 2..400 }
        val picked = if (rest.size <= 3) rest else {
            val start = (seed % (rest.size - 2)).toInt()
            rest.subList(start, start + 3)
        }
        return (listOf(top) + picked.reversed()).distinctBy { it.thing.title }
    }

    /** Следующие покупки, которые станут доступны. */
    fun nextTargets(moneyKzt: Double, n: Int = 2) = things.filter { it.priceKzt > moneyKzt }.take(n)
}

data class GoalProgress(val goal: Goal, val funded: Double, val daysLeft: Long?) {
    val done get() = funded >= goal.price
    val fraction get() = if (goal.price <= 0) 1f else (funded / goal.price).toFloat().coerceIn(0f, 1f)
}

object Money {
    fun total(s: AppState, now: Long) = s.habits.sumOf { it.stats(now).money }
    fun daily(s: AppState) = s.habits.sumOf { it.costPerDay }

    /** Цели заполняются по очереди из общей суммы сбережений. */
    fun goals(s: AppState, now: Long): List<GoalProgress> {
        var left = total(s, now)
        val perDay = daily(s)
        var cumulative = 0.0
        return s.goals.sortedBy { it.createdMillis }.map { g ->
            val funded = minOf(left, g.price).coerceAtLeast(0.0)
            left -= funded
            cumulative += g.price - funded
            val days = if (funded >= g.price) 0L else if (perDay > 0) kotlin.math.ceil(cumulative / perDay).toLong() else null
            GoalProgress(g, funded, days)
        }
    }

    fun idea(moneyKzt: Double): String = when {
        moneyKzt < 7_000 -> "Пока скромно, но это уже реальные деньги, которые остались у тебя, а не ушли в дым и похмелье."
        moneyKzt < 50_000 -> "Этого хватит, чтобы порадовать себя чем-то полезным — абонементом, хорошей книгой или ужином с близким человеком."
        moneyKzt < 200_000 -> "Это уже ощутимая сумма: хватит на короткую поездку или вещь, о которой давно думал."
        moneyKzt < 700_000 -> "Это отпуск, новый ноутбук или солидная подушка безопасности. Всё — благодаря одному решению."
        else -> "Это серьёзный капитал. Подумай, как он может изменить твою жизнь: обучение, жильё, машина, своё дело."
    }
}

package com.rumahku.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class HairStyle(val label: String) {
    SHORT("Pendek"), LONG("Panjang"), BUN("Cepol"), CAP("Topi"),
}

enum class GuestActivity(val label: String, val emoji: String) {
    RAMEN("Makan ramen", "🍜"), TEA("Minum teh", "🍵"), PHONE("Main HP", "📱"),
}

data class Guest(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val hairStyle: HairStyle = HairStyle.SHORT,
    val hairColor: Int = HAIR_COLORS[0],
    val outfitColor: Int = OUTFIT_COLORS[0],
    val activity: GuestActivity = GuestActivity.RAMEN,
) {
    val tag: String
        get() {
            var out = ""
            for (ch in name.uppercase().filter { PixelFont.supports(it) }.trim()) {
                if (PixelFont.width(out + ch) > TAG_MAX_WIDTH) break
                out += ch
            }
            return out.trim()
        }

    companion object {
        const val TAG_MAX_WIDTH = 20
        val HAIR_COLORS = intArrayOf(0x2E2230, 0x6B4226, 0xC8843A, 0xF2D27A, 0xE86A9A, 0x6AA8F0, 0xF4F1EA)
        val OUTFIT_COLORS = intArrayOf(0xF2C94C, 0xD9483B, 0x8A5AC8, 0x4FB06A, 0x3E8FD9, 0xF29BB5, 0x2E3A5C, 0xF4F1EA)
    }
}

object GuestStore {
    const val MAX = 3
    private const val PREFS = "kedai"
    private const val KEY = "guests"

    fun load(ctx: Context): List<Guest> {
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i -> fromJson(array.getJSONObject(i)) }
        }.getOrDefault(emptyList())
    }

    fun find(ctx: Context, id: String) = load(ctx).firstOrNull { it.id == id }

    fun upsert(ctx: Context, guest: Guest) {
        val list = load(ctx).toMutableList()
        val index = list.indexOfFirst { it.id == guest.id }
        when {
            index >= 0 -> list[index] = guest
            list.size < MAX -> list.add(guest)
            else -> return
        }
        save(ctx, list)
    }

    fun delete(ctx: Context, id: String) = save(ctx, load(ctx).filter { it.id != id })

    private fun save(ctx: Context, list: List<Guest>) {
        val array = JSONArray()
        list.forEach { array.put(toJson(it)) }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    private fun toJson(g: Guest) = JSONObject()
        .put("id", g.id)
        .put("name", g.name)
        .put("hairStyle", g.hairStyle.name)
        .put("hairColor", g.hairColor)
        .put("outfitColor", g.outfitColor)
        .put("activity", g.activity.name)

    private fun fromJson(o: JSONObject) = Guest(
        id = o.getString("id"),
        name = o.optString("name"),
        hairStyle = runCatching { HairStyle.valueOf(o.getString("hairStyle")) }.getOrDefault(HairStyle.SHORT),
        hairColor = o.optInt("hairColor", Guest.HAIR_COLORS[0]),
        outfitColor = o.optInt("outfitColor", Guest.OUTFIT_COLORS[0]),
        activity = runCatching { GuestActivity.valueOf(o.getString("activity")) }.getOrDefault(GuestActivity.RAMEN),
    )
}

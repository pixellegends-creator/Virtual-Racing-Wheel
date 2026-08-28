package com.vrw.app.settings

import com.vrw.app.builder.LayoutElement
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists Controller Builder layouts as JSON. Multiple named layouts are supported; one is
 * marked active at a time and is what WheelScreen renders.
 *
 * Note: in the real app this is backed by SharedPreferences/DataStore; here the storage backend
 * is abstracted behind [Store] so the JSON (de)serialization logic - the part that actually broke
 * once when duplicateCurrentLayout() was left with an empty body - can be unit tested directly.
 */
object LayoutRepository {

    interface Store {
        fun getRawLayouts(): String?
        fun setRawLayouts(json: String)
        fun getActiveLayoutName(): String?
        fun setActiveLayoutName(name: String)
    }

    var store: Store = InMemoryStore()

    data class NamedLayout(val name: String, val elements: List<LayoutElement>)

    fun getAllLayouts(): List<NamedLayout> {
        val raw = store.getRawLayouts() ?: return emptyList()
        return deserialize(raw)
    }

    fun getActiveLayout(): List<LayoutElement>? {
        val activeName = store.getActiveLayoutName() ?: return null
        return getAllLayouts().find { it.name == activeName }?.elements
    }

    fun saveLayout(layout: NamedLayout) {
        val all = getAllLayouts().filterNot { it.name == layout.name } + layout
        store.setRawLayouts(serialize(all))
    }

    fun deleteLayout(name: String) {
        val all = getAllLayouts().filterNot { it.name == name }
        store.setRawLayouts(serialize(all))
    }

    fun setActive(name: String) {
        store.setActiveLayoutName(name)
    }

    fun duplicateCurrentLayout(newName: String) {
        val activeName = store.getActiveLayoutName() ?: return
        val current = getAllLayouts().find { it.name == activeName } ?: return
        saveLayout(NamedLayout(name = newName, elements = current.elements))
    }

    fun exportLayout(layout: NamedLayout): String = serializeOne(layout)

    fun importLayout(json: String): NamedLayout? = try {
        deserializeOne(JSONObject(json))
    } catch (_: Exception) {
        null
    }

    // --- JSON (de)serialization ---

    private fun serialize(layouts: List<NamedLayout>): String {
        val array = JSONArray()
        layouts.forEach { array.put(layoutToJson(it)) }
        return array.toString()
    }

    private fun serializeOne(layout: NamedLayout): String = layoutToJson(layout).toString()

    private fun layoutToJson(layout: NamedLayout): JSONObject {
        val obj = JSONObject()
        obj.put("name", layout.name)
        val elementsArray = JSONArray()
        layout.elements.forEach { e ->
            val eo = JSONObject()
            eo.put("id", e.id)
            eo.put("type", e.type)
            eo.put("x", e.x)
            eo.put("y", e.y)
            eo.put("width", e.width)
            eo.put("height", e.height)
            elementsArray.put(eo)
        }
        obj.put("elements", elementsArray)
        return obj
    }

    private fun deserialize(raw: String): List<NamedLayout> {
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i -> deserializeOne(array.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun deserializeOne(obj: JSONObject): NamedLayout? {
        return try {
            val name = obj.getString("name")
            val elementsArray = obj.getJSONArray("elements")
            val elements = (0 until elementsArray.length()).map { i ->
                val eo = elementsArray.getJSONObject(i)
                LayoutElement(
                    id = eo.getString("id"),
                    type = eo.getString("type"),
                    x = eo.getDouble("x").toFloat(),
                    y = eo.getDouble("y").toFloat(),
                    width = eo.getDouble("width").toFloat(),
                    height = eo.getDouble("height").toFloat()
                )
            }
            NamedLayout(name, elements)
        } catch (_: Exception) {
            null
        }
    }

    class InMemoryStore : Store {
        private var raw: String? = null
        private var activeName: String? = null
        override fun getRawLayouts(): String? = raw
        override fun setRawLayouts(json: String) { raw = json }
        override fun getActiveLayoutName(): String? = activeName
        override fun setActiveLayoutName(name: String) { activeName = name }
    }
}

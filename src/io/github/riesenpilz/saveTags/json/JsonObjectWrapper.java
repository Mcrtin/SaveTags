package io.github.riesenpilz.saveTags.json;

import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class JsonObjectWrapper {
	private final JsonObject jsonObject;

	public JsonObjectWrapper() {
		jsonObject = new JsonObject();
	}

	public JsonObjectWrapper(JsonObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	public void add(String property, JsonElement value) {
		if (value != null)
			jsonObject.add(property, value);
	}

	public JsonElement remove(String property) {
		return jsonObject.remove(property);
	}

	public void add(String property, String value) {
		if (value != null)
			jsonObject.add(property, new JsonPrimitive(value));
	}

	public void add(String property, Number value) {
		if (value != null)
			jsonObject.add(property, new JsonPrimitive(value));
	}

	public void add(String property, JsonObjectWrapper value) {
		jsonObject.add(property, value.jsonObject());
	}

	public void add(String property, boolean value) {
		jsonObject.add(property, new JsonPrimitive(value));
	}

	public void add(String property, char value) {
		jsonObject.add(property, new JsonPrimitive(value));
	}

	public Set<Entry<String, JsonElement>> entrySet() {
		return jsonObject.entrySet();
	}

	public int size() {
		return jsonObject.size();
	}

	public boolean has(String memberName) {
		return jsonObject.has(memberName);
	}

	public boolean hasJsonArray(String memberName) {
		return getJsonArrayOrDef(memberName, null) != null;
	}

	public boolean hasJsonObject(String memberName) {
		return getJsonObjectOrDef(memberName, null) != null;
	}

	@Nullable
	public JsonElement get(String memberName) {
		return jsonObject.get(memberName);
	}

	public JsonElement getOrDef(String memberName, JsonElement def) {
		JsonElement jsonElement;
		return ((jsonElement = jsonObject.get(memberName)) != null) ? jsonElement : def;
	}

	public JsonArray getAsJsonArray(String memberName) {
		return (JsonArray) jsonObject.get(memberName);
	}

	public JsonObjectWrapper getAsJsonObject(String memberName) {
		return new JsonObjectWrapper((JsonObject) jsonObject.get(memberName));
	}

	public JsonArray getJsonArray(String memberName) {
		JsonElement jsonArray2;
		if ((jsonArray2 = get(memberName)) != null && jsonArray2.isJsonArray())
			return (JsonArray) jsonArray2;
		jsonArray2 = new JsonObject();
		add(memberName, jsonArray2);
		return (JsonArray) jsonArray2;
	}

	public JsonObjectWrapper getJsonObject(String memberName) {
		JsonElement jsonObject2;
		if ((jsonObject2 = get(memberName)) != null && jsonObject2.isJsonObject())
			return new JsonObjectWrapper((JsonObject) jsonObject2);
		jsonObject2 = new JsonObject();
		add(memberName, jsonObject2);
		return new JsonObjectWrapper((JsonObject) jsonObject2);
	}

	public JsonArray getJsonArrayOrDef(String memberName, JsonArray def) {
		JsonElement jsonArray;
		return (jsonArray = getOrDef(memberName, def)).isJsonArray() ? (JsonArray) jsonArray : def;
	}

	public JsonObjectWrapper getJsonObjectOrDef(String memberName, JsonObject def) {
		JsonElement jsonObject;
		return (jsonObject = getOrDef(memberName, def)).isJsonObject() ? new JsonObjectWrapper((JsonObject) jsonObject)
				: new JsonObjectWrapper(def);
	}

	public JsonObject jsonObject() {
		return jsonObject;
	}
}

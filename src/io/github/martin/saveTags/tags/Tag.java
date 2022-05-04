package io.github.riesenpilz.saveTags.tags;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;

public abstract class Tag extends JsonObjectWrapper {
	private boolean exists;

	protected Tag(JsonObjectWrapper jsonObject, boolean exists) {
		super(jsonObject);
		this.exists = exists;
	}

	public boolean hasTags() {
		return exists;
	}


	@Override
	public void add(String property, boolean value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, char value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, String value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, Number value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, JsonElement value) {
		add();
		super.add(property, value);
	}

	@Override
	public void add(String property, JsonObjectWrapper value) {
		add();
		super.add(property, value);
	}

	@Override
	public JsonObjectWrapper getJsonObject(String memberName) {
		add();
		return super.getJsonObject(memberName);
	}

	@Override
	public JsonArray getJsonArray(String memberName) {
		add();
		return super.getJsonArray(memberName);
	}

	private void add() {
		if (exists)
			return;
		exists = true;
		addThis();
	}

	public void remove() {
		if (!exists)
			return;
		exists = false;
		removeThis();
	}

	protected abstract void addThis();

	protected abstract void removeThis();
}

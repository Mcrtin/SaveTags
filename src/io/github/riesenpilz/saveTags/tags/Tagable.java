package io.github.riesenpilz.saveTags.tags;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;

public interface Tagable {
	public boolean hasTags();

	public JsonObjectWrapper getTags();

	public void setTags(@Nullable JsonObject jsonObject);

	public default void remove() {
		setTags(null);
	}
}

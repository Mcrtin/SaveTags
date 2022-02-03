package io.github.riesenpilz.saveTags.tags;

import javax.annotation.Nullable;

import io.github.riesenpilz.saveTags.json.JsonObjectWrapper;

public interface Tagable {
	public boolean hasTags();

	public JsonObjectWrapper getTags();

	public void setTags(@Nullable JsonObjectWrapper jsonObject);

	public default void removeTags() {
		setTags(null);
	}
}

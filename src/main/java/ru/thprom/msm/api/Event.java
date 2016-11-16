package ru.thprom.msm.api;

import java.util.Map;

/**
 * Created by void on 17.08.16
 */
public class Event {
	private Object id;
	private String type;
	private Map<String, Object> data;

	public Event() {
	}

	@SuppressWarnings("unchecked")
	public Event(Map<String, Object> event) {
		id = event.get("id");
		type = (String) event.get("event");
		data = (Map<String, Object>) event.get("data");
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "Event{" +
				"id=" + id +
				", type='" + type + '\'' +
				", data=" + data +
				'}';
	}
}

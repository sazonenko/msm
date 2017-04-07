package ru.thprom.msm.api;

import java.util.Map;

/**
 * Created by void on 17.08.16
 */
public class Event {
	private Object id;
	private Object stateId;
	private String type;
	private Map<String, Object> context;

	public Event() {
	}

	@SuppressWarnings("unchecked")
	public Event(Object stateId, Map<String, Object> event, Map<String, Object> eventContext) {
		id = event.get("id");
		this.stateId = stateId;
		type = (String) event.get("event");
		context = eventContext;
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
	}

	public Object getStateId() {
		return stateId;
	}

	public void setStateId(Object stateId) {
		this.stateId = stateId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getContext() {
		return context;
	}

	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	@Override
	public String toString() {
		return "Event{" +
				"id=" + id +
				", type='" + type + '\'' +
				", context=" + context +
				'}';
	}
}

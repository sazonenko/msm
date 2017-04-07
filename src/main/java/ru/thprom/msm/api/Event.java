package ru.thprom.msm.api;

import java.util.Date;
import java.util.Map;

/**
 * Created by void on 17.08.16
 */
public class Event {
	private Object id;
	private Object stateId;
	private String type;
	private Date created;
	private Map<String, Object> context;

	public Event() {
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

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
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
				", stateId=" + stateId +
				", type='" + type + '\'' +
				", created=" + created +
				", context=" + context +
				'}';
	}
}

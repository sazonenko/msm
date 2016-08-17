package ru.thprom.msm.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by void on 17.08.16
 */
public class State {

	private Object id;
	private String stateName;
	private String status;
	private Date mTime;
	private Map<String, Object> data;
	private List<Event> events;

	public State() {
	}

	@SuppressWarnings("unchecked")
	public State(Map<String, Object> stateData) {
		id = stateData.get("_id");
		stateName = (String) stateData.get("name");
		status = (String) stateData.get("status");
		mTime = (Date) stateData.get("mTime");
		data = (Map<String, Object>) stateData.get("data");
		List<Map<String, Object>> events = (List<Map<String, Object>>) stateData.get("events");
		this.events = new ArrayList<>(events.size());
		for (Map<String, Object> eventDoc : events) {
			Event event = new Event(eventDoc);
			this.events.add(event);
		}
	}

	public Object getId() {
		return id;
	}

	public void setId(Object id) {
		this.id = id;
	}

	public String getStateName() {
		return stateName;
	}

	public void setStateName(String stateName) {
		this.stateName = stateName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getmTime() {
		return mTime;
	}

	public void setmTime(Date mTime) {
		this.mTime = mTime;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}
}

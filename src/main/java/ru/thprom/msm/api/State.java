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
	private Map<String, Object> context;
	private List<Event> events;

	public State() {
	}

	@SuppressWarnings("unchecked")
	public State(Map<String, Object> stateData, Map<String, Object> context) {
		id = stateData.get("_id");
		stateName = (String) stateData.get("name");
		status = (String) stateData.get("status");
		mTime = (Date) stateData.get("mTime");
		this.context = context;
		List<Map<String, Object>> events = (List<Map<String, Object>>) stateData.get("events");
		int eventsCount = null == events? 0 : events.size();
		this.events = new ArrayList<>(eventsCount);
		if (null != events) {
			for (Map<String, Object> eventDoc : events) {
				Event event = new Event(id, eventDoc);
				this.events.add(event);
			}
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

	public Map<String, Object> getContext() {
		return context;
	}

	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	public List<Event> getEvents() {
		return events;
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

	@Override
	public String toString() {
		return "State{" +
				"id=" + id +
				", stateName='" + stateName + '\'' +
				", status='" + status + '\'' +
				", mTime=" + mTime +
				", context=" + context +
				", events=" + events +
				'}';
	}
}

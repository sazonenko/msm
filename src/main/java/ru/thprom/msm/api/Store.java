package ru.thprom.msm.api;

import java.util.Map;

/**
 * Created by void on 18.08.16
 */
public interface Store {
	String STATUS_PROCESS = "process";
	String STATUS_ERROR_NO_PROCESSOR = "err_no_processor";

	Object saveState(String stateName, Map<String, Object> data);

	void updateState(State state, Event event);

	void updateStateStatus(State state);

	boolean saveEvent(String eventType, Object stateId, Map<String, Object> data);

	State findStateWithEvent();

	void notifyListenerAdded(String state, String event);

	State findState(Object id);

	void delete(Object id);

	void clear();

}

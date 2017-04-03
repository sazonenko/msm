package ru.thprom.msm.api;

import java.util.Date;
import java.util.Map;

/**
 * Created by void on 18.08.16
 */
public interface Store {
	String STATUS_PROCESS = "process";
	String STATUS_ERROR_NO_PROCESSOR = "err_no_processor";
	String STATUS_ERROR = "processing_error";

	Object saveState(String stateName, Map<String, Object> data);

	/**
	 * update state after event processed
	 * @param state - new state data
	 * @param event - event that was processed. it will be deleted from state. can be null
	 */
	void updateState(State state, Event event);

	/**
	 * update only status field of the state
	 * @param status - new status
	 */
	void updateStateStatus(Object stateId, String status);

	boolean saveEvent(String eventType, Object stateId, Map<String, Object> data);

	void saveEvent(String eventType, Object stateId, Map<String, Object> data, Date fireTime);

	State findStateWithEvent();

	boolean processReadyEvent();

	void notifyListenerAdded(String state, String event);

	State findState(Object id);

	void delete(Object id);

	void clear();

}

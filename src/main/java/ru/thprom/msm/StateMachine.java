package ru.thprom.msm;

import java.util.Date;
import java.util.Map;

/**
 * Created by void on 26.07.2016
 */
public class StateMachine {

	private String id;
	private String state;
	private Date stateChanged;
	private Map<String, Object> data;

	public String process(String event) {
		return null;
	}

}

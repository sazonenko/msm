package ru.thprom.msm.api;

import java.util.Map;

/**
 * Created by void on 28.07.2016
 */
public interface EventProcessor {
	State process(State state, Event event);
}

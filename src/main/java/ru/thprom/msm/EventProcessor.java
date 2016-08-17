package ru.thprom.msm;

import java.util.Map;

/**
 * Created by void on 28.07.2016
 */
public interface EventProcessor {
	Map<String, Object> process(Map<String, Object> state, Map<String, Object> event);
}

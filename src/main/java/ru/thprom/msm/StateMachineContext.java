package ru.thprom.msm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thprom.msm.api.Event;
import ru.thprom.msm.api.EventProcessor;
import ru.thprom.msm.api.State;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by void on 28.07.2016
 */
public class StateMachineContext {
	private static Logger log = LoggerFactory.getLogger(StateMachineContext.class);

	private MongoStore store;
	private Map<String, EventProcessor> eventListeners;
	private ExecutorService processExecutor;
	private boolean destroy;
	private int timeout = 1;
	private int terminationTimeout = 60;

	public StateMachineContext() {
		eventListeners = new HashMap<>();

		processExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
			AtomicInteger count = new AtomicInteger();
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "StateMachineContext-" + count.incrementAndGet());
			}
		});
	}

	public void saveState(Map<String, Object> stateData) {
		String state = (String) stateData.get("state");
		if (null == state) {
			throw new IllegalArgumentException("No 'state' field in state data");
		}
		Object id = store.saveState(state, stateData);
		store.saveEvent(state, id, null);
	}

	public Object addState(String state, Map<String, Object> data) {
		Object id = store.saveState(state, data);
		store.saveEvent(state, id, null);
		return id;
	}

	public void saveEvent(Object stateID, String eventType) {
		store.saveEvent(eventType, stateID, null);
	}

	public void saveEvent(Object stateID, String eventType, Map<String, Object> data) {
		store.saveEvent(eventType, stateID, data);
	}

	public void addListener(String state, EventProcessor listener) {
		addListener(state, state, listener);
	}

	public void addListener(String state, String event, EventProcessor listener) {
		eventListeners.put(getListenerKey(state, event), listener);
	}

	private String getListenerKey(String state, String event) {
		return state + "!" + event;
	}

	public void start() {
		processExecutor.submit(new Runnable() {
			@Override
			public void run() {

				try {
					TimeUnit.SECONDS.sleep(timeout);  // wait for listeners

					while (!destroy) {
						State stateBefore = store.findStateWithEvent();
						if (null == stateBefore) {
							TimeUnit.SECONDS.sleep(timeout);
							continue;
						}
						String stateName = stateBefore.getStateName();
						Event event = stateBefore.getEvents().get(0);
						String eventType = event.getType();
						EventProcessor processor = eventListeners.get(getListenerKey(stateName, eventType));

						if (null == processor) {
							log.warn("No processor fount for state [{}], event [{}]", stateName, eventType);
							stateBefore.setStatus("err_no_processor");
							store.updateStateStatus(stateBefore);
							continue;
						}

						State stateAfter = processor.process(stateBefore, event);
						if (null == stateAfter) {
							store.delete(stateBefore.getId());
						} else {
							Map<String, Object> data = stateAfter.getData();
							store.updateState(stateAfter, event);
						}
					}
				} catch (InterruptedException e) {
					log.warn("Thread interrupted");
				}
			}
		});
	}

	@PreDestroy
	public void stop() {
		destroy = true;
		try {
			processExecutor.shutdown();
			boolean done = processExecutor.awaitTermination(terminationTimeout, TimeUnit.SECONDS);
			log.debug("terminated correctly: {}", done);
			if (!done) {
				log.error("process executor failed to terminate correctly");
				processExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			// ignored
		}

	}

	public void setStore(MongoStore store) {
		this.store = store;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setTerminationTimeout(int terminationTimeout) {
		this.terminationTimeout = terminationTimeout;
	}
}

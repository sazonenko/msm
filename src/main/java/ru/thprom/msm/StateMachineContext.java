package ru.thprom.msm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thprom.msm.api.Event;
import ru.thprom.msm.api.EventProcessor;
import ru.thprom.msm.api.State;
import ru.thprom.msm.api.Store;

import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by void on 28.07.2016
 */
public class StateMachineContext {
	private static Logger log = LoggerFactory.getLogger(StateMachineContext.class);

	private static final int DEFAULT_THREAD_COUNT = 5;

	private Store store;
	private Map<String, EventProcessor> eventListeners;
	private ExecutorService processExecutor;
	private ScheduledExecutorService scheduledExecutorService;
	private boolean destroy;
	private int timeout = 500;
	private int terminationTimeout = 60000;

	public StateMachineContext() {
		eventListeners = new HashMap<>();

		processExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
			AtomicInteger count = new AtomicInteger();
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "smc-worker-" + count.incrementAndGet());
			}
		});

		scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			AtomicInteger count = new AtomicInteger();
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, "smc-scheduled-" + count.incrementAndGet());
			}
		});
	}

	/**
	 * create new State
	 * @param state - name of the state
	 * @return Object ID identifying created state
	 */
	public Object addState(String state) {
		return addState(state, new HashMap<>());
	}

	/**
	 * create new State
	 * @param state - nema of the state
	 * @param data - additional data
	 * @return Object ID identifying created state
	 */
	public Object addState(String state, Map<String, Object> data) {
		Object id = store.saveState(state, data);
		//store.saveEvent(state, id, null);
		return id;
	}

	public State findState(Object id) {
		return store.findState(id);
	}

	public void saveEvent(Object stateID, String eventType) {
		store.saveEvent(eventType, stateID, null);
	}

	public void saveEvent(Object stateID, String eventType, Map<String, Object> data) {
		store.saveEvent(eventType, stateID, data);
	}

	public void saveEvent(Object stateID, String eventType, Map<String, Object> data, Date fireTime) {
		store.saveEvent(eventType, stateID, data, fireTime);
	}

	public void addListener(String state, EventProcessor listener) {
		addListener(state, state, listener);
	}

	public void addListener(String state, String event, EventProcessor listener) {
		eventListeners.put(getListenerKey(state, event), listener);
		store.notifyListenerAdded(state, event);
	}

	private String getListenerKey(String state, String event) {
		return state + "!" + event;
	}

	public void start() {
		start(DEFAULT_THREAD_COUNT);
	}

	public void start(int threadCount) {
		log.trace("starting context");

		for (int i=0; i<threadCount; i++) {
			processExecutor.submit(new Stoppable(new EventWorker()));
		}

		scheduledExecutorService.scheduleAtFixedRate(new DelayedEventWorker(), 100, 100, TimeUnit.MILLISECONDS);
	}

	@PreDestroy
	public void stop() {
		destroy = true;
		try {
			scheduledExecutorService.shutdown();
			processExecutor.shutdown();
			boolean done = processExecutor.awaitTermination(terminationTimeout, TimeUnit.MILLISECONDS);
			log.debug("terminated: {}", done);
			if (!done) {
				log.error("process executor failed to terminate");
				processExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			// ignored
		}
	}

	/**
	 * set up Store implementation
	 * @param store - Store implementation
	 */
	public void setStore(Store store) {
		this.store = store;
	}

	/**
	 * set timeout for wait after empty Store response
	 * @param timeout - timeout in milliseconds
	 */
	public void setPollTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * set timeout for context termination
	 * @param terminationTimeout - timeout in milliseconds
	 */
	public void setTerminationTimeout(int terminationTimeout) {
		this.terminationTimeout = terminationTimeout;
	}

	private class DelayedEventWorker implements Runnable {

		@Override
		public void run() {
			boolean result = true;
			for (int i=0; i<100 && result; i++){
				result = store.processReadyEvent();
			}
		}
	}

	private class EventWorker implements Worker {

		@Override
		public void run() throws InterruptedException {
			State stateBefore = store.findStateWithEvent();
			if (null == stateBefore) {
				log.trace("no events to process. sleep {}ms.", timeout);
				TimeUnit.MILLISECONDS.sleep(timeout);
				return;
			}
			log.trace("before processing state: {}", stateBefore);

			String stateName = stateBefore.getStateName();
			Event event = stateBefore.getEvents().get(0);
			String eventType = event.getType();
			EventProcessor processor = eventListeners.get(getListenerKey(stateName, eventType));

			if (null == processor) {
				log.warn("No processor found for state [{}], event [{}]", stateName, eventType);
				stateBefore.setStatus(Store.STATUS_ERROR_NO_PROCESSOR);
				store.updateStateStatus(stateBefore.getId(), Store.STATUS_ERROR_NO_PROCESSOR);
				return;
			}

			State stateAfter;
			try {
				stateAfter = processor.process(stateBefore, event);
			} catch (Exception e) {
				log.error("error processing state: ["+ stateBefore +"], event: ["+ event +"]", e);
				stateAfter = stateBefore;
				stateAfter.setStatus(Store.STATUS_ERROR);
				Map<String, Object> data = stateAfter.getContext();
				data.put("exception", e.toString());
				event = null; // prevent mark this event processed
			}
			if (null == stateAfter) {
				store.delete(stateBefore.getId());
			} else {
				store.updateState(stateAfter, event);
			}
		}
	}

	private interface Worker {
		void run() throws InterruptedException;
	}
	private class Stoppable implements Runnable {

		private Worker worker;

		public Stoppable(Worker worker) {
			this.worker = worker;
		}

		@Override
		public void run() {
			while (!destroy) {
				try {
					worker.run();
					//processEvent();
				} catch (InterruptedException e) {
					log.warn("Thread interrupted");
					break;
				} catch (Exception e) {
					log.error("Error in processing thread", e);
				}
			}
		}
	}
}

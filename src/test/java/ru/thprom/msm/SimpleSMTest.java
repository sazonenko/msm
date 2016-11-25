package ru.thprom.msm;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.thprom.msm.api.State;
import ru.thprom.msm.api.Store;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by void on 08.08.16
 */
public class SimpleSMTest {
	private static Logger log = LoggerFactory.getLogger(SimpleSMTest.class);

	private StateMachineContext smc;

	@Before
	public void init() {
		ApplicationContext appContext = new AnnotationConfigApplicationContext(SpringContext.class);
		smc = appContext.getBean("smContext", StateMachineContext.class);
		Store store = appContext.getBean("mongoStore", MongoStore.class);
		store.clear();
	}

	@Test
	public void testOneStep() throws InterruptedException {
		smc.addListener("one", (state, event) -> {
			log.info("one : init state [{}] processed", state);
			return null;
		});
		Object key = smc.addState("one", new HashMap<>());
		smc.start();

		TimeUnit.SECONDS.sleep(1);
		State state = smc.findState(key);
		log.debug("found state: {}", state);
		assertNull(state);
	}

	@Test
	public void testTwoStep() throws InterruptedException {

		smc.addListener("two", (state, event) -> {
			log.info("two : init state [{}] processed", state);
			state.setStateName("three");
			return state;
		});

		smc.addListener("three", "fire", (state, event) -> {
			log.info("fire event of the state [{}] processed", state);
			return state;
		});

		Object stateTwoId = smc.addState("two", new HashMap<>());
		smc.start();
		TimeUnit.SECONDS.sleep(2);
		smc.saveEvent(stateTwoId, "fire");

		log.info("wait for events");
		TimeUnit.SECONDS.sleep(2);
		State stateTwo = smc.findState(stateTwoId);
		log.debug("found state: {}", stateTwo);
		assertNotNull(stateTwo);
		log.info("done");
	}

	@Test
	public void testConcurrent() throws InterruptedException {
		smc.addListener("start", (state, event) -> {
			log.info("start : state [{}] processed", state);

			int workCount = 0;
			for (int i=0; i<10; i+=2) {
				Map<String, Object> data = new HashMap<>();
				data.put("parent", event.getStateId());
				data.put("left", i);
				data.put("right", i+1);
				smc.addState("work", data);
				workCount++;
			}

			Map<String, Object> data = new HashMap<>();
			data.put("result", 0);
			data.put("count", workCount);
			state.setData(data);
			state.setStateName("wait");
			log.info("start done");
			return state;
		});

		smc.addListener("work", (state, event) -> {
			log.info("work : state [{}] processed", state);
			Map<String, Object> stateData = state.getData();
			Map<String, Object> data = new HashMap<>();
			int left = (Integer) stateData.get("left");
			int right = (Integer) stateData.get("right");
			data.put("result", left + right);
			log.debug("work: before save event");
			smc.saveEvent(stateData.get("parent"), "work_done", data);
			return null;
		});

		smc.addListener("wait", "work_done", (state, event) -> {
			log.info("work_done : state [{}] processed", state);
			Map<String, Object> stateData = state.getData();
			Map<String, Object> eventData = event.getData();

			int count = (Integer) stateData.get("count");
			int result = (Integer) stateData.get("result");
			int value = (Integer) eventData.get("result");
			stateData.put("count", count -= 1);
			stateData.put("result", result += value);
			log.debug("work_done: before end");
			if (count > 0) {
				return state;
			} else {
				log.info("All done! result: {}", result);
				return null;
			}
		});

		Object stateId = smc.addState("start", new HashMap<>());
		smc.start();
		TimeUnit.SECONDS.sleep(10);
		log.info("test done");
	}
}

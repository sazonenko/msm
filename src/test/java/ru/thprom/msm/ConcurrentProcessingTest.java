package ru.thprom.msm;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.thprom.msm.api.Store;
import ru.thprom.msm.mongo.MongoStore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

/**
 * Created by void on 28.03.17
 */
public class ConcurrentProcessingTest {
	private static Logger log = LoggerFactory.getLogger(SimpleSMTest.class);

	private StateMachineContext smc;

	@Before
	public void init() {
		ApplicationContext appContext = new AnnotationConfigApplicationContext(SpringContext.class);
		smc = appContext.getBean("smContext", StateMachineContext.class);
		smc.setPollTimeout(100);
		Store store = appContext.getBean("mongoStore", MongoStore.class);
		store.clear();
	}

	@After
	public void clean() {
		smc.stop();
	}

	@Ignore
	@Test
	public void testConcurrent() throws InterruptedException {

		final AtomicInteger total = new AtomicInteger(0);
		final CountDownLatch latch = new CountDownLatch(1);

		smc.addListener("start", (state, event) -> {
			log.info("start : state [{}] processed", state);

			int workCount = 0;
			for (int i=0; i<1000; i+=2) {
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
			state.setContext(data);
			state.setStateName("wait");
			log.info("start done");
			return state;
		});

		smc.addListener("work", (state, event) -> {
			log.info("work : state [{}] processed", state);
			Map<String, Object> stateData = state.getContext();
			Map<String, Object> eventData = new HashMap<>();
			int left = (Integer) stateData.get("left");
			int right = (Integer) stateData.get("right");
			eventData.put("result", left + right);
			log.debug("work: before save event");
			smc.saveEvent(stateData.get("parent"), "work_done", eventData);
			return null;
		});

		smc.addListener("wait", "work_done", (state, event) -> {
			log.info("work_done : state [{}] processed", state);
			Map<String, Object> stateData = state.getContext();
			Map<String, Object> eventData = event.getContext();

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
				total.set(result);
				latch.countDown();
				return null;
			}
		});


		smc.addState("start", new HashMap<>());
		smc.start(3);
		latch.await();
		log.info("test done. result: {}", total.get());
		assertTrue(499500 == total.get());
	}

}

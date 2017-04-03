package ru.thprom.msm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.thprom.msm.api.State;
import ru.thprom.msm.api.Store;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
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

	@After
	public void clean() {
		smc.stop();
	}

	@Test
	public void testOneStep() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);

		smc.addListener("one", (state, event) -> {
			log.info("one : init state [{}] processed", state);
			latch.countDown();
			return null;
		});
		Object key = smc.addState("one", new HashMap<>());
		smc.start(1);

		latch.await();
		State state = smc.findState(key);
		log.debug("found state: {}", state);
		assertNull("found state: " + state, state);
	}

	@Test
	public void testTwoStep() throws InterruptedException {

		final CountDownLatch latch = new CountDownLatch(1);

		smc.addListener("two", (state, event) -> {
			log.info("two : init state [{}] processed", state);
			state.setStateName("three");
			return state;
		});

		smc.addListener("three", "fire", (state, event) -> {
			log.info("fire event of the state [{}] processed", state);
			latch.countDown();
			return state;
		});

		Object stateTwoId = smc.addState("two");
		smc.start(1);
//		TimeUnit.SECONDS.sleep(2);
		smc.saveEvent(stateTwoId, "fire");

		log.info("wait for events");
		latch.await();
		State stateTwo = smc.findState(stateTwoId);
		log.debug("found state: {}", stateTwo);
		assertNotNull(stateTwo);
		log.info("done");
	}

	@Test
	public void testDelayedEvent() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicInteger data = new AtomicInteger(0);

		smc.addListener("d1", (state, event) ->{
			log.info("state d1");
			return state;
		});

		smc.addListener("d1","showTime", (state, event) -> {
			log.info("event 'showTime'");
			data.incrementAndGet();
			latch.countDown();
			return null;
		});

		smc.start(1);
		Object d1 = smc.addState("d1");
		Date startTime = new Date();
		Date fireTime = new Date(startTime.getTime() + 100);
		smc.saveEvent(d1, "showTime", new HashMap<>(), fireTime);
		TimeUnit.MILLISECONDS.sleep(50);
		assertEquals("event fired before timeout", data.get(), 0);

		latch.await(1, TimeUnit.SECONDS);
		assertEquals("wrong data after event", 1, data.get());
		Date now = new Date();
		assertEquals("the timeout has not expired",true, now.getTime()-fireTime.getTime() > 0);
	}

	@Test
	public void testException() throws InterruptedException {
		AtomicInteger i = new AtomicInteger();

		smc.addListener("e1", (state, event) ->{
			log.info("state e1");
			if (i.incrementAndGet() == 1) {
				throw new RuntimeException("i == 1");
			}
			return null;
		});

		smc.start(1);
		Object e1 = smc.addState("e1");
		TimeUnit.SECONDS.sleep(2);

	}
}

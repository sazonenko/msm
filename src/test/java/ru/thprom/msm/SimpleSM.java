package ru.thprom.msm;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by void on 08.08.16
 */
public class SimpleSM {
	private static Logger log = LoggerFactory.getLogger(SimpleSM.class);

	private StateMachineContext smc;
	private MongoStore store;

	@Before
	public void init() {
		ApplicationContext appContext = new AnnotationConfigApplicationContext(SpringContext.class);
		smc = appContext.getBean("smContext", StateMachineContext.class);
		store = appContext.getBean("mongoStore", MongoStore.class);

	}

	@Test
	public void testSm() throws InterruptedException {
		smc.addListener("one", (state, event) -> {
			log.info("one : init state [{}] processed", state);
			return null;
		});

		smc.addListener("two", (state, event) -> {
			log.info("two : init state [{}] processed", state);
			state.setStateName("three");
			return state;
		});

		smc.addListener("three", "fire", (state, event) -> {
			log.info("fire event of the state [{}] processed", state);
			return state;
		});

		smc.addState("one", new HashMap<>());
		Object stateTwo = smc.addState("two", new HashMap<>());
		smc.saveEvent(stateTwo, "fire");

		TimeUnit.SECONDS.sleep(3);
		log.info("done");
	}
}

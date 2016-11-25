package ru.thprom.msm;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.thprom.msm.api.State;
import ru.thprom.msm.api.Store;

/**
 * Created by void on 24.11.16
 */
public class MongoStoreTest {
	private static Logger log = LoggerFactory.getLogger(MongoStoreTest.class);

	private MongoStore mongoStore;
	@Before
	public void init() {
		ApplicationContext appContext = new AnnotationConfigApplicationContext(SpringContext.class);
		mongoStore = appContext.getBean("mongoStore", MongoStore.class);
		mongoStore.clear();
	}

	@Test
	public void findStateTest() {
		ObjectId testStateId = mongoStore.saveState("test", null);
		State stateWithEvent = mongoStore.findStateWithEvent();
		log.info("found state: {}", stateWithEvent);
		State state = mongoStore.findState(testStateId);
		log.info("for id {} found state: {}", testStateId, state);
	}
}

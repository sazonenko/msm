package ru.thprom.msm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.thprom.msm.api.Store;
import ru.thprom.msm.mongo.MongoStore;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * Created by void on 06.04.17
 */
public class PojoTest {

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

	@Test
	public void testPojoInState() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final User[] users = {new User("Василий"), null};

		smc.addListener("pojo1", (state, event) ->{
			log.info("state pojo1");
			users[1] = (User) state.getContext().get("user");
			latch.countDown();
			return null;
		});

		smc.start(1);
		HashMap<String, Object> data = new HashMap<>();
		data.put("user", users[0]);
		smc.addState("pojo1", data);
		latch.await();
		assertEquals("Users is not equal", users[0], users[1]);
	}

	@Test
	public void testPojoInEvent() throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final User[] users = {new User("Пётр"), null};

		smc.addListener("pojo2", (state, event) ->{
			log.info("event 'pojo2'");
			return state;
		});

		smc.addListener("pojo2", "update", (state, event) ->{
			log.info("event 'update'");
			users[1] = (User) event.getContext().get("user");
			latch.countDown();
			return null;
		});

		smc.start(1);
		Object stateId = smc.addState("pojo2");

		HashMap<String, Object> data = new HashMap<>();
		data.put("user", users[0]);
		smc.saveEvent(stateId, "update", data);

		latch.await();
		assertEquals("Users is not equal", users[0], users[1]);
	}

	public static class User {
		public String name;

		@SuppressWarnings("unused") // used by serialization
		public User() {
		}

		public User(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			User user = (User) o;

			return name != null ? name.equals(user.name) : user.name == null;
		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "User{" +
					"name='" + name + '\'' +
					'}';
		}
	}

}

package ru.thprom.msm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.core.env.Environment;
import ru.thprom.msm.api.Store;

/**
 * Created by void on 26.07.2016
 */
@PropertySources({
		@PropertySource("classpath:default.properties"),
		@PropertySource(value = "classpath:msm.properties", ignoreResourceNotFound = true)
})
@Configuration
public class SpringContext {

	@Autowired
	private Environment env;

	@Bean
	public Store mongoStore() {
		MongoStore mongoStore = new MongoStore();
		mongoStore.setHost(env.getProperty("mongo.host"));
		mongoStore.setPort(env.getProperty("mongo.port", Integer.class));
		mongoStore.setDatabaseName(env.getProperty("mongo.database"));
		mongoStore.connect();
		return mongoStore;
	}

	@Bean
	public StateMachineContext smContext() {
		StateMachineContext context = new StateMachineContext();
		context.setStore(mongoStore());
		context.start();
		return context;
	}
}

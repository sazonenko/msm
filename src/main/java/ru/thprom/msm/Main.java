package ru.thprom.msm;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.HashMap;

/**
 * Created by void on 26.07.2016
 */
public class Main {
	public static void main(String[] args) {

		ApplicationContext appContext = new AnnotationConfigApplicationContext(SpringContext.class);
		StateMachineContext smContext = appContext.getBean("smContext", StateMachineContext.class);

		smContext.addState("init", new HashMap<>());
	}
}

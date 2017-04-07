package ru.thprom.msm.mongo;

import org.bson.Document;

import java.util.Map;

/**
 * Created by void on 05.04.17
 */
public interface Serializer {

	String toDocument(Map<String, Object> data);

	Map<String, Object> toObject(String doc);

}

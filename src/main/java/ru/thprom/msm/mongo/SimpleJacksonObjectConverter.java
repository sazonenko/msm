package ru.thprom.msm.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.bson.Document;

import java.io.IOException;
import java.util.Map;

/**
 * Created by void on 05.04.17
 */
public class SimpleJacksonObjectConverter implements ObjectConverter {

	private ObjectMapper mapper;
	private ObjectWriter writer;
	private ObjectReader reader;

	public SimpleJacksonObjectConverter() {
		mapper = new ObjectMapper();
		mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		writer = mapper.writer();
		reader = mapper.reader();
	}

	@Override
	public String toDocument(Map<String, Object> data) {
		try {
			String json = writer.writeValueAsString(data);
			return json;
			//return Document.parse(json);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("can't convert Map object to JSON", e);
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> toObject(String doc) {
		try {
			return doc == null ? null : mapper.readValue(doc, Map.class);
//			return reader.readValue(doc.toJson());
		} catch (IOException e) {
			throw new RuntimeException("can't convert Map object from JSON", e);
		}
	}
}

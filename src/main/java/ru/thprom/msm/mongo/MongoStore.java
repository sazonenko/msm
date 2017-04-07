package ru.thprom.msm.mongo;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.thprom.msm.api.Event;
import ru.thprom.msm.api.State;
import ru.thprom.msm.api.Store;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by void on 11.12.15
 */
public class MongoStore implements Store {
	private static Logger log = LoggerFactory.getLogger(MongoStore.class);
	public static final String COLLECTION_PREFIX = "msm_";
	public static final String STATES_COLLECTION = "states";
	public static final String DELAYED_EVENTS_COLLECTION = "delayed_events";

	public static final String FIELD_STATE_NAME = "name";
	public static final String FIELD_STATUS = "status";
	public static final String FIELD_MOD_TIME = "mTime";
	public static final String FIELD_DATA = "data";
	public static final String FIELD_EVENTS = "events";
	public static final String FIELD_EVENT_COUNT = "eventCount";

	public static final String EF_ID = "id";
	public static final String EF_TYPE = "event";
	public static final String EF_CREATED = "created";
	public static final String DEF_STATE_ID = "stateId";
	public static final String DEF_FIRE_TIME = "fireTime";

	private MongoClient softClient;
	private MongoClient hardClient;
	private MongoDatabase dbs;
	private MongoDatabase dbh;
	private String host;
	private int port;
	private String databaseName;
	private String collectionPrefix = COLLECTION_PREFIX;
	private String statesCollectionName = STATES_COLLECTION;
	private String delayedEventsCollection = DELAYED_EVENTS_COLLECTION;
	private ObjectConverter converter;

	public void connect() {
		ServerAddress serverAddress = new ServerAddress(host, port);
		softClient = new MongoClient(serverAddress);
		dbs = softClient.getDatabase(databaseName);

		MongoClientOptions hardOptions = new MongoClientOptions.Builder()
				.writeConcern(WriteConcern.JOURNALED)
				.writeConcern(WriteConcern.W1)
				.codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry())
				.build();
		hardClient = new MongoClient(serverAddress, hardOptions);
		dbh = hardClient.getDatabase(databaseName);
	}

	@PostConstruct
	public void init() {
		statesCollectionName = collectionPrefix + STATES_COLLECTION;
		delayedEventsCollection = collectionPrefix + DELAYED_EVENTS_COLLECTION;
		if (null == converter) {
			converter = new SimpleJacksonObjectConverter();
		}
	}

	@Override
	public ObjectId saveState(String stateName, Map<String, Object> data) {
		log.trace("before save state '{}'", stateName);
		Document stateDoc = new Document(FIELD_STATE_NAME, stateName)
				.append(FIELD_MOD_TIME, new Date())
				.append(FIELD_STATUS, "");
		if (null != data) {
			stateDoc.append(FIELD_DATA, converter.toDocument(data));
		}
		Document eventDoc = new Document(EF_TYPE, stateName)
				.append(EF_ID, new ObjectId())
				.append(EF_CREATED, new Date());
		List<Document> events = new ArrayList<>(1);
		events.add(eventDoc);
		stateDoc.append(FIELD_EVENT_COUNT, 1)
				.append(FIELD_EVENTS, events);

		MongoCollection<Document> states = dbh.getCollection(statesCollectionName);
		states.insertOne(stateDoc);
		ObjectId stateId = stateDoc.get("_id", ObjectId.class);
		log.debug("saved state [{} : '{}']", stateId, stateName);
		return stateId;
	}

	@Override
	public void updateState(State state, Event event) {
		Document filter = new Document("_id", state.getId());
		Document updateDoc = new Document(FIELD_STATE_NAME, state.getStateName())
				.append(FIELD_MOD_TIME, new Date())
				.append(FIELD_DATA, converter.toDocument(state.getContext()));

		if (STATUS_PROCESS.equals(state.getStatus())) {
			updateDoc.append(FIELD_STATUS, "");
		} else {
			updateDoc.append(FIELD_STATUS, state.getStatus());
		}

		Document document = new Document("$set", updateDoc);
		if (null != event) {
			document.append("$pull", new Document(FIELD_EVENTS, new Document(EF_ID, event.getId())))
					.append("$inc", new Document(FIELD_EVENT_COUNT, -1));
		}
		log.debug("update doc: {}", document);
		MongoCollection<Document> collection = dbh.getCollection(statesCollectionName);
		collection.updateOne(filter, document);
	}

	@Override
	public void updateStateStatus(Object stateId, String status) {
		Document filter = new Document("_id", stateId);
		Document updateDoc = new Document(FIELD_STATUS, status);
		Document document = new Document("$set", updateDoc);

		MongoCollection<Document> collection = dbh.getCollection(statesCollectionName);
		collection.updateOne(filter, document);
	}

	@Override
	public boolean saveEvent(String eventType, Object stateId, Map<String, Object> data) {
		log.debug("save event '{}' for [{}]", eventType, stateId);
		Document eventDoc = new Document(EF_TYPE, eventType)
				.append(EF_ID, new ObjectId())
				.append(EF_CREATED, new Date());
		if (null != data) {
			eventDoc.append(FIELD_DATA, data);
			//eventDoc.append(FIELD_DATA, converter.toDocument(data));
		}

		return saveEvent(stateId, eventDoc);
	}

	private boolean saveEvent(Object stateId, Document eventDoc) {
		MongoCollection<Document> collection = dbh.getCollection(statesCollectionName);
		Document filter = new Document("_id", stateId);
		Document updateDoc = new Document("$inc", new Document(FIELD_EVENT_COUNT, 1))
				.append("$push", new Document(FIELD_EVENTS, eventDoc));
		UpdateResult updateResult = collection.updateOne(filter, updateDoc);
		if (updateResult.getModifiedCount() < 1) {
			log.error("state({}) not found", stateId);
			return false;
		}
		return true;
	}

	@Override
	public void saveEvent(String eventType, Object stateId, Map<String, Object> data, Date fireTime) {
		log.debug("save delayed event '{}' for [{}], fire at [{}]", eventType, stateId, fireTime);
		Document eventDoc = new Document(EF_TYPE, eventType)
				.append(EF_ID, new ObjectId());
		if (null != data) {
			eventDoc.append(FIELD_DATA, data);
		//	eventDoc.append(FIELD_DATA, converter.toDocument(data));
		}

		eventDoc.append(DEF_STATE_ID, stateId);
		eventDoc.append(EF_CREATED, new Date());
		eventDoc.append(DEF_FIRE_TIME, fireTime);
		eventDoc.append(FIELD_STATUS, "");

		MongoCollection<Document> collection = dbh.getCollection(delayedEventsCollection);
		collection.insertOne(eventDoc);
	}

	@Override
	public State findStateWithEvent() {
		MongoCollection<Document> collection = dbs.getCollection(statesCollectionName);
		Document filter = new Document(FIELD_STATUS, "")
				.append(FIELD_EVENT_COUNT, new Document("$gt", 0));
		Document update = new Document("$set", new Document(FIELD_STATUS, "process").append(FIELD_MOD_TIME, new Date()));

		log.debug("before find state with event");
		Document stateDoc = collection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().sort(new Document("_id", 1)));
		log.debug("found state [{}]", stateDoc);
		return buildState(stateDoc);
	}

	@Override
	public boolean processReadyEvent() {
		MongoCollection<Document> collection = dbs.getCollection(delayedEventsCollection);
		Document filter =  new Document(FIELD_STATUS, "")
				.append(DEF_FIRE_TIME, new Document("$lte", new Date()));
		Document update = new Document("$set", new Document(FIELD_STATUS, "process").append(FIELD_MOD_TIME, new Date()));

		Document eventDoc = collection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().sort(new Document("_id", 1)));

		if (null != eventDoc) {
			Object stateId = eventDoc.remove(DEF_STATE_ID);
			eventDoc.remove(DEF_FIRE_TIME);
			eventDoc.append(FIELD_STATUS, "");
			if (saveEvent(stateId, eventDoc)) {
				DeleteResult deleteResult = collection.deleteOne(new Document("_id", eventDoc.get("_id")));
				return deleteResult.getDeletedCount() > 0;
			}
		}
		return false;
	}

	@Override
	public void notifyListenerAdded(String state, String event) {
		Document filter = new Document(FIELD_STATE_NAME, state)
				.append("events.event", event)  // FIELD_EVENTS
				.append(FIELD_STATUS, STATUS_ERROR_NO_PROCESSOR);
		Document update = new Document("$set", new Document(FIELD_STATUS, "").append(FIELD_MOD_TIME, new Date()));

		MongoCollection<Document> collection = dbs.getCollection(statesCollectionName);
		UpdateResult updateResult = collection.updateMany(filter, update);
		log.debug("update for listener [{}:{}]: {}", state, event, updateResult);
	}

	@Override
	public State findState(Object id) {
		MongoCollection<Document> collection = dbh.getCollection(statesCollectionName);
		FindIterable<Document> docs = collection.find(new Document("_id", id)).limit(1);
		return buildState(docs.first());
	}

	@Override
	public void delete(Object id) {
		MongoCollection<Document> collection = dbh.getCollection(statesCollectionName);

		DeleteResult deleteResult = collection.deleteOne(new Document("_id", id));
		log.debug("delete {}, result: {}", id, deleteResult);
	}

	@Override
	public void clear() {
		MongoCollection<Document> states = dbh.getCollection(statesCollectionName);

		DeleteResult statesResult = states.deleteMany(new Document());
		log.debug("delete all states, result: {}", statesResult);
		DeleteResult eventsResult = dbh.getCollection(delayedEventsCollection).deleteMany(new Document());
		log.debug("delete all events, result: {}", statesResult);
	}

	@PreDestroy
	public void close() {
		softClient.close();
		hardClient.close();
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public void setCollectionsPrefix(String prefix) {
		collectionPrefix = prefix;
	}

	public void setConverter(ObjectConverter converter) {
		this.converter = converter;
	}

	private State buildState(Document stateDoc) {
		State result = null;
		if (null != stateDoc) {
			Map<String, Object> context = converter.toObject(stateDoc.get(FIELD_DATA, String.class));
			result = new State(stateDoc, context);
		}
		return result;
	}
}

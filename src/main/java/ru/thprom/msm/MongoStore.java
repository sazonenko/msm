package ru.thprom.msm;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
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

import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.Map;

/**
 * Created by void on 11.12.15
 */
public class MongoStore {
	public static final String FIELD_MOD_TIME = "mTime";
	public static final String STATUS_PROCESS = "process";
	private static Logger log = LoggerFactory.getLogger(MongoStore.class);

	public static final String STATES_COLLECTION = "states";

	public static final String FIELD_EVENTS = "events";
	public static final String FIELD_DATA = "data";
	public static final String FIELD_STATE_NAME = "name";
	public static final String FIELD_STATUS = "status";

	private MongoClient softClient;
	private MongoClient hardClient;
	private MongoDatabase dbs;
	private MongoDatabase dbh;
	private String host;
	private int port;
	private String databaseName;
	private String collectionName = STATES_COLLECTION;

	public void connect() {
		ServerAddress serverAddress = new ServerAddress(host, port);
		softClient = new MongoClient(serverAddress);
		dbs = softClient.getDatabase(databaseName);

		MongoClientOptions hardOptions = new MongoClientOptions.Builder()
				.writeConcern(WriteConcern.JOURNALED)
				.writeConcern(WriteConcern.W1)
				.build();
		hardClient = new MongoClient(serverAddress, hardOptions);
		dbh = hardClient.getDatabase(databaseName);
	}


	public ObjectId saveState(String stateName, Map<String, Object> data) {
		Document stateDoc = new Document(FIELD_STATE_NAME, stateName)
				.append(FIELD_MOD_TIME, new Date())
				.append(FIELD_STATUS, "")
				.append(FIELD_DATA, data);
		MongoCollection<Document> states = dbh.getCollection(collectionName);
		states.insertOne(stateDoc);
		return stateDoc.get("_id", ObjectId.class);
	}

	public void updateState(State state, Event event) {
		Document filter = new Document("_id", state.getId());
		Document updateDoc = new Document(FIELD_STATE_NAME, state.getStateName())
				.append(FIELD_MOD_TIME, new Date())
				.append(FIELD_DATA, state.getData());

		if (STATUS_PROCESS.equals(state.getStatus())) {
			updateDoc.append(FIELD_STATUS, "");
		} else {
			updateDoc.append(FIELD_STATUS, state.getStatus());
		}

		Document document = new Document("$set", updateDoc)
				.append("$pull", new Document(FIELD_EVENTS, new Document("id", event.getId())))
				.append("$inc", new Document("eventCount", -1));

		log.debug("update doc: {}", document);
		MongoCollection<Document> collection = dbh.getCollection(collectionName);
		collection.updateOne(filter, document);
	}

	public void updateStateStatus(State state) {
		Document filter = new Document("_id", state.getId());
		Document updateDoc = new Document(FIELD_STATUS, state.getStatus());
		Document document = new Document("$set", updateDoc);

		MongoCollection<Document> collection = dbh.getCollection(collectionName);
		collection.updateOne(filter, document);
	}
	public boolean saveEvent(String eventType, Object stateId, Map<String, Object> data) {
		Document filter = new Document("_id", stateId);
		Document eventDoc = new Document("event", eventType)
				.append("id", new ObjectId());
		if (null != data) {
			eventDoc.append(FIELD_DATA, data);
		}
		eventDoc.append("created", new Date());
		Document updateDoc = new Document("$inc", new Document("eventCount", 1))
				.append("$push", new Document(FIELD_EVENTS, eventDoc));

		MongoCollection<Document> collection = dbh.getCollection(collectionName);
		UpdateResult updateResult = collection.updateOne(filter, updateDoc);
		if (updateResult.getModifiedCount() < 1) {
			log.error("state({}) not found", stateId);
			return false;
		}
		return true;
	}

	public State findStateWithEvent() {
		MongoCollection<Document> collection = dbs.getCollection(collectionName);
		Document filter = new Document(FIELD_STATUS, "")
				.append("eventCount", new Document("$gt", 0));
		Document update = new Document("$set", new Document(FIELD_STATUS, "process").append(FIELD_MOD_TIME, new Date()));

		Document stateDoc = collection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().sort(new Document("_id", 1)));
		return new State(stateDoc);
	}

	public void delete(Object id) {
		MongoCollection<Document> collection = dbh.getCollection(collectionName);

		DeleteResult deleteResult = collection.deleteOne(new Document("_id", id));
		log.debug("delete {}, result: {}", id, deleteResult);
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

	public void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
}

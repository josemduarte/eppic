package eppic.db;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoDatabase;
import eppic.db.tools.ConfigurableMapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoUtils {

    private static final Logger logger = LoggerFactory.getLogger(MongoUtils.class);

    /**
     * Writes a single {@link Object} to a {@param collectionName} collection in the given {@param mongoDatabase}.
     *
     *  @param mongoDatabase destination {@link MongoDatabase}
     *  @param collectionName destination {@link com.mongodb.client.MongoCollection} name
     *  @param object {@link Object}
     */
    public static <T> void writeObject(MongoDatabase mongoDatabase, String collectionName, T object) {

        Document document = convertValueToDocument(object);

        writeDocument(mongoDatabase, collectionName, document);
    }

    /**
     * Writes a single {@link Document} to a {@param collectionName} collection in the given {@param mongoDatabase}.
     *
     *  @param mongoDatabase destination {@link MongoDatabase}
     *  @param collectionName destination {@link com.mongodb.client.MongoCollection} name
     *  @param document {@link Document}
     */
    private static void writeDocument(MongoDatabase mongoDatabase, String collectionName, Document document) {

        try {
            mongoDatabase.getCollection(collectionName).insertOne(document);
        } catch (MongoWriteException e) {
            logger.error("{}: {}. Error category: {}", e.getError().getMessage(), document.toJson(), e.getError().getCategory().name());
            throw e;
        }
    }

    /**
     * Uses an object mapper {@link ConfigurableMapper#getMapper()} to
     * convert an {@link Object} to a {@link Document}.
     *
     * @param o input {@link Object}.
     * @return an instance of {@link Document}.
     */
    public static Document convertValueToDocument(Object o) {

        if ( !(o instanceof Document) )
            return ConfigurableMapper.getMapper().convertValue(o, Document.class);
        else
            return (Document) o;
    }

    /**
     * Reads configuration properties and create a connection to Mongo database using specified connection URI.
     *
     * @param dbName the Mongo database name
     * @param connUriStr the Mongo connection URI
     * @return {@link com.mongodb.client.MongoDatabase} database
     */
    public static MongoDatabase getMongoDatabase(String dbName, String connUriStr) {

        MongoClient mongoClient;
        MongoClientURI uri = new MongoClientURI(connUriStr);
        try {
            mongoClient = new MongoClient(uri);
        } catch (Exception e) {
            logger.error("Cannot connect to mongodb using connection parameters: {}", connUriStr);
            throw e;
        }

        if (dbName == null)
            throw new IllegalArgumentException("dbName must not be null");

        return mongoClient.getDatabase(dbName);
    }

}
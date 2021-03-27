package eppic.db.mongoutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DbPropertiesReader {

    private final Properties properties;

    public DbPropertiesReader(File configurationFile) throws IOException {
        properties = new Properties();
        properties.load(new FileInputStream(configurationFile));
    }

    public String getMongoUri() {
        String val = properties.getProperty("mongo.uri");
        if (val!=null && !val.isEmpty())
            return val.trim();
        return null;
    }

    public String getDbName() {
        String val = properties.getProperty("dbname");
        if (val!=null && !val.isEmpty()) {
            return val.trim();
        }
        return null;
    }
}
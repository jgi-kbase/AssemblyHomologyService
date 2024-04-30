package us.kbase.test.assemblyhomology;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Clock;

import org.bson.Document;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;
import us.kbase.testutils.controllers.mongo.MongoController;

public class MongoStorageTestManager {

	public MongoController mongo;
	public MongoClient mc;
	public MongoDatabase db;
	public MongoAssemblyHomologyStorage storage;
	public Clock clockMock;
	public Version mongoDBVer;
	public int indexVer;
	public boolean wiredTiger;
	public boolean includeSystemIndexes;
	
	public MongoStorageTestManager(final String dbName) throws Exception {
		TestCommon.stfuLoggers();
		mongo = new MongoController(TestCommon.getMongoExe().toString(),
				TestCommon.getTempDir(),
				TestCommon.useWiredTigerEngine());
		wiredTiger = TestCommon.useWiredTigerEngine();
		System.out.println(String.format("Testing against mongo executable %s on port %s",
				TestCommon.getMongoExe(), mongo.getServerPort()));
		mc = new MongoClient("localhost:" + mongo.getServerPort());
		db = mc.getDatabase(dbName);
		
		final Document bi = db.runCommand(new Document("buildinfo", 1));
		final String version = bi.getString("version");
		mongoDBVer = Version.valueOf(version);
		indexVer = mongoDBVer.greaterThanOrEqualTo(Version.forIntegers(3, 4)) ? 2 : 1;
		includeSystemIndexes = mongoDBVer.lessThan(Version.forIntegers(3, 2)) && !wiredTiger;
		reset();
	}
	
	public void destroy() throws Exception {
		if (mc != null) {
			mc.close();
		}
		if (mongo != null) {
			try {
				mongo.destroy(TestCommon.isDeleteTempFiles());
			} catch (IOException e) {
				System.out.println("Error deleting temporarary files at: " +
						TestCommon.getTempDir());
				e.printStackTrace();
			}
		}
	}
	
	public void reset() throws Exception {
		// only drop the data, not the indexes, since creating indexes is slow and will be done
		// anyway when the new storage instance is created
		// db.drop();
		TestCommon.destroyDB(db);
		clockMock = mock(Clock.class);
		final Constructor<MongoAssemblyHomologyStorage> con = MongoAssemblyHomologyStorage.class.
				getDeclaredConstructor(MongoDatabase.class, Clock.class);
		con.setAccessible(true);
		storage = con.newInstance(db, clockMock);
	}
}

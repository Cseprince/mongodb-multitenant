package se.webinventions.mongomultitenant

import com.mongodb.DBObject
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.mongo.MongoSession
import org.grails.datastore.mapping.mongo.engine.MongoEntityPersister

/**
 * Created by IntelliJ IDEA.
 * User: kushal
 * Date: 9/12/11
 * Time: 8:44 PM
 * To change this template use File | Settings | File Templates.
 */
class MongoTenantMetaHelper {

    public MongoTenantMetaHelper() {
        overrideMongoTenantEntityPersisterGetCollectionName()
        println "QQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQqqq CALLED"
    }

    private void overrideMongoTenantEntityPersisterGetCollectionName() {

        MongoEntityPersister.metaClass.invokeMethod = {name, args ->
            System.out.println "invoked ${}"
        }

        MongoEntityPersister.metaClass.getCollectionName = {PersistentEntity persistentEntity, DBObject nativeEntry ->
            println "QQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQqqq yooooo"
            @SuppressWarnings("hiding") String collectionName;
            if (persistentEntity.isRoot()) {
                MongoSession mongoSession = (MongoSession) getSession();
                collectionName = mongoSession.getCollectionName(persistentEntity);
            }
            else {
                MongoSession mongoSession = (MongoSession) getSession();
                collectionName = mongoSession.getCollectionName(persistentEntity.getRootEntity());
                if (nativeEntry != null) {
                    nativeEntry.put(MONGO_CLASS_FIELD, persistentEntity.getDiscriminator());
                }
            }
            return collectionName;
        }

    }
}
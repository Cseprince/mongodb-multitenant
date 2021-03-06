package se.webinventions.mongomultitenant

import org.springframework.data.document.mongodb.MongoTemplate
import com.mongodb.Mongo
import org.apache.log4j.Logger
import org.grails.datastore.mapping.mongo.config.MongoCollection
import org.grails.datastore.mapping.model.PersistentEntity

/**
 * Created by IntelliJ IDEA.
 * This class acts as a wrapper around tenant templates. It keeps track of which tenant 'template' should be called/used based on
 * a tenant service. It intercepts all method calls to the class and delegates to the correct instance. should it not exist
 * it is created on the fly (lazy) , we extend the MongoTemplate just having to avoid to implement all MongoOperations interface methods
 * if the interface changes in the future.
 * Todo: Make it work with groovy interceptable instead.. only seems to be called when method is invoked directly on class and not on super class methods. :/
 */




class MongoTenantTemplateDelegator implements GroovyInterceptable {

    static Logger log = Logger.getLogger(this)

    String originalDatabaseName

    String originalCollectionName

    Mongo originalMongoInstance

    MongoTenantDatastore creatorStore

    PersistentEntity peUponCreate

    MongoCollection collectionUpconCreate;

    @Override
    def invokeMethod(String name, args) {
        if (log) {
            log.debug "Method $name invoked"
        }
		
		if("asBoolean".equals(name)){
			return true;
		}

        def delegateTo = creatorStore.getTenantDelegate(peUponCreate, originalMongoInstance);

        def originalMethod = null
        if (delegateTo) {
            originalMethod = MongoTemplate.metaClass.getMetaMethod(name, args)
        } else {
            log?.warn("could not delegate to tenant");
            //check if we are calling constructor of this object
        }

        return originalMethod ? originalMethod.invoke(delegateTo, args) : null
    }

    public MongoTenantTemplateDelegator(
            Mongo mongoInstance,
            String databaseName,
            String collectionName,
            MongoTenantDatastore store,
            MongoCollection collUponCreate,
            PersistentEntity peUponCreate
    ) {
        this.collectionUpconCreate = collUponCreate
        this.peUponCreate = peUponCreate
        this.originalCollectionName = collectionName
        this.originalDatabaseName = databaseName; //saved just for reference what it was from the beginning, used to calculate tenant database name
        this.originalMongoInstance = mongoInstance
        this.creatorStore = store;
    }
}
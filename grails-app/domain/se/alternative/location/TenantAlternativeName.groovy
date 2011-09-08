package se.alternative.location

import se.webinventions.mongomultitenant.TenantProvider
import org.bson.types.ObjectId

class TenantAlternativeName implements TenantProvider {

    ObjectId id
    String collectionNameSuffix
    String databaseNameSuffix
    String name = ""
    static constraints = {
    }

    static mapping = {
        name index: true

    }

}


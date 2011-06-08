

package se.webinventions

import se.webinventions.mongomultitenant.TenantProvider
import org.bson.types.ObjectId


class Tenant implements TenantProvider {

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


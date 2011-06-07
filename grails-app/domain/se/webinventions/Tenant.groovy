

package se.webinventions

import se.webinventions.mongomultitenant.TenantProvider


class Tenant implements TenantProvider {


    String collectionNameSuffix
    String databaseNameSuffix
    String name = ""
    static constraints = {
    }

  static mapping = {
     name index: true

   }

}


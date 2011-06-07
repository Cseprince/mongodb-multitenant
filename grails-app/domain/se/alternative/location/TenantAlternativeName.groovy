

package se.alternative.location

import se.webinventions.mongomultitenant.TenantProvider

class TenantAlternativeName implements TenantProvider {


    String collectionNameSuffix
    String databaseNameSuffix
    String name = ""
    static constraints = {
    }

  static mapping = {
     name index: true

   }

}


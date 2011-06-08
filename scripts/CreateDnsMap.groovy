includeTargets << grailsScript("Init")

target(main: "This will create the default domain classes that maps tenants towards servernames") {
  def ant = new AntBuilder();


  ant.mkdir(dir: 'grails-app/domain/se/webinventions')
  new File("grails-app/domain/se/webinventions/TenantDomainMap.groovy").write('''

package se.webinventions

import se.webinventions.mongomultitenant.TenantDomainMapProvider
import se.webinventions.mongomultitenant.TenantProvider
import org.bson.types.ObjectId
/**
 * Class to map tenant to a specific url.
 */
class TenantDomainMap implements TenantDomainMapProvider{

ObjectId id

static transients = ['tenantProvider']

  Tenant tenant
  String domainUrl


    TenantProvider getTenantProvider() {
   return tenant
}

    void setTenantProvider(TenantProvider tenantProvider) {
        this.tenant = tenantProvider
    }


    static constraints = {
    }
}

''')


   ant.mkdir(dir: 'grails-app/domain/se/webinventions')
  new File("grails-app/domain/se/webinventions/Tenant.groovy").write('''

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

''')


}

setDefaultTarget(main)




package se.webinventions

import se.webinventions.mongomultitenant.TenantDomainMapProvider
import se.webinventions.mongomultitenant.TenantProvider
import org.bson.types.ObjectId
/**
 * Class to map tenant to a specific url.
 */
class TenantDomainMap implements TenantDomainMapProvider{

ObjectId id

  Tenant tenant
  String domainUrl

    static transients = ['tenantProvider']

    TenantProvider getTenantProvider() {
   return tenant
}

    void setTenantProvider(TenantProvider tenantProvider) {
        this.tenant = tenantProvider
    }

    static constraints = {
    }
}


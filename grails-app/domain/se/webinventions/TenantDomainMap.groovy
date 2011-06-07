

package se.webinventions

import se.webinventions.mongomultitenant.TenantDomainMapProvider
/**
 * Class to map tenant to a specific url.
 */
class TenantDomainMap implements TenantDomainMapProvider{

  Tenant tenant
  String domainUrl


         public Tenant getTenant() {
          return this.tenant;
          }
    public void setTenant(Tenant ten) {
        this.tenant=ten;
    }
    static constraints = {
    }
}


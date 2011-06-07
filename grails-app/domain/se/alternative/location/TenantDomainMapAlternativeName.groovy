

package se.alternative.location

import se.webinventions.mongomultitenant.TenantDomainMapProvider

/**
 * Class to map tenant to a specific url.
 */
class TenantDomainMapAlternativeName implements TenantDomainMapProvider{

  se.alternative.location.TenantAlternativeName tenant
  String domainUrl


         public se.alternative.location.TenantAlternativeName getTenant() {
          return this.tenant;
          }
    public void setTenant(se.alternative.location.TenantAlternativeName ten) {
        this.tenant=ten;
    }
    static constraints = {
    }
}


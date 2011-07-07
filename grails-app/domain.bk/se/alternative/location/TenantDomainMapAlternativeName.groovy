

package se.alternative.location

import se.webinventions.mongomultitenant.TenantDomainMapProvider
import se.webinventions.mongomultitenant.TenantProvider

/**
 * Class to map tenant to a specific url.
 */
class TenantDomainMapAlternativeName implements TenantDomainMapProvider{
    static mapWith = "mongo"


    static transients = ['tenantProvider']

    Object id
    se.alternative.location.TenantAlternativeName tenant
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


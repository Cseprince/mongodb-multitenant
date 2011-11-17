import org.bson.types.ObjectId
import se.webinventions.Tenant
import se.webinventions.TenantDomainMap





grails.mongo.tenant.tenantclassname = "se.webinventions.Tenant"
grails.mongo.tenant.tenantsPerDb = 500
grails.mongo.tenant.excludingdomainclasses = [Tenant, TenantDomainMap]
//alternatively grails.mongo.tenant.includingdomainclasses =[Author,Book,ContentItem,Article] … etc
grails.mongo.tenant.defaultTenantName = "default"
grails.mongo.tenant.defaultTenantId = new ObjectId()
package se.webinventions.mongomultitenant

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.grails.datastore.mapping.core.ConnectionNotFoundException
import org.grails.datastore.mapping.core.Session
import org.springframework.beans.factory.InitializingBean


class TenantService implements ApplicationContextAware, InitializingBean {

    static transactional = true

    static scope = "session"

    static proxy = true

    def applicationContext

    def tenantResolverProxy

    def sessionFactory

    def config = ConfigurationHolder.config

    DefaultGrailsApplication grailsApplication

    MongoTenantDatastore mongoDatastore



    private Session getSession() {
        Session mongoSession
        try {
            mongoSession = mongoDatastore.retrieveSession()
        } catch (ConnectionNotFoundException ex) {
            mongoSession = null
            safeLog("could not get session from datastore .. " + ex)
        }
        return mongoSession;
    }

    public void doWithTenant(Object tenantId, Closure closure) throws Throwable {
        Object currentTenantId = tenantResolverProxy.getTenantId()
        tenantResolverProxy.setTenantId(tenantId)
        def mongoSession = getSession()
        safeLog "${mongoSession}"
        Throwable caught = null;
        try {
            closure.call();
        } catch (Throwable t) {
            caught = t;
            tenantResolverProxy.setTenantId(currentTenantId);
        } finally {
            tenantResolverProxy.setTenantId(currentTenantId);
        }
        if (caught != null) {
            throw caught;
        }
    }

    public TenantProvider createOrGetDefaultTenant(String name) {
        def tenantClassName = config?.grails?.mongo?.tenant?.tenantclassname ?: "se.webinventions.Tenant"
        def domainClass = grailsApplication.getClassForName(tenantClassName)
        TenantProvider tp = null
        try {
            tp = domainClass.findByName(name)
        } catch (Exception e) {
        } finally {
            if (!tp) {
                tp = createNewTenant(name)
                try {
                    if (tp?.validate()) {
                        tp?.save(flush: true)
                        safeLog("got and saved default tenant")
                    }
                    else {
                        safeLog("Could not save default tenant due to validation errors? " + tp?.errors)
                    }
                } catch (Exception e) {
                    safeLog("could not save default tenant" + e)
                }
            }
            return tp;
        }
    }

    public TenantProvider createNewTenant(String name) {
        def tenantsPerDb = config?.grails?.mongo?.tenant?.tenantsPerDb ?: 500

        if (tenantsPerDb instanceof Integer) {
            tenantsPerDb = 500
        }

        def tenantClassName = config?.grails?.mongo?.tenant?.tenantclassname ?: "se.webinventions.Tenant"
        def domainClass = grailsApplication.getClassForName(tenantClassName)
        def tenants

        try {
            tenants = domainClass?.list()
        } catch (Exception e) {
            safeLog("We are probably bootstrapping so list() could not be invoked on Tenant object to check number of teants when creating a new tenant..")
            tenants = []
        }

        def noOfTenants = tenants?.size()
        Integer dbNum = Math.floor(((double) noOfTenants) / ((double) tenantsPerDb))

        TenantProvider tp = domainClass.newInstance()

        tp.setName(name)
        tp.setCollectionNameSuffix("_" + name)
        tp.setDatabaseNameSuffix("_" + dbNum.toString())

        try {
            tp.save()
            tp.setCollectionNameSuffix(tp.getCollectionNameSuffix() + "_" + tp.id);
            tp.save(flush: true)
        } catch (Throwable e) {
            safeLog("could not save tenant on creation (bootstrapping??) " + e)
        }
        return tp; //saving has to be done by the user of the method in case heY/she wants to add more properties..
    }

    void setApplicationContext(ApplicationContext apctx) {
        this.applicationContext = apctx
    }

    void safeLog(def payLoad) {
        try {
            log.info(payLoad)
        } catch (Exception e) {
            println(payLoad)
        }
    }

    void afterPropertiesSet() {
        if (!tenantResolverProxy) {
            tenantResolverProxy = applicationContext.getBean("tenantResolverProxy")
        }
        if (!mongoDatastore) {
            mongoDatastore = applicationContext.getBean("mongoDatastore")
        }
    }
}

package se.webinventions.mongomultitenant


import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.springframework.beans.factory.InitializingBean


class DomainTenantResolverService implements MongodbTenantResolver, ApplicationContextAware, InitializingBean {

    static transactional = true

    static scope = "session"

    static proxy = true

    def tenantServiceProxy

    String currentServerName

    def config = ConfigurationHolder.config

    GrailsApplication grailsApplication

    ApplicationContext applicationContext

    List<TenantChangeListener> listeners = new LinkedList<TenantChangeListener>();

    TenantProvider defaultTenant

    TenantProvider currentTenant


    def resolveDefaultTenant() {
        TenantProvider tenant = resolveDomainTenant()
        defaultTenant = tenant;
        updateListeners()
        return defaultTenant
    }

    private TenantProvider resolveDomainTenant() {
        def domainTenantMap
        try {
            domainTenantMap = getTenantDomainMapping()
        }
        catch (Exception e) {
            try {
                log.info("Bootstrapping so resolving tenant to null tenant")
            } catch (Exception ex) {
                println("Bootstrapping so resolving tenant to null tenant")
            }
            domainTenantMap = null
        }
        domainTenantMap?.getTenant() ?: null
    }


    public def getTenantDomainMapping() throws Exception {
        this.currentServerName = resolveServerName()
        def tenantMappingClassName = config?.grails?.mongo?.tenant?.tenantmappingclassname ?: "se.webinventions.TenantDomainMap"
        def tenantMappingDomainClass = grailsApplication.getClassForName(tenantMappingClassName)
        String filteredUrl = (currentServerName.find(/.*:\/\/[^\/]*/)?.replaceAll(/^.*:\/\//, "")) ?: currentServerName
        boolean lock = false
        def domain = null
        filteredUrl.tokenize(".").size().times {
            if (!lock) {
                String compared = filteredUrl
                it.times {
                    compared = compared.reverse().replaceFirst(/[\w:]*\./, "").reverse()
                }
                domain = tenantMappingDomainClass.findByDomainUrl(compared)
                if (domain) {
                    lock = true
                }
            }
        }
        return domain;
    }

    private getCurrentRequestAttr() {
        try {
            return RequestContextHolder.currentRequestAttributes()
        } catch (IllegalStateException e) {
            return null
        }
    }

    private String resolveServerName() {
        return getCurrentRequestAttr()?.getRequest()?.getServerName() ?: "bootstrap"
    }

    private updateListeners() {
        this.listeners.each { listener ->
            listener.tenantChanged()
        }
    }

    def revertToDefaultTenant() {
        currentTenant = null;
        updateListeners()
        return defaultTenant
    }

    def getTenantId() {
        securityCheckDomainChangeAndTenantChange()
        if (currentTenant) {
            return currentTenant?.id
        } else {
            if (!defaultTenant) {
                return resolveDefaultTenant()?.id
            } else {
                return defaultTenant?.id
            }
        }
    }

    def securityCheckDomainChangeAndTenantChange() {
        if (!resolveServerName()?.equalsIgnoreCase(currentServerName)) {
            def newTenant = resolveDomainTenant()
            if (newTenant != defaultTenant) {
                if (PluginManagerHolder.pluginManager.hasGrailsPlugin('spring-security-core')) {
                    def springSecurityService = applicationContext.getBean("springSecurityService")
                    if (springSecurityService?.isLoggedIn()) {
                        springSecurityService?.reauthenticate();
                    }
                }
                defaultTenant = newTenant;
                updateListeners()
                currentTenant = null;
            }
        }
    }

    @Override
    String getTenantCollectionName(String originalCollectionName) {
        if (currentTenant) {
            return originalCollectionName + currentTenant?.getCollectionNameSuffix()
        } else {
            return originalCollectionName + defaultTenant?.getCollectionNameSuffix()
        }
    }

    @Override
    String getTenantDatabaseName(String originalDatabaseName) {
        if (currentTenant) {
            return originalDatabaseName + currentTenant?.getDatabaseNameSuffix()
        } else {
            def suffix = defaultTenant?.getDatabaseNameSuffix()
            return originalDatabaseName + suffix
        }
    }

    void resetTodefaultTenant() {
        resolveDefaultTenant()
        currentTenant = null
    }

    @Override
    public void setTenantId(def tenantId) {
        def tenantClassName = config?.grails?.mongo?.tenant?.tenantclassname ?: "se.webinventions.Tenant"
        currentTenant = applicationContext?.getBean(tenantClassName)?.get(tenantId)
    }

    public setTenant(def tenant) {
        if (!defaultTenant) {
            defaultTenant = tenant
        } else {
            currentTenant = tenant
        }
    }

    public def getTenant() {
        securityCheckDomainChangeAndTenantChange();
        if (currentTenant) {return currentTenant}
        else { return defaultTenant}
    }

    void resetToDefaultTenant() {
        revertToDefaultTenant()
    }

    void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    void addListener(TenantChangeListener l) {
        this.listeners.add(l)
    }

    void afterPropertiesSet() {
        if (!tenantServiceProxy) {
            tenantServiceProxy = applicationContext.getBean("tenantServiceProxy")
        }
    }
}

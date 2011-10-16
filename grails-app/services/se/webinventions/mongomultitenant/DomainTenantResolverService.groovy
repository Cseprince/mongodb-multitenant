package se.webinventions.mongomultitenant


import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

/**
 *
 */
class DomainTenantResolverService implements MongodbTenantResolver, ApplicationContextAware {
    //TODO: implement cashing (hosts) of already fetched tenants.
    private final Logger log = Logger.getLogger(getClass())
    private final static String BOOTSTRAP_TENANT_NAME = "bootstrap"
    static transactional = true
    static scope = "session"
    static proxy = true

    def tenantServiceProxy
    def currentServerName
    def config = ConfigurationHolder.getConfig();

    GrailsApplication grailsApplication
    ApplicationContext applicationContext
    List<TenantChangeListener> listeners = new LinkedList<TenantChangeListener>();

    TenantProvider defaultTenant //tenant that is cohering to the current url etc (default resovlved)
    TenantProvider currentTenant  //tenant that is forced by eg.g. admin

    Logger log = Logger.getLogger(getClass())


    def resolveDefaultTenant() {
        TenantProvider tenant = resolveDomainTenant()
        defaultTenant = tenant;
        updateListeners()
        return defaultTenant
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

    /**
     * Find tenant that is mapped to the current url, this method is the MAIN resolver method for deciding which tenant
     * we currently have
     * @return
     * @throws Exception
     */
    public Object getTenantDomainMapping() throws Exception {

        this.currentServerName = resolveServerName()


        def tenantMappingClassName = config?.grails?.mongo?.tenant?.tenantmappingclassname ?: "se.webinventions.TenantDomainMap"

        if (log.isDebugEnabled()) {
            log.debug("Using tenant mapping class: ${tenantMappingClassName}")
        }

        def domainClass = grailsApplication.getClassForName(tenantMappingClassName)

        if (!tenantServiceProxy) {
            tenantServiceProxy = applicationContext.getBean("tenantServiceProxy")
        }

        def foundMapping = null
        domainClass.list()?.each { domainTenantMapping ->
            if (currentServerName.toString().indexOf(domainTenantMapping.getDomainUrl() as String) > -1) {

                if (foundMapping != null) {
                    //determine if its a better match than the previous (more exact)
                    if (currentServerName.toString().indexOf(domainTenantMapping.getDomainUrl() as String) > -1 &&
                            currentServerName.toString().indexOf(foundMapping.getDomainUrl() as String) > -1) {
                        if (domainTenantMapping.getDomainUrl().length() >= foundMapping.getDomainUrl().length()) {
                            foundMapping = domainTenantMapping
                        }
                    }
                } else {
                    //first match, check if its at all matching, if not keep it as null
                    if (currentServerName.toString().indexOf(domainTenantMapping.getDomainUrl() as String) > -1) {
                        foundMapping = domainTenantMapping;
                    }
                }
            }
        }


        return foundMapping;
    }

    /**
     * perhaps make this public +send in the request attribute
     * and use it from a filter and make the service work on application context level instead
     * @return
     */
    private TenantProvider resolveDomainTenant() {

        def domainTenantMap

        try {
            domainTenantMap = getTenantDomainMapping()
        }
        catch (Exception e) {
            //we are in bootstrapping perhaps so the gorm methods are not yet available
            log.info("Bootstrapping so resolving tenant to null tenant")

            domainTenantMap = null

        }
        return domainTenantMap ? (domainTenantMap.getTenant() ?: null) : null
    }

    private updateListeners() {
        this.listeners.each { l ->
            l.tenantChanged()
        }
    }

    def revertToDefaultTenant() {
        currentTenant = null;
        updateListeners()
        return defaultTenant
    }

    Boolean hasCurrentTenant() {
        return (currentTenant != null)
    }

    Boolean setCurrentTenantToNull() {
        currentTenant = null
    }


    def getTenantId() {
        log.debug("getTenantId()")
        securityCheckDomainChangeAndTenantChange()
        if (currentTenant) {

            return currentTenant?.id
        } else {
            if (!defaultTenant) {
                return resolveDefaultTenant()?.id

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("returning currentTenant: ${defaultTenant}")
                }
                return defaultTenant.id
            }
        }
    }

    def securityCheckDomainChangeAndTenantChange() {


        //make security check based on the current server name change
        if (!resolveServerName()?.equalsIgnoreCase(currentServerName)) {
            //switch tenant
            def newTenant = resolveDomainTenant()
            if (newTenant != defaultTenant) {
                if (log.isDebugEnabled()) {
                    log.debug("securityCheckDomainChangeAndTenantChange() - changing tenant from '${defaultTenant}' to '${newTenant}'")
                }
                //we have a new domain and should logout if necessary.
                if (PluginManagerHolder.pluginManager.hasGrailsPlugin('spring-security-core')) {
                    def springSecurityService = applicationContext.getBean("springSecurityService")
                    if (springSecurityService?.isLoggedIn()) {
                        springSecurityService?.reauthenticate();
                    }
                }
                //todo add support for shiro security or others..
                defaultTenant = newTenant;
                updateListeners()
                currentTenant = null;
            }
        }
    }

    @Override
    String getTenantCollectionName(String originalCollectionName) {

        //check with ? because in bootstrap it will be NULL!
        if (currentTenant) {
            return originalCollectionName + currentTenant?.getCollectionNameSuffix()
        } else {
            return originalCollectionName + defaultTenant?.getCollectionNameSuffix()
        }
    }


    @Override
    String getTenantDatabaseName(String originalDatabaseName) {


        //check with ? because in bootstrapping situations the tenant will be NULL!
        if (currentTenant) {
            return originalDatabaseName + currentTenant?.getDatabaseNameSuffix()
        } else {
            def suffix = defaultTenant?.getDatabaseNameSuffix()
            return originalDatabaseName + suffix
        }
    }

    @Override
    void resetTodefaultTenant() {
        resolveDefaultTenant()
        currentTenant = null
    }

    @Override
    public void setTenantId(Object tenantid) {
        //find the tenant and set it
        def tenantClassName = config?.grails?.mongo?.tenant?.tenantclassname ?: "se.webinventions.Tenant"
        currentTenant = applicationContext?.getBean(tenantClassName)?.get(tenantid)
    }

    public setTenant(Object tenant) {
        if (!defaultTenant) {
            defaultTenant = tenant
        } else {
            currentTenant = tenant
        }
    }

    public Object getTenant() {
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
}

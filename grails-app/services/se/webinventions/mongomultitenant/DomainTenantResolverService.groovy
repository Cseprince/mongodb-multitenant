package se.webinventions.mongomultitenant


import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.web.converters.ConverterUtil
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.PluginManagerHolder
import org.bson.types.ObjectId

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
    GrailsApplication grailsApplication
    def tenantServiceProxy
    def currentServerName
    List<TenantChangeListener> listeners = new LinkedList<TenantChangeListener>();


    ApplicationContext applicationContext

    def config = ConfigurationHolder.getConfig();



    TenantProvider defaultTenant //tenant that is cohering to the current url etc (default resovlved)
    TenantProvider currentTenant  //tenant that is forced by eg.g. admin


    def resolvedefaultTenant() {
        TenantProvider tenant = resolveDomainTenant()
        defaultTenant = tenant;

        updateListeners(defaultTenant)
        return defaultTenant
    }


    private String resolveServerName() {

        def serverName = getCurrentRequestAttr()?.getRequest()?.getServerName();
        if (!serverName) {
            serverName = BOOTSTRAP_TENANT_NAME
        }
        if ( log.isDebugEnabled() )
            log.debug("resolveServerName() returning ${serverName}")
        return serverName
    }

    private getCurrentRequestAttr() {
        try {
            return RequestContextHolder.currentRequestAttributes()
        } catch (IllegalStateException e) {
            return null
        }

    }

    /**
     * Find tenant that is mapped to the current url, this method is the MAIN resolver method for deciding which tenant
     * we currently have
     * @return
     * @throws Exception
     */
    public Object getTenantDomainMapping() throws Exception {
        log.debug("getTenantDomainMapping()")

        this.currentServerName = resolveServerName()

        if ( currentServerName == BOOTSTRAP_TENANT_NAME) {
            return null
        }

        def tenantMappingClassName = config?.grails?.mongo?.tenant?.tenantmappingclassname ?: "se.webinventions.TenantDomainMap"

        if ( log.isDebugEnabled() ) {
            log.debug("Using tenant mapping class: ${tenantMappingClassName}")
        }

        def domainClass = grailsApplication.getClassForName(tenantMappingClassName)

        if (!tenantServiceProxy) {
            tenantServiceProxy = applicationContext.getBean("tenantServiceProxy")
        }

        def domainTenantMappings
        def tenant;

        def foundMapping = false

        domainTenantMappings = domainClass.list()

        domainTenantMappings?.each { domtm ->

            if (currentServerName.toString().indexOf(domtm.getDomainUrl()) > -1) {
                if (foundMapping != null) {
                    //determine if its a better match than the previous (more exact)


                    if (currentServerName.toString().indexOf(domtm.getDomainUrl()) > -1 &&
                            currentServerName.toString().indexOf(foundMapping.getDomainUrl()) > -1) {

                        def fml = foundMapping.getDomainUrl().length()
                        def dml = domtm.getDomainUrl().length()
                        if (dml >= fml) {

                            foundMapping = domtm
                        }

                    }
                } else {
                    //first match, check if its at all matching, if not keep it as null
                    if (currentServerName.toString().indexOf(domtm.getDomainUrl()) > -1) {
                        foundMapping = domtm;
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
        def dommap
        def tenant
        try {
            dommap = getTenantDomainMapping()
        }
        catch (Exception e) {

            //we are in bootstrapping perhaps so the gorm methods are not yet available
            log.info("Bootstrapping so resolving tenant to null tenant")
            return null

        };


        if (dommap) {
            if (dommap?.getTenant()) {
                tenant = dommap.getTenant()
            }

        }



        return tenant;
    }

    private updateListeners(TenantProvider newtenant) {
        this.listeners.each { l ->
            l.tenantChanged()
        }
    }

    def revertToDefaultTenant() {
        currentTenant = null;
        updateListeners(defaultTenant)
        return defaultTenant
    }

    Boolean hasCurrentTenant() {
        return ( currentTenant != null )
    }

    Boolean setCurrentTenantToNull() {
        currentTenant = null
    }


    def getTenantId() {
        securityCheckDomainChangeAndTenantChange()

        if (currentTenant) {
            return currentTenant?.id
        } else {

            if (!defaultTenant) {
                return resolvedefaultTenant()?.id
            } else {
                return defaultTenant?.id
            }

        }
    }

    def securityCheckDomainChangeAndTenantChange() {

        //make security check based on the current server name change
        if (!resolveServerName()?.equalsIgnoreCase(currentServerName)) {
            //switch tenant
            def newTenant = resolveDomainTenant()
            if (newTenant != defaultTenant) {
                //we have a new domain and should logout if necessary.
                if (PluginManagerHolder.pluginManager.hasGrailsPlugin('spring-security-core')) {
                    def springSecurityService = applicationContext.getBean("springSecurityService")
                    if (springSecurityService?.isLoggedIn()) {
                        springSecurityService?.reauthenticate();
                    }

                }
                //todo add support for shiro security or others..


                defaultTenant = newTenant;
                updateListeners(newTenant)
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
        resolvedefaultTenant()
        currentTenant = null
    }

    @Override
    public void setTenantId(Object tenantid) {
        //find the tenant and set it
        def tenantClassName = config?.grails?.mongo?.tenant?.tenantclassname ?: "se.webinventions.Tenant"
        currentTenant = applicationContext?.getBean(tenantClassName).get(tenantid)
        //currentTenant = tenantid
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

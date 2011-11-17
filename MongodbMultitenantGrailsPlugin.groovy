import se.webinventions.mongomultitenant.MongoTenantDatastoreFactoryBean
import org.springframework.aop.scope.ScopedProxyFactoryBean
import se.webinventions.mongomultitenant.DomainTenantResolverService
import se.webinventions.mongomultitenant.TenantService
import se.webinventions.mongomultitenant.MongoMultiTenantConstants



class MongodbMultitenantGrailsPlugin {

    def version = "1.0.0.RC1"

    def grailsVersion = "1.3.6 > *"

    def dependsOn = [mongodb: " * > 0.9"]

    def loadAfter = ['mongodb']

    def pluginExcludes = [
            "grails-app/views/error.gsp", "grails-app/domain/**"
    ]

    def author = "Per Sundberg"

    def authorEmail = "contact@webinventions.se"

    def title = "Mongodb Multitenant plugin"

    def description = MongoMultiTenantConstants.description

    def documentation = "http://grails.org/plugin/mongodb-multitenant"

    def doWithSpring = {
        def mongoConfig = application.config?.grails?.mongo

        tenantResolver(DomainTenantResolverService) {
            grailsApplication = ref("grailsApplication")
        }
        tenantResolverProxy(ScopedProxyFactoryBean) {
            targetBeanName = 'tenantResolver'
            proxyTargetClass = true
        }

        mongoDatastore(MongoTenantDatastoreFactoryBean) {
            mongo = ref("mongoBean")
            mappingContext = ref("mongoMappingContext")
            config = mongoConfig.toProperties()
            tenantResolverProxy = ref("tenantResolverProxy")

        }

        tenantService(TenantService) {
            grailsApplication = ref("grailsApplication")
        }

        tenantServiceProxy(ScopedProxyFactoryBean) {
            targetBeanName = 'tenantService'
            proxyTargetClass = true
        }
    }

    def doWithDynamicMethods = { ctx ->
    }

    def doWithApplicationContext = { applicationContext ->
    }

    def onChange = { event ->
    }

    def onConfigChange = { event ->
    }
}

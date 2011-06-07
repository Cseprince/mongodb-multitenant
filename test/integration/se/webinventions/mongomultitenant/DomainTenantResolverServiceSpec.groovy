package se.webinventions.mongomultitenant

import spock.lang.*
import grails.plugin.spock.*
import grails.datastore.test.DatastoreUnitTestMixin
import se.webinventions.*
import se.alternative.*
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.SessionScope
import org.springframework.web.context.request.ServletRequestAttributes



class DomainTenantResolverServiceSpec extends IntegrationSpec {

    def domainTenantResolverServiceProxy
    def tenantServiceProxy
    def grailsApplication
    def config

    def setupSpec() {

    }

    def setup() {
        domainTenantResolverServiceProxy.tenantServiceProxy = tenantServiceProxy
        domainTenantResolverServiceProxy.grailsApplication = grailsApplication

        tenantServiceProxy.grailsApplication = grailsApplication

        config = grailsApplication.config
    }

    def "resolve default tenant outside bootstrapping"() {


        when:
        def defaultTenant = domainTenantResolverServiceProxy.resolvedefaultTenant()

        then:
        defaultTenant.name == "maindefaulttenant"


    }

    def "resolve default tenant outside bootstrapping with custom name"() {

        given:
        config.grails.mongo.tenant.defaultTenantName = "testnametenant"

        when:
        def defaultTenant = domainTenantResolverServiceProxy.resolvedefaultTenant()

        then:
        defaultTenant.name == "testnametenant"


    }



    def "test normal domain mapping"() {

        given:

        def ten1 = tenantServiceProxy.createNewTenant("test1")
        def ten2 = tenantServiceProxy.createNewTenant("test2")
        def ten3 = tenantServiceProxy.createNewTenant("test3")
        grailsApplication.getParentContext().getBeanFactory().registerScope("session", new SessionScope());

        def request = new MockHttpServletRequest();
        request.setServerName("test2.localhost")
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))

        def tdm1 = new TenantDomainMap(domainUrl: "test1.localhost", tenant: ten1)
        def tdm2 = new TenantDomainMap(domainUrl: "test2.localhost", tenant: ten2)
        ten1.save(flush: true)
        ten2.save(flush: true)
        ten3.save()
        tdm1.save(flush: true)
        tdm2.save(flush: true)



        def tdmtest1 = domainTenantResolverServiceProxy.getTenantDomainMapping(ten1)
        def tdmtest2 = domainTenantResolverServiceProxy.getTenantDomainMapping(ten2)
        def tdmtestnoexist = domainTenantResolverServiceProxy.getTenantDomainMapping(ten3)

        expect:

        tdmtest1 != null
        tdmtest1.id == tdm1.id

        tdmtest2 != null
        tdmtest2.id == tdm2.id

        tdmtestnoexist == null






    }


}
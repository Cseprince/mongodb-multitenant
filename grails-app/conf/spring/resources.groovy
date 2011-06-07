

import se.webinventions.mongomultitenant.DomainTenantResolverService
import org.springframework.aop.scope.ScopedProxyFactoryBean
import se.webinventions.mongomultitenant.TenantService

// Place your Spring DSL code here
beans = {




    tenantService(TenantService) {

    }
       tenantServiceProxy(ScopedProxyFactoryBean) {
      targetBeanName = 'tenantService'
      proxyTargetClass = true
    }

    domainTenantResolverService(DomainTenantResolverService) {

    }
       domainTenantResolverServiceProxy(ScopedProxyFactoryBean) {
      targetBeanName = 'domainTenantResolverService'
      proxyTargetClass = true
    }
}

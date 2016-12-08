package org.dukecon.server.adapter

import org.dukecon.server.conference.ConferencesConfiguration
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertySource

/**
 * Reads all conferences from configuration file and generates an #ConferenceDataProvider for each.
 *
 * @author Falk Sippach, falk@jug-da.de, @sippsack
 */
class DataProviderLoader implements BeanDefinitionRegistryPostProcessor {

    private final ConferencesConfiguration configuration = new ConferencesConfiguration()

    DataProviderLoader(Environment env) {
        configuration.conferences.addAll(ConferencesConfiguration.fromFile('conferences.yml', getAllKnownConfigurationProperties(env))?.conferences)
    }

    private static Map<String, Object> getAllKnownConfigurationProperties(ConfigurableEnvironment env) {
        Map<String, Object> result = [:]
        env.propertySources.each {PropertySource ps ->
            if (ps instanceof EnumerablePropertySource) {
                ((EnumerablePropertySource) ps).propertyNames.each {String name ->
                    result[name] = ps.getProperty(name)
                }
            }
        }
        result
    }

    @Override
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        configuration.conferences.each { ConferencesConfiguration.Conference config ->
            if (config.backupUri) {
                BeanDefinitionBuilder builderDataProviderRemote = BeanDefinitionBuilder.genericBeanDefinition(WebResourceDataProviderRemote)
                builderDataProviderRemote.addConstructorArgValue({ ->
                    (config.talksUri.startsWith('http') ? new URL(config.talksUri) : this.class.getResourceAsStream("/${config.talksUri}"))
                } as RawDataResourceSupplier)
                builderDataProviderRemote.addConstructorArgValue(config)
                beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider remote", builderDataProviderRemote.beanDefinition)

                BeanDefinitionBuilder builderDataProvider = BeanDefinitionBuilder.genericBeanDefinition(WebResourceDataProvider)
                builderDataProvider.addConstructorArgReference("${config.name} dataprovider remote")
                beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider", builderDataProvider.beanDefinition)

                BeanDefinitionBuilder builderHealthCheck = BeanDefinitionBuilder.genericBeanDefinition(WebResourceDataProviderHealthIndicator)
                builderHealthCheck.addConstructorArgReference("${config.name} dataprovider")
                builderHealthCheck.addConstructorArgReference("${config.name} dataprovider remote")
                beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider health indicator", builderHealthCheck.beanDefinition)
            } else {
                BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(LocalResourceDataProvider)
                def dataExtractor = config.extractorClass.newInstance(config.id, this.class.getResourceAsStream("/${config.talksUri}"), config.startDate, config.name, config.url)
                builder.addConstructorArgValue(dataExtractor)
                beanDefinitionRegistry.registerBeanDefinition("${config.name} dataprovider", builder.beanDefinition)
            }
        }
    }

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
    }
}

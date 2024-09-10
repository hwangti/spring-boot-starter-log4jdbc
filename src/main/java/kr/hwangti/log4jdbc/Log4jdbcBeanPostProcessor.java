package kr.hwangti.log4jdbc;

import net.sf.log4jdbc.log.slf4j.Slf4jSpyLogDelegator;
import net.sf.log4jdbc.sql.jdbcapi.DataSourceSpy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * A {@link BeanPostProcessor} implementation that sets up log4jdbc logging.
 * To do so, it:
 * <ul>
 * <li>Copies log4jdbc configuration properties from the Spring {@link Environment} to system properties (log4jdbc only reads system properties)</li>
 * <li>Wraps {@link DataSource} beans with {@link DataSourceSpy}</li>
 * </ul>
 *
 * @author Craig Andrews
 * @see net.sf.log4jdbc.Properties
 */
public class Log4jdbcBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    private Environment environment;

    private static final String[] PROPERTIES = {
            "log4jdbc.auto.load.popular.drivers",
            "log4jdbc.debug.stack.prefix",
            "log4jdbc.drivers",
            "log4jdbc.dump.booleanastruefalse",
            "log4jdbc.dump.fulldebugstacktrace",
            "log4jdbc.dump.sql.addsemicolon",
            "log4jdbc.dump.sql.create",
            "log4jdbc.dump.sql.delete",
            "log4jdbc.dump.sql.insert",
            "log4jdbc.dump.sql.maxlinelength",
            "log4jdbc.dump.sql.select",
            "log4jdbc.dump.sql.update",
            "log4jdbc.log4j2.properties.file",
            "log4jdbc.sqltiming.error.threshold",
            "log4jdbc.sqltiming.warn.threshold",
            "log4jdbc.statement.warn",
            "log4jdbc.spylogdelegator.name",
            "log4jdbc.trim.sql",
            "log4jdbc.trim.sql.extrablanklines",
            "log4jdbc.suppress.generated.keys.exception",
            "spring.jpa.properties.hibernate.format_sql",
            "spring.jpa.properties.hibernate.highlight_sql"
    };

    /**
     * Create a new {@link Log4jdbcBeanPostProcessor}.
     */
    public Log4jdbcBeanPostProcessor() {
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            initializeProperty();
            return new DataSourceSpy((DataSource) bean);
        }

        return bean;
    }

    private void initializeProperty() {
        // log4jdbc only reads configuration from system properties,
        // so copy relevant environment property to system properties
        // See net.sf.log4jdbc.Properties.getProperties()
        for (final String property : PROPERTIES) {
            if (this.environment.containsProperty(property)) {
                String p = this.environment.getProperty(property);

                if (p != null) {
                    System.setProperty(property, p);
                }
            }
        }

        // Use slf4j by default.
        // Most users will have slf4j configured
        // (because Spring does that by default) and they won't be using log4j (which is the log4jdbc default)
        System.setProperty(
                "log4jdbc.spylogdelegator.name",
                this.environment.getProperty("log4jdbc.spylogdelegator.name", Slf4jSpyLogDelegator.class.getName()));
    }

}

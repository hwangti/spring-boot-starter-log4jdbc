package kr.hwangti.log4jdbc;

import net.sf.log4jdbc.sql.jdbcapi.DataSourceSpy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration Auto-Configuration} for log4jdbc.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(DataSourceSpy.class)
@ConditionalOnProperty(name = "log4jdbc.spy.enabled", havingValue = "true", matchIfMissing = true)
public class Log4jdbcAutoConfiguration {

    /**
     * Create a new {@link Log4jdbcAutoConfiguration}.
     */
    public Log4jdbcAutoConfiguration() {
    }

    /**
     * {@link Bean} for {@link Log4jdbcBeanPostProcessor}.
     *
     * @return the {@link Log4jdbcBeanPostProcessor} bean
     */
    @Bean
    public static Log4jdbcBeanPostProcessor log4jdbcBeanPostProcessor() {
        return new Log4jdbcBeanPostProcessor();
    }

}

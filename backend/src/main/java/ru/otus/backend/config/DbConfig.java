package ru.otus.backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DbConfig {

  @Bean(name = "masterDataSource")
  @ConfigurationProperties(prefix = "spring.datasource.master")
  public DataSource masterDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "slaveDataSource")
  @ConditionalOnProperty(prefix = "spring.datasource.slave", name = "url")
  @ConfigurationProperties(prefix = "spring.datasource.slave")
  public DataSource slaveDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "routingDataSource")
  public DataSource routingDataSource(
          @Qualifier("masterDataSource") DataSource masterDataSource,
          @Autowired(required = false) @Qualifier("slaveDataSource") DataSource slaveDataSource) {

    ReplicationRoutingDataSource routingDataSource = new ReplicationRoutingDataSource();

    Map<Object, Object> dataSources = new HashMap<>();
    dataSources.put("master", masterDataSource);
    if (slaveDataSource != null) {
      dataSources.put("slave", slaveDataSource);
    }

    routingDataSource.setTargetDataSources(dataSources);
    routingDataSource.setDefaultTargetDataSource(masterDataSource);

    return routingDataSource;
  }

  @Primary
  @Bean(name = "mainDataSource")
  public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
    return new LazyConnectionDataSourceProxy(routingDataSource);
  }

  @Bean
  public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("mainDataSource") DataSource dataSource) {
    return new NamedParameterJdbcTemplate(dataSource);
  }
}
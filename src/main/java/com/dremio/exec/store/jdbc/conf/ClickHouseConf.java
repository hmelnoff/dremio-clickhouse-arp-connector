/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import com.dremio.options.OptionManager;
import com.dremio.security.CredentialsService;

import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.Secret;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcPluginConfig;
import com.dremio.exec.store.jdbc.JdbcStoragePlugin;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;
import io.protostuff.Tag;
import java.util.Properties;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
/**
 * Configuration for ClickHouseConf sources.
 */
@SourceType(value = "ClickHouse", label = "ClickHouse", uiConfig = "clickhouse-layout.json", externalQuerySupported = true)
public class ClickHouseConf extends AbstractArpConf<ClickHouseConf> {
  private static final String ARP_FILENAME = "arp/implementation/clickhouse-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
  private static final String DRIVER = "com.clickhouse.jdbc.ClickHouseDriver";
  private static Logger logger = Logger.getLogger(ClickHouseConf.class);

  @NotBlank
  @Tag(1)
  @DisplayMetadata(label = "Host")
  public String host;

  @NotBlank
  @Tag(2)
  @Min(1)
  @Max(65535)
  @DisplayMetadata(label = "Port")
  public String port;

  @NotBlank
  @Tag(3)
  @DisplayMetadata(label = "Username")
  public String username;

  @NotBlank
  @Tag(4)
  @Secret
  @DisplayMetadata(label = "Password")
  public String password;

  @Tag(5)
  @DisplayMetadata(label = "Record fetch size")
  @NotMetadataImpacting
  public int fetchSize = 200;

  @Tag(6)
  @DisplayMetadata(label = "Encrypt connection")
  public boolean useSsl = false;

  @Tag(7)
  @NotMetadataImpacting
  @JsonIgnore
  @DisplayMetadata(label = "Grant External Query access (External Query allows creation of VDS from a Snowflake query. Learn more here: https://docs.dremio.com/data-sources/external-queries.html#enabling-external-queries)")
  public boolean enableExternalQuery = false;

  @Tag(8)
  @DisplayMetadata(label = "Maximum idle connections")
  @NotMetadataImpacting
  public int maxIdleConns = 8;

  @Tag(9)
  @DisplayMetadata(label = "Connection idle time (s)")
  @NotMetadataImpacting
  public int idleTimeSec = 60;

  @NotBlank
  @Tag(10)
  @DisplayMetadata(label = "Database")
  public String database;

  @VisibleForTesting
  public String toJdbcConnectionString() {
    final String host = checkNotNull(this.host, "Missing host.");
    final String username = checkNotNull(this.username, "Missing username.");
    final String password = checkNotNull(this.password, "Missing password.");
    final String port = checkNotNull(this.port, "Missing port.");
    final String database = checkNotNull(this.database, "Missing database.");

    final String connect = String.format("jdbc:ch://%s:%s/%s", host, port, database);
    logger.info("url to ClickHouse: " + connect);
    return connect;
  }

    @Override
    @VisibleForTesting
    public JdbcPluginConfig buildPluginConfig(
            JdbcPluginConfig.Builder configBuilder,
            CredentialsService credentialsService,
            OptionManager optionManager
    ){
        return configBuilder.withDialect(getDialect())
                .withDatasourceFactory(this::newDataSource)
                .withAllowExternalQuery(enableExternalQuery)
                .build();
    }

  private CloseableDataSource newDataSource() {
        final Properties properties = new Properties();

        if (useSsl) {
        properties.setProperty("sslConnection", "true");
        }
    return DataSources.newGenericConnectionPoolDataSource(DRIVER,
      toJdbcConnectionString(), username, password, properties, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE, maxIdleConns, idleTimeSec);
  }

  @Override
  public ArpDialect getDialect() {
    return ARP_DIALECT;
  }

  @VisibleForTesting
  public static ArpDialect getDialectSingleton() {
    return ARP_DIALECT;
  }
}
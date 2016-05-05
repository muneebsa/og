/*
 * Copyright (C) 2005-2015 Cleversafe, Inc. All rights reserved.
 * 
 * Contact Information: Cleversafe, Inc. 222 South Riverside Plaza Suite 1700 Chicago, IL 60606, USA
 * 
 * licensing@cleversafe.com
 */

package com.cleversafe.og.json;

import java.util.Map;

import com.cleversafe.og.api.DataType;
import com.cleversafe.og.http.Api;
import com.cleversafe.og.http.Scheme;
import com.google.common.collect.Maps;

public class OGConfig {
  public Scheme scheme;
  public SelectionConfig<String> host;
  public Integer port;
  public Api api;
  public String uriRoot;
  public ContainerConfig container;
  public Map<String, SelectionConfig<String>> headers;
  public OperationConfig write;
  public OperationConfig overwrite;
  public OperationConfig metadata;
  public OperationConfig read;
  public OperationConfig delete;
  public OperationConfig list;
  public OperationConfig containerList;
  public SelectionConfig<FilesizeConfig> filesize;
  public DataType data;
  public ConcurrencyConfig concurrency;
  public AuthenticationConfig authentication;
  public ClientConfig client;
  public StoppingConditionsConfig stoppingConditions;
  public ObjectManagerConfig objectManager;
  public boolean shutdownImmediate;
  public boolean virtualHost;

  public OGConfig() {
    this.scheme = Scheme.HTTP;
    this.host = null;
    this.port = null;
    this.api = null;
    this.uriRoot = null;
    this.container = new ContainerConfig();
    this.headers = Maps.newLinkedHashMap();
    this.write = new OperationConfig();
    this.overwrite = new OperationConfig();
    this.read = new OperationConfig();
    this.metadata = new OperationConfig();
    this.delete = new OperationConfig();
    this.list = new OperationConfig();
    this.containerList = new OperationConfig();
    this.filesize = null;
    this.data = DataType.RANDOM;
    this.concurrency = null;
    this.authentication = new AuthenticationConfig();
    this.client = new ClientConfig();
    this.stoppingConditions = new StoppingConditionsConfig();
    this.objectManager = new ObjectManagerConfig();
    this.shutdownImmediate = true;
    this.virtualHost = false;
  }
}

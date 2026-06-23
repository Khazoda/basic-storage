package com.khazoda.basicstorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
  public static final String BS_NAMESPACE = "basicstorage";
  public static final String BS_NAME = "Basic Storage";
  public static final Logger BS_LOG = LoggerFactory.getLogger(BS_NAME);
  public static final int CRATE_MAX_COUNT = 1000000000; // 1 Billion
}
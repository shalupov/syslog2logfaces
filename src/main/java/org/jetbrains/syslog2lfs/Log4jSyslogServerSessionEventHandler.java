package org.jetbrains.syslog2lfs;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.productivity.java.syslog4j.SyslogConstants;
import org.productivity.java.syslog4j.server.SyslogServerEventIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.SyslogServerSessionEventHandlerIF;
import org.productivity.java.syslog4j.server.impl.event.structured.StructuredSyslogServerEvent;
import org.productivity.java.syslog4j.util.SyslogUtility;

import java.net.SocketAddress;
import java.util.HashMap;

class Log4jSyslogServerSessionEventHandler implements SyslogServerSessionEventHandlerIF {
  private final Appender appender;

  public Log4jSyslogServerSessionEventHandler(Appender appender) {
    this.appender = appender;
  }

  @Override
  public void initialize(SyslogServerIF syslogServer) {

  }

  @Override
  public void destroy(SyslogServerIF syslogServer) {

  }

  @Override
  public Object sessionOpened(SyslogServerIF syslogServer, SocketAddress socketAddress) {
    return null;
  }

  @Override
  public void event(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress, SyslogServerEventIF event) {
    if (event instanceof StructuredSyslogServerEvent)
      logStructuredEvent((StructuredSyslogServerEvent) event);
    else
      logTraditionalEvent(event);
  }

  private void logTraditionalEvent(SyslogServerEventIF event) {
    final LoggingEvent loggingEvent = new LoggingEvent(
        event.getHost(),
        Logger.getLogger(SyslogUtility.getFacilityString(event.getFacility() << 3)),
        event.getDate().getTime(),
        getLevel(event),
        event.getMessage(),
        null
    );

    appender.doAppend(loggingEvent);
  }

  private void logStructuredEvent(StructuredSyslogServerEvent event) {
    final String applicationName = event.getApplicationName();
    final String processId = event.getProcessId();

    HashMap<String, String> properties = new HashMap<String, String>();

    properties.put("application", "syslog");
    properties.put("hostname", event.getHost());

    final String facility = SyslogUtility.getFacilityString(event.getFacility() << 3);

    final LoggingEvent loggingEvent = new LoggingEvent(
        event.getHost(),
        Logger.getLogger(isNullOrEmpty(applicationName) ? facility : (facility + "." + applicationName)),
        event.getDate().getTime(),
        getLevel(event),
        trimMessage(event.getMessage()),
        processId,
        null,
        null,
        null,
        properties
    );

    appender.doAppend(loggingEvent);
  }

  private boolean isNullOrEmpty(String s) {
    return s == null || s.length() == 0;
  }

  private String trimMessage(String message) {
    for (int i = 0; i < message.length(); i++) {
      final char c = message.charAt(i);

      if (c != ' ' && c != '-')
        return message.substring(i);
    }

    return "";
  }

  private Level getLevel(SyslogServerEventIF event) {
    switch (event.getLevel()) {
      case SyslogConstants.LEVEL_DEBUG:
        return Level.DEBUG;
      case SyslogConstants.LEVEL_INFO:
        return Level.INFO;
      case SyslogConstants.LEVEL_NOTICE:
        return Level.DEBUG;
      case SyslogConstants.LEVEL_WARN:
        return Level.WARN;
      case SyslogConstants.LEVEL_ERROR:
        return Level.ERROR;
      case SyslogConstants.LEVEL_CRITICAL:
        return Level.FATAL;
      case SyslogConstants.LEVEL_ALERT:
        return Level.FATAL;
      case SyslogConstants.LEVEL_EMERGENCY:
        return Level.FATAL;

      default:
        return Level.INFO;
    }
  }

  @Override
  public void exception(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress, Exception exception) {
  }

  @Override
  public void sessionClosed(Object session, SyslogServerIF syslogServer, SocketAddress socketAddress, boolean timeout) {
  }
}

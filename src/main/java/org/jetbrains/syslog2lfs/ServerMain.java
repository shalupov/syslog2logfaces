package org.jetbrains.syslog2lfs;

import com.moonlit.logfaces.appenders.AsyncSocketAppender;
import com.moonlit.logfaces.appenders.LFXMLLayout;
import org.productivity.java.syslog4j.server.SyslogServer;
import org.productivity.java.syslog4j.server.SyslogServerConfigIF;
import org.productivity.java.syslog4j.server.SyslogServerIF;
import org.productivity.java.syslog4j.server.impl.net.tcp.TCPNetSyslogServerConfigIF;
import org.productivity.java.syslog4j.util.SyslogUtility;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Properties;

public class ServerMain {
  public static boolean CALL_SYSTEM_EXIT_ON_FAILURE = true;
  public static String SYSLOG_PROTOCOL = "tcp";

  public static class Options {
    public String host = null;
    public String logfacesHost = null;
    public String port = null;
    public String timeout = null;

    public String usage = null;
  }

  public static void usage(String problem) {
    if (problem != null) {
      System.out.println("Error: " + problem);
      System.out.println();
    }

    System.out.println("Usage:");
    System.out.println();
    System.out.println("SyslogServer [-h <host>] [-p <port>] [-t <timeout>] -l <lfsserveraddress>[:<port>]");
    System.out.println();
    System.out.println("-h <host>    host or IP to bind");
    System.out.println("-p <port>    port to bind");
    System.out.println("-t <timeout> socket timeout (in milliseconds)");
  }

  public static Options parseOptions(String[] args) {
    Options options = new Options();

    int i = 0;
    while (i < args.length) {
      String arg = args[i++];

      if ("-h".equals(arg)) {
        if (i == args.length) {
          options.usage = "Must specify host with -h";
          return options;
        }
        options.host = args[i++];
      } else if ("-p".equals(arg)) {
        if (i == args.length) {
          options.usage = "Must specify port with -p";
          return options;
        }
        options.port = args[i++];
      } else if ("-t".equals(arg)) {
        if (i == args.length) {
          options.usage = "Must specify value (in milliseconds)";
          return options;
        }
        options.timeout = args[i++];
      } else if ("-l".equals(arg)) {
        if (i == args.length) {
          options.usage = "Must specify logfaces server address";
          return options;
        }
        options.logfacesHost = args[i++];
      } else {
        options.usage = "Unknown option " + arg;
        return options;
      }
    }

    if (options.logfacesHost == null)
      options.usage = "Must specify logfaces server with -l";

    return options;
  }

  public static void main(String[] args) throws Exception {
    Locale.setDefault(Locale.ENGLISH);

    Options options = parseOptions(args);

    if (options.usage != null) {
      usage(options.usage);
      if (CALL_SYSTEM_EXIT_ON_FAILURE) {
        System.exit(1);
      } else {
        return;
      }
    }

    SyslogServerIF syslogServer = SyslogServer.getInstance(SYSLOG_PROTOCOL);

    SyslogServerConfigIF syslogServerConfig = syslogServer.getConfig();

    syslogServerConfig.setUseStructuredData(true);

    if (options.host != null) {
      syslogServerConfig.setHost(options.host);
    }

    if (options.port != null) {
      syslogServerConfig.setPort(Integer.parseInt(options.port));
    }

    if (options.timeout != null) {
      ((TCPNetSyslogServerConfigIF) syslogServerConfig).setTimeout(Integer.parseInt(options.timeout));
    }

    final AsyncSocketAppender lfsAppender = new AsyncSocketAppender();
    configureAppender(lfsAppender, options);
    lfsAppender.activateOptions();

    final LFXMLLayout layout = (LFXMLLayout) getField(lfsAppender, "layout");
    final Properties properties = (Properties) getField(layout, "properties");
    properties.clear();

    syslogServerConfig.addEventHandler(new Log4jSyslogServerSessionEventHandler(lfsAppender));

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        lfsAppender.close();
      }
    });

    SyslogServer.getThreadedInstance(SYSLOG_PROTOCOL);

    //noinspection InfiniteLoopStatement
    while (true) {
      SyslogUtility.sleep(1000);
    }
  }

  private static void configureAppender(AsyncSocketAppender appender, Options options) {
    final String host = options.logfacesHost;

    final int i = host.indexOf(':');
    if (i < 0) {
      appender.setRemoteHost(host);
    } else {
      appender.setRemoteHost(host.substring(0, i));
      appender.setPort(Integer.parseInt(host.substring(i + 1)));
    }
  }

  private static Object getField(Object o, String fieldName) throws IllegalAccessException, NoSuchFieldException {
    final Class cls = o.getClass();

    final Field declaredField = cls.getDeclaredField(fieldName);
    declaredField.setAccessible(true);

    return declaredField.get(o);
  }
}

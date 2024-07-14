package com.soulfiremc.server.viaversion.platform;

import com.soulfiremc.server.viaversion.JLoggerToSLF4J;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class SFViaConfig extends AbstractViaConfig {

  private static final List<String> UNSUPPORTED = List.of(
    "checkforupdates",
    "bungee-ping-interval",
    "bungee-ping-save",
    "bungee-servers",
    "velocity-ping-interval",
    "velocity-ping-save",
    "velocity-servers",
    "block-protocols",
    "block-disconnect-msg",
    "reload-disconnect-msg",
    "max-pps",
    "max-pps-kick-msg",
    "tracking-period",
    "tracking-warning-pps",
    "tracking-max-warnings",
    "tracking-max-kick-msg",
    "blockconnection-method",
    "quick-move-action-fix",
    "item-cache",
    "change-1_9-hitbox",
    "change-1_14-hitbox",
    "use-new-deathmessages",
    "nms-player-ticking");

  public SFViaConfig(Path dataFolder, JLoggerToSLF4J logger) {
    super(dataFolder.resolve("config.yml").toFile(), logger);
    this.reload();
  }

  @Override
  protected void handleConfig(Map<String, Object> config) {}

  @Override
  public List<String> getUnsupportedOptions() {
    return UNSUPPORTED;
  }

  @Override
  public boolean isCheckForUpdates() {
    return false;
  }

  @Override
  public String getBlockConnectionMethod() {
    return "packet";
  }

  @Override
  public boolean is1_12QuickMoveActionFix() {
    return false;
  }

}

/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.settings.lib;

import com.soulfiremc.grpc.generated.BoolSetting;
import com.soulfiremc.grpc.generated.ClientPluginSettingEntry;
import com.soulfiremc.grpc.generated.ClientPluginSettingEntryMinMaxPair;
import com.soulfiremc.grpc.generated.ClientPluginSettingEntryMinMaxPairSingle;
import com.soulfiremc.grpc.generated.ClientPluginSettingEntrySingle;
import com.soulfiremc.grpc.generated.ClientPluginSettingType;
import com.soulfiremc.grpc.generated.ClientPluginSettingsPage;
import com.soulfiremc.grpc.generated.ComboOption;
import com.soulfiremc.grpc.generated.ComboSetting;
import com.soulfiremc.grpc.generated.DoubleSetting;
import com.soulfiremc.grpc.generated.IntSetting;
import com.soulfiremc.grpc.generated.StringSetting;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.DoubleProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.SingleProperty;
import com.soulfiremc.server.settings.property.StringProperty;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerSettingsRegistry {
  private final Map<String, NamespaceRegistry> namespaceMap = new LinkedHashMap<>();

  private static IntSetting createIntSetting(IntProperty property) {
    var builder =
      IntSetting.newBuilder()
        .setDef(property.defaultValue())
        .setMin(property.minValue())
        .setMax(property.maxValue())
        .setStep(property.stepValue());

    if (property.format() != null) {
      builder = builder.setFormat(property.format());
    }

    return builder.build();
  }

  private static DoubleSetting createDoubleSetting(DoubleProperty property) {
    var builder =
      DoubleSetting.newBuilder()
        .setDef(property.defaultValue())
        .setMin(property.minValue())
        .setMax(property.maxValue())
        .setStep(property.stepValue());

    if (property.format() != null) {
      builder = builder.setFormat(property.format());
    }

    return builder.build();
  }

  public ServerSettingsRegistry addClass(Class<? extends SettingsObject> clazz, String pageName) {
    return addClass(clazz, pageName, false);
  }

  public ServerSettingsRegistry addClass(
    Class<? extends SettingsObject> clazz, String pageName, boolean hidden) {
    for (var field : clazz.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers())
        && Modifier.isFinal(field.getModifiers())
        && Modifier.isStatic(field.getModifiers())
        && Property.class.isAssignableFrom(field.getType())) {
        field.setAccessible(true);

        try {
          var property = (Property) field.get(null);
          if (property == null) {
            throw new IllegalStateException("Property is null!");
          }

          var registry = namespaceMap.get(property.namespace());
          if (registry == null) {
            registry = new NamespaceRegistry(pageName, hidden, new ArrayList<>());
            namespaceMap.put(property.namespace(), registry);
          }

          registry.properties.add(property);
        } catch (IllegalAccessException e) {
          throw new IllegalStateException("Failed to get property!", e);
        }
      }
    }

    return this;
  }

  public List<ClientPluginSettingsPage> exportSettingsMeta() {
    List<ClientPluginSettingsPage> list = new ArrayList<>();

    for (var namespaceEntry : namespaceMap.entrySet()) {
      var namespaceRegistry = namespaceEntry.getValue();
      List<ClientPluginSettingEntry> entries = processProperties(namespaceRegistry.properties);
      list.add(createSettingsPage(namespaceEntry.getKey(), namespaceRegistry, entries));
    }

    return list;
  }

  private List<ClientPluginSettingEntry> processProperties(List<Property> properties) {
    List<ClientPluginSettingEntry> entries = new ArrayList<>();
    for (var property : properties) {
      switch (property) {
        case BooleanProperty booleanProperty -> entries.add(createBooleanEntry(booleanProperty));
        case IntProperty intProperty -> entries.add(createIntEntry(intProperty));
        case DoubleProperty doubleProperty -> entries.add(createDoubleEntry(doubleProperty));
        case MinMaxPropertyLink minMaxPropertyLink -> entries.add(createMinMaxEntry(minMaxPropertyLink));
        case StringProperty stringProperty -> entries.add(createStringEntry(stringProperty));
        case ComboProperty comboProperty -> entries.add(createComboEntry(comboProperty));
      }
    }
    return entries;
  }

  private ClientPluginSettingEntry createBooleanEntry(BooleanProperty property) {
    return ClientPluginSettingEntry.newBuilder()
      .setSingle(
        fillSingleProperties(property)
          .setType(ClientPluginSettingType.newBuilder()
            .setBool(BoolSetting.newBuilder()
              .setDef(property.defaultValue())
              .build())
            .build())
          .build())
      .build();
  }

  private ClientPluginSettingEntry createIntEntry(IntProperty property) {
    return ClientPluginSettingEntry.newBuilder()
      .setSingle(
        fillSingleProperties(property)
          .setType(ClientPluginSettingType.newBuilder()
            .setInt(createIntSetting(property))
            .build())
          .build())
      .build();
  }

  private ClientPluginSettingEntry createDoubleEntry(DoubleProperty property) {
    return ClientPluginSettingEntry.newBuilder()
      .setSingle(
        fillSingleProperties(property)
          .setType(ClientPluginSettingType.newBuilder()
            .setDouble(createDoubleSetting(property))
            .build())
          .build())
      .build();
  }

  private ClientPluginSettingEntry createMinMaxEntry(MinMaxPropertyLink propertyLink) {
    var minProperty = propertyLink.min();
    var maxProperty = propertyLink.max();
    return ClientPluginSettingEntry.newBuilder()
      .setMinMaxPair(
        ClientPluginSettingEntryMinMaxPair.newBuilder()
          .setMin(
            fillMultiProperties(minProperty)
              .setIntSetting(createIntSetting(minProperty))
              .build())
          .setMax(
            fillMultiProperties(maxProperty)
              .setIntSetting(createIntSetting(maxProperty))
              .build())
          .build())
      .build();
  }

  private ClientPluginSettingEntry createStringEntry(StringProperty property) {
    return ClientPluginSettingEntry.newBuilder()
      .setSingle(
        fillSingleProperties(property)
          .setType(ClientPluginSettingType.newBuilder()
            .setString(StringSetting.newBuilder()
              .setDef(property.defaultValue())
              .setSecret(property.secret())
              .build())
            .build())
          .build())
      .build();
  }

  private ClientPluginSettingEntry createComboEntry(ComboProperty property) {
    List<ComboOption> options = new ArrayList<>();
    for (var option : property.options()) {
      options.add(
        ComboOption.newBuilder()
          .setId(option.id())
          .setDisplayName(option.displayName())
          .build()
      );
    }

    return ClientPluginSettingEntry.newBuilder()
      .setSingle(
        fillSingleProperties(property)
          .setType(
            ClientPluginSettingType.newBuilder()
              .setCombo(
                ComboSetting.newBuilder()
                  .setDef(property.defaultValue())
                  .addAllOptions(options)
                  .build()
              )
              .build()
          )
          .build()
      )
      .build();
  }

  private ClientPluginSettingsPage createSettingsPage(String namespace, NamespaceRegistry namespaceRegistry, List<ClientPluginSettingEntry> entries) {
    return ClientPluginSettingsPage.newBuilder()
      .setPageName(namespaceRegistry.pageName)
      .setHidden(namespaceRegistry.hidden)
      .setNamespace(namespace)
      .addAllEntries(entries)
      .build();
  }


  private ClientPluginSettingEntrySingle.Builder fillSingleProperties(SingleProperty property) {
    return ClientPluginSettingEntrySingle.newBuilder()
      .setKey(property.key())
      .setUiName(property.uiName())
      .addAllCliFlags(Arrays.asList(property.cliFlags()))
      .setDescription(property.description());
  }

  private ClientPluginSettingEntryMinMaxPairSingle.Builder fillMultiProperties(
    SingleProperty property) {
    return ClientPluginSettingEntryMinMaxPairSingle.newBuilder()
      .setKey(property.key())
      .setUiName(property.uiName())
      .addAllCliFlags(Arrays.asList(property.cliFlags()))
      .setDescription(property.description());
  }

  private record NamespaceRegistry(String pageName, boolean hidden, List<Property> properties) {}
}

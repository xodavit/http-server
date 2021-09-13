package ru.xodavit.http.server.app.dto;

import lombok.Value;

import java.util.Set;

@Value
public class SimpleRequestDto {
  long id;
  String name;
  Set<String> tags;
}

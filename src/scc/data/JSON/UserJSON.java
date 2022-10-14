package scc.data.JSON;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserJSON(String nickname, String name, String password, String imageBase64) { }

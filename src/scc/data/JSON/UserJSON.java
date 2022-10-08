package scc.data.JSON;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserJSON {

    public final String nickname;
    public final String name;
    public final String password;
    public final String imageBase64;

    public UserJSON(@JsonProperty("nickname") String nickname, @JsonProperty("name") String name,
                    @JsonProperty("password") String password, @JsonProperty("imageBase64") String imageBase64) {
        this.nickname = nickname;
        this.name = name;
        this.password = password;
        this.imageBase64 = imageBase64;
    }

    @Override
    public String toString() {
        return "UserJSON{" +
                "nickname='" + nickname + '\'' +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", imageBase64='" + imageBase64 + '\'' +
                '}';
    }
}

package scc.data;

public class UserParamsJSON  {

    public final String nickname;
    public final String name;
    public final String password;
    public final String imageBase64;

    public UserParamsJSON(String nickname, String name, String password, String imageBase64) {
        this.nickname = nickname;
        this.name = name;
        this.password = password;
        this.imageBase64 = imageBase64;
    }

    @Override
    public String toString() {
        return "UserParamsJSON{" +
                "nickname='" + nickname + '\'' +
                ", name='" + name + '\'' +
                ", password='" + password + '\'' +
                ", imageBase64='" + imageBase64 + '\'' +
                '}';
    }
}

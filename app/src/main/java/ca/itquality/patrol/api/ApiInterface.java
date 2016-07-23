package ca.itquality.patrol.api;

import ca.itquality.patrol.auth.data.User;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface ApiInterface {

    @FormUrlEncoded
    @POST("user/register.php")
    Call<User> register(@Field("name") String name, @Field("email") String email,
                        @Field("password") String password, @Field("photo") String photo);

    @FormUrlEncoded
    @POST("user/sign_in_facebook.php")
    Call<User> signInFacebook(@Field("id") String id, @Field("name") String name,
                              @Field("email") String email, @Field("photo") String photo);

    @FormUrlEncoded
    @POST("user/login.php")
    Call<User> login(@Field("email") String email, @Field("password") String password);

    @FormUrlEncoded
    @POST("user/profile.php")
    Call<User> getProfile(@Field("id") String id, @Field("token") String token);
}
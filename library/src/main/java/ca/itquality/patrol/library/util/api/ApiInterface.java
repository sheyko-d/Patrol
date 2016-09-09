package ca.itquality.patrol.library.util.api;

import java.util.ArrayList;

import ca.itquality.patrol.library.util.auth.data.User;
import ca.itquality.patrol.library.util.messages.data.Message;
import ca.itquality.patrol.library.util.messages.data.MessageThread;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;


public interface ApiInterface {

    /**
     * User APIs.
     */
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
    @POST("user/update_profile.php")
    Call<User> updateProfile(@Field("id") String id, @Field("token") String token,
                             @Field("google_token") String googleToken);

    /**
     * Assigned Object APIs.
     */
    @FormUrlEncoded
    @POST("assigned_object/assign_user.php")
    Call<User.AssignedObject> assignUser(@Field("id") String id, @Field("title") String title,
                                         @Field("latitude") double latitude,
                                         @Field("longitude") double longitude);

    /**
     * Alert APIs.
     */
    @FormUrlEncoded
    @POST("alert/send_alert.php")
    Call<Void> sendAlert(@Field("token") String token, @Field("latitude") double latitude,
                         @Field("longitude") double longitude);

    /**
     * Message APIs.
     */
    @FormUrlEncoded
    @POST("message/send_message.php")
    Call<MessageThread> sendMessage(@Field("token") String token,
                                    @Field("thread_id") String threadId,
                                    @Field("participants") String participants,
                                    @Field("thread_title") String threadTitle,
                                    @Field("message") String message);

    @FormUrlEncoded
    @POST("message/get_contacts.php")
    Call<ArrayList<User>> getContacts(@Field("token") String token);

    @FormUrlEncoded
    @POST("message/get_messages.php")
    Call<ArrayList<Message>> getMessages(@Field("token") String token,
                                         @Field("thread_id") String threadId,
                                         @Field("participants") String participants,
                                         @Field("thread_title") String threadTitle);

    @FormUrlEncoded
    @POST("message/get_unread_messages.php")
    Call<ArrayList<Message>> getUnreadMessages(@Field("token") String token,
                                               @Field("last_seen_time") Long chatLastSeenTime);

    @FormUrlEncoded
    @POST("message/get_threads.php")
    Call<ArrayList<MessageThread>> getThreads(@Field("token") String token);

    @FormUrlEncoded
    @POST("clock_in/post_clock_in_reason.php")
    Call<Void> postClockInReason(@Field("token") String token, @Field("shift_id") String shiftId,
                                 @Field("reason") String reason);

    @FormUrlEncoded
    @POST("data/post_heart_rate.php")
    Call<Void> uploadHeartRate(@Field("token") String token,
                               @Field("heart_rate_values") String values);

    @FormUrlEncoded
    @POST("data/post_steps.php")
    Call<Void> uploadSteps(@Field("token") String token, @Field("step_values") String values);

    @FormUrlEncoded
    @POST("data/post_activity.php")
    Call<Void> uploadActivity(@Field("token") String token,
                              @Field("activity_values") String values);
}
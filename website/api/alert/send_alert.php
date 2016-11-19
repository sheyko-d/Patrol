<?

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}
if (isset($_POST['latitude'])){
	$latitude = $_POST['latitude'];
}
if (isset($_POST['longitude'])){
	$longitude = $_POST['longitude'];
}

// Check if any parameters are empty
if (empty($token) or empty($latitude) or empty($longitude)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);
$latitude = mysqli_real_escape_string($con, $latitude);
$longitude = mysqli_real_escape_string($con, $longitude);

// Find id of the user with the specified token
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");

// Check if user exists
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

// Find user in the database with the specified id
$user_query = $db->makeQuery($con, "SELECT name, assigned_object_id FROM user WHERE user_id='$id'");
$user_result = $user_query->fetch_assoc();
$name = $user_result["name"];
$assigned_object_id = $user_result["assigned_object_id"];

$assigned_object_query = $db->makeQuery($con, "SELECT title, admin_emails FROM assigned_object WHERE assigned_object_id='$assigned_object_id'");
if ($assigned_object_result = $assigned_object_query->fetch_assoc()){
	$assigned_object_title = " (".$assigned_object_result["title"].")";
	$admin_emails = $assigned_object_result["admin_emails"];
}

$recipients = $admin_emails;

if ($recipients!=null){
	mail($recipients, 'Emergency alert', $name.' just reported an emergency! Other guards from his team were requested for backup.');
}

// Find assigned object id of the user with the specified id
$assigned_object_query = $db->makeQuery($con, "SELECT assigned_object_id FROM user WHERE user_id='$id'");

// Check if assigned object exists
$assigned_object_result = $assigned_object_query->fetch_assoc();
if ($assigned_object_result != null){
	$assigned_object_id = $assigned_object_result["assigned_object_id"];
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

// Find other users who are assigned to the same object
$users_query = $db->makeQuery($con, "SELECT google_token FROM user WHERE assigned_object_id='$assigned_object_id' AND user_id<>'$id'");

// The recipient registration tokens for this notification
// https://developer.android.com/google/gcm/    
$recipient_google_tokens = array();

// Check if users exist
while ($users_result = $users_query->fetch_assoc()){
	$google_token = $users_result["google_token"];
	array_push($recipient_google_tokens, $google_token);
}

// Send push notification via Google Cloud Messaging
$data = array('type' => 'alert', 'name' => $name, 'latitude' => $latitude, 'longitude' => $longitude);
sendPushNotification($data, $recipient_google_tokens);

function sendPushNotification($data, $recipient_google_tokens){   
    $apiKey = 'AIzaSyCMIuNEpBVT9nOXmkyok9Aevf1db0RrZ3Y';

    // Set POST request body
    $post = array(
                    'registration_ids'   => $recipient_google_tokens,
                    'data' => $data,
                 );

    // Set CURL request headers 
    $headers = array( 
                        'Authorization: key=' . $apiKey,
                        'Content-Type: application/json'
                    );

    // Initialize curl handle       
    $ch = curl_init();

    // Set URL to GCM push endpoint     
    curl_setopt($ch, CURLOPT_URL, 'https://fcm.googleapis.com/fcm/send');

    // Set request method to POST       
    curl_setopt($ch, CURLOPT_POST, true);

    // Set custom request headers       
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);

    // Get the response back as string instead of printing it       
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

    // Set JSON post data
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($post));

    // Actually send the request    
    $result = curl_exec($ch);

    // Handle errors
    if (curl_errno($ch)){
        echo 'GCM error: ' . curl_error($ch);
    }

    // Close curl handle
    curl_close($ch);

    // Debug GCM response       
    echo $result;
}

?>
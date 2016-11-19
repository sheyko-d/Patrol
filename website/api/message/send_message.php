<?php

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}
if (isset($_POST['thread_id'])){
	$thread_id = $_POST['thread_id'];
} else {
	$thread_id = null;
}
if (isset($_POST['participants'])){
	$participants = $_POST['participants'];
} else {
	$participants = null;
}
if (isset($_POST['message'])){
	$message = $_POST['message'];
}
if (isset($_POST['thread_title']) and count($participants)>0){
	$thread_title = $_POST['thread_title'];
}

// Check if any parameters are empty
if (empty($token) or (empty($thread_id) and empty($participants)) or empty($message)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect("localhost", "root", "", "database_guard");
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);
$thread_id = mysqli_real_escape_string($con, $thread_id);
$participants = json_decode($participants, true);
$message = mysqli_real_escape_string($con, $message);
$thread_title = mysqli_real_escape_string($con, $thread_title);

// Find id of the user with the specified token
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
	
	$user_query = $db->makeQuery($con, "SELECT name, photo FROM user WHERE user_id='$id'");
	$user_result = $user_query->fetch_assoc();
	$name = $user_result["name"];
	$photo = $user_result["photo"];
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

// Check if user is trying to create a new conversation
if ($thread_id == null){
	$create_new_thread = true;

	$participants_to_check = $participants;
	array_push($participants_to_check, $id+0);

	// Convert all strings to integers
	foreach ($participants_to_check as $key => $var) {
		$participants_to_check[$key] = (int)$var;
	}
	
	sort($participants_to_check);
	$participants_search = substr(json_encode($participants_to_check), 1, -1);
	
	$threads_query = $db->makeQuery($con, "SELECT thread_id, group_concat(DISTINCT user_id ORDER BY user_id ASC) as participant_ids FROM message_thread_participant GROUP BY thread_id ORDER BY user_id");
	while ($threads_result = $threads_query->fetch_assoc()){
		$participant_ids = $threads_result["participant_ids"];
		if ($participant_ids == $participants_search){
			$thread_id = $threads_result["thread_id"];
			
			// Check if group with the same title doesn't already exist
			if ($thread_title != null){
				$thread_query = $db->makeQuery($con, "SELECT title FROM message_thread WHERE message_thread_id='$thread_id'");
				if ($thread_result = $thread_query->fetch_assoc()){
					if ($thread_result["title"] != $thread_title){
						$create_new_thread = true;
						$thread_id = null;
					} else {
						$create_new_thread = false;
					}
				}
			} else {
				$create_new_thread = false;
			}
		}
	}
	
	if ($create_new_thread){
		
		// Create a new thread
		if (!mysqli_query($con, "INSERT INTO message_thread (title) VALUES ('$thread_title')")){
			http_response_code(409);
			die(json_encode(array("message"=>"Can't create new user.")));
		}
		
		$thread_id = mysqli_insert_id($con);
		
		// Add the user to the participants table
		mysqli_query($con, "INSERT INTO message_thread_participant (thread_id, user_id) VALUES ('$thread_id', '$id')");
		
		// Add other invited users to the participants table
		foreach ($participants as $participant_id){
			mysqli_query($con, "INSERT INTO message_thread_participant (thread_id, user_id) VALUES ('$thread_id', '$participant_id')");
		}
	} else {
		// Retrieve the existing thread
		$thread_query = $db->makeQuery($con, "SELECT title FROM message_thread WHERE message_thread_id='$thread_id'");
		$thread_result = $thread_query->fetch_assoc();
		if ($thread_result != null){
			$thread_title = $thread_result["title"];
		}
	}
} else {
	// Retrieve the existing thread
	$thread_query = $db->makeQuery($con, "SELECT title FROM message_thread WHERE message_thread_id='$thread_id'");
	$thread_result = $thread_query->fetch_assoc();
	if ($thread_result != null){
		$thread_title = $thread_result["title"];
	}
}

// Set a UTC timezone
date_default_timezone_set("UTC");
	
// Add message to the database
$time = time() * 1000;
mysqli_query($con, "INSERT INTO message (thread_id, user_id, text, time) VALUES ('$thread_id', '$id', '$message', '$time')");
	
// Retrieve user ids of the recipients in the same thread
$participant_google_tokens = array();
$participant_token_query = $db->makeQuery($con, "SELECT google_token FROM user WHERE user_id IN (SELECT user_id FROM message_thread_participant WHERE thread_id='$thread_id' AND user_id<>'$id')");
while ($participant_token_result = $participant_token_query->fetch_assoc()){
	$participant_google_token = $participant_token_result["google_token"];
	
	array_push($participant_google_tokens, $participant_google_token);
}

// Send push notification via Google Cloud Messaging
$data = array('type' => 'message', 'text' => $message, 'name' => $name, 'title' => $thread_title, 'photo' => $photo, 'thread_id' => $thread_id);
sendPushNotification($data, $participant_google_tokens);

echo json_encode(array("thread_id" => $thread_id, "title" => $thread_title));

function sendPushNotification($data, $participant_google_tokens){   
    $apiKey = 'AIzaSyCMIuNEpBVT9nOXmkyok9Aevf1db0RrZ3Y';

    // Set POST request body
    $post = array(
                    'registration_ids'   => $participant_google_tokens,
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

    // Close curl handle
    curl_close($ch);
}

?>
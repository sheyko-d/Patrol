<?php

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}
if (isset($_POST['last_seen_time'])){
	$last_seen_time = $_POST['last_seen_time'];
}

// Check if any parameters are empty
if (empty($token) or empty($last_seen_time)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect("localhost", "root", "", "database_guard");
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);
$last_seen_time = mysqli_real_escape_string($con, $last_seen_time);

// Check if user's token is valid to prevent unauthorized access
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else { 
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

$messages = array();

// Retrieve all messages from the thread with a specified id
$messages_query = $db->makeQuery($con, "SELECT message_id, user_id, text, time FROM message WHERE thread_id IN (SELECT thread_id FROM message_thread_participant WHERE user_id='$id' GROUP BY thread_id) AND time>'$last_seen_time' ORDER BY time ASC");
while ($messages_result = $messages_query->fetch_assoc()){
	$message_id = $messages_result["message_id"];
	$user_id = $messages_result["user_id"];
	$text = $messages_result["text"];
	$time = $messages_result["time"];
		
		$user_query = $db->makeQuery($con, "SELECT name FROM user WHERE user_id='$user_id'");
		$user_result = $user_query->fetch_assoc();
		$name = $user_result["name"];
		
	array_push($messages, array("message_id" => $message_id, "user_id" => $user_id, "user_name" => $name, "text" => $text, "time" => $time));
}

// If no new messages are found, return the last seen message
if (empty($messages)){
	$message_query = $db->makeQuery($con, "SELECT message_id, user_id, text, time FROM message WHERE thread_id IN (SELECT thread_id FROM message_thread_participant WHERE user_id='$id' GROUP BY thread_id) ORDER BY time DESC LIMIT 1");
	if ($message_result = $message_query->fetch_assoc()){
		$message_id = $message_result["message_id"];
		$user_id = $message_result["user_id"];
		$text = $message_result["text"];
		$time = $message_result["time"];
		
		$user_query = $db->makeQuery($con, "SELECT name FROM user WHERE user_id='$user_id'");
		$user_result = $user_query->fetch_assoc();
		$name = $user_result["name"];
			
		array_push($messages, array("message_id" => $message_id, "user_id" => $user_id, "user_name" => $name, "text" => $text, "time" => $time));
	}
}

echo json_encode($messages);

?>
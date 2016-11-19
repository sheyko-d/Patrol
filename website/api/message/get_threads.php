<?php

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}

// Check if any parameters are empty
if (empty($token)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect("localhost", "root", "", "database_guard");
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);

// Find user in the database with the specified token
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else { 
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

$threads = array();

// Retrieve all message threads where user participates
$threads_query = $db->makeQuery($con, "SELECT thread_id FROM message_thread_participant WHERE user_id='$id'");
while ($threads_result = $threads_query->fetch_assoc()){
	$thread_id = $threads_result["thread_id"];
	
	// Retrieve a thread title
	$thread_title_query = $db->makeQuery($con, "SELECT title FROM message_thread WHERE message_thread_id='$thread_id'");
	$thread_title_result = $thread_title_query->fetch_assoc();
	$thread_title = $thread_title_result["title"];
	
	// If thread title is empty, then replace title with the recipient's name
	if (empty($thread_title)){
		// Find id of the other person from the same conversation
		$thread_recipient_id_query = $db->makeQuery($con, "SELECT user_id FROM message_thread_participant WHERE thread_id='$thread_id' AND user_id<>'$id'");
		$thread_recipient_id_result = $thread_recipient_id_query->fetch_assoc();
		$thread_recipient_id = $thread_recipient_id_result["user_id"];
		
		// And retrieve his/her name
		$thread_recipient_name_query = $db->makeQuery($con, "SELECT name, photo FROM user WHERE user_id='$thread_recipient_id'");
		$thread_recipient_name_result = $thread_recipient_name_query->fetch_assoc();
		$thread_title = $thread_recipient_name_result["name"];
		$photo = $thread_recipient_name_result["photo"];
	}
	
	$last_message_query = $db->makeQuery($con, "SELECT text, user_id, time FROM message WHERE thread_id='$thread_id' ORDER BY time DESC");
	$last_message_result = $last_message_query->fetch_assoc();
	if ($last_message_result != null){
		$text = $last_message_result["text"];
		$time = $last_message_result["time"];
		$sender_user_id = $last_message_result["user_id"];
		
		$sender_query = $db->makeQuery($con, "SELECT name FROM user WHERE user_id='$sender_user_id'");
		$sender_result = $sender_query->fetch_assoc();
		$sender_name = $sender_result["name"];
		$last_message = array("user_id" => $sender_user_id, "user_name" => $sender_name, "text" => $text, "time" => $time);
	}
	
	array_push($threads, array("thread_id" => $thread_id, "title" => $thread_title, "last_message" => $last_message, "photo" => $photo));
}

echo json_encode($threads);

?>
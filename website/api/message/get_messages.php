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
if (isset($_POST['thread_title'])){
	$thread_title = $_POST['thread_title'];
}

// Check if any parameters are empty
if (empty($token) or (empty($thread_id) and empty($participants))){
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
$thread_title = mysqli_real_escape_string($con, $thread_title);

// Check if user's token is valid to prevent unauthorized access
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else { 
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

if ($thread_id == null){
	// Check if the same thread was created before
	$participants_to_check = $participants;
	array_push($participants_to_check, $id);

	// Convert all strings to integers
	foreach ($participants_to_check as $key => $var) {
		$participants_to_check[$key] = (int)$var;
	}
	
	sort($participants_to_check);
	$participants_search = substr(json_encode($participants_to_check), 1, -1);
	
	$threads_query = $db->makeQuery($con, "SELECT thread_id, group_concat(DISTINCT user_id ORDER BY user_id ASC) as participant_ids FROM message_thread_participant GROUP BY thread_id ORDER BY user_id");
	while ($thread_result = $threads_query->fetch_assoc()){
		$participant_ids = $thread_result["participant_ids"];
		if ($participant_ids == $participants_search){
			$thread_id = $thread_result["thread_id"];
			
			// Check if group with the same title doesn't already exist
			if ($thread_title != null){
				$thread_query = $db->makeQuery($con, "SELECT title FROM message_thread WHERE message_thread_id='$thread_id'");
				if ($thread_result = $thread_query->fetch_assoc()){
					if ($thread_result["title"] != $thread_title){
						$thread_id = null;
					}
				}
			}
		}
	}
}

$messages = array();

if ($thread_id != null){
	// Retrieve all messages from the thread with a specified id
	$messages_query = $db->makeQuery($con, "SELECT message_id, user_id, text, time FROM message WHERE thread_id='$thread_id' ORDER BY time ASC");
	while ($messages_result = $messages_query->fetch_assoc()){
		$message_id = $messages_result["message_id"];
		$user_id = $messages_result["user_id"];
		$text = $messages_result["text"];
		$time = $messages_result["time"];
		
		array_push($messages, array("message_id" => $message_id, "user_id" => $user_id, "text" => $text, "time" => $time));
	}
}

echo json_encode($messages);

?>
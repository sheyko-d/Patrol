<?php

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}
if (isset($_POST['google_token'])){
	$google_token = $_POST['google_token'];
}

// Check if any parameters are empty
if (empty($token)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);
$google_token = mysqli_real_escape_string($con, $google_token);

// Retrieve user id from token
$user_id_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$user_id_result = $user_id_query->fetch_assoc();
if ($user_id_result != null){
	$id = $user_id_result["user_id"];
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect")));
}

// Find user in the database with the specified id
$user_query = $db->makeQuery($con, "SELECT user_id, assigned_object_id, email, name, photo FROM user WHERE user_id='$id'");

// Check if user exists
$user_result = $user_query->fetch_assoc();
if ($user_result != null){
	$assigned_object_id = $user_result["assigned_object_id"];
	$email = $user_result["email"];
	$name = $user_result["name"];
	$photo = $user_result["photo"];
	
	// Find the assigned object of the current guard in the database
	$assigned_object_query = $db->makeQuery($con, "SELECT title, latitude, longitude, contacts, safety, video, sitting_duration, watch_removed_max_min FROM assigned_object
		WHERE assigned_object_id='$assigned_object_id'");
	$assigned_object_result = $assigned_object_query->fetch_assoc();
	if ($assigned_object_result != null){
		$title = $assigned_object_result["title"];
		$latitude = $assigned_object_result["latitude"];
		$longitude = $assigned_object_result["longitude"];
		$contacts = $assigned_object_result["contacts"];
		$safety = $assigned_object_result["safety"];
		$video = $assigned_object_result["video"];
		$watch_removed_max_min = $assigned_object_result["watch_removed_max_min"];
		$sitting_duration = $assigned_object_result["sitting_duration"];
		$assigned_object = array("assigned_object_id"=>$assigned_object_id, "title"=>$title, "latitude"=>$latitude, "longitude"=>$longitude, "contacts"=>$contacts, "safety"=>$safety, "video"=>$video, "sitting_duration"=>$sitting_duration, "watch_removed_max_min"=>$watch_removed_max_min);
	}
	
	// Find the assigned shift of the current guard in the database
	$assigned_shift_query = $db->makeQuery($con, "SELECT shift_id FROM assigned_shift WHERE user_id='$id'");
	$assigned_shifts = array();
	while ($assigned_shift_result = $assigned_shift_query->fetch_assoc()){
		$assigned_shift_id = $assigned_shift_result["shift_id"];
		$shift_query = $db->makeQuery($con, "SELECT shift_id, name, start_time, end_time FROM shift WHERE shift_id='$assigned_shift_id'");
		$shift_result = $shift_query->fetch_assoc();
		$shift_id = $shift_result["shift_id"];
		$assigned_shift_name = $shift_result["name"];
		$assigned_shift_start_time = $shift_result["start_time"] * 1000;
		$assigned_shift_end_time = $shift_result["end_time"] * 1000;
		$assigned_shift = array("assigned_shift_id"=>$shift_id, "name"=>$assigned_shift_name, "start_time"=>$assigned_shift_start_time, "end_time"=>$assigned_shift_end_time);
		array_push($assigned_shifts, $assigned_shift);
	}
	
	if (!mysqli_query($con, "UPDATE user SET google_token='$google_token' WHERE user_id='$id'")){
		http_response_code(403);
		die(json_encode(array("message"=>"Can't update token")));
	}
	
	http_response_code(200);
	echo json_encode(array("token"=>$token, "user_id"=>$id, "assigned_object"=>$assigned_object, "assigned_shifts"=>$assigned_shifts, "name"=>$name, "email"=>$email, "photo"=>$photo));
} else {
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect")));
}

?>
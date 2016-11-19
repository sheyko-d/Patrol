<?php

// Retrieve POST parameters
if (isset($_POST['id'])){
	$id = $_POST['id'];
}
if (isset($_POST['name'])){
	$name = $_POST['name'];
}
if (isset($_POST['email'])){
	$email = $_POST['email'];
}
if (isset($_POST['photo'])){
	$photo = $_POST['photo'];
} else {
	$photo = null;
}

// Check if any parameters are empty
if (empty($id) or empty($name) or empty($email)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$id = mysqli_real_escape_string($con, $id);
$name = mysqli_real_escape_string($con, $name);
$email = mysqli_real_escape_string($con, $email);
$photo = mysqli_real_escape_string($con, $photo);

// Find user in the database with the specified email and password
$user_query = $db->makeQuery($con, "SELECT user_id, assigned_object_id, email, name, photo FROM user WHERE user_id='$id'");

// Check if user exists
$results_num = $db->countResults($user_query);
if ($results_num == 0){
	if (!mysqli_query($con, "INSERT INTO user (user_id, name, email, photo) VALUES ('$id', '$name', '$email', '$photo')")){
		http_response_code(409);
		die(json_encode(array("message"=>"Can't create new user.")));
	}
	$id = mysqli_insert_id($con);
} else {	
	$user_result = $user_query->fetch_assoc();
	$id = $user_result["user_id"];	
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
}

$token = updateToken($db, $con, $id);

http_response_code(200);
echo json_encode(array("token"=>$token, "user_id"=>$id, "assigned_object"=>$assigned_object, "assigned_shifts"=>$assigned_shifts, "name"=>$name, "email"=>$email, "photo"=>$photo));

function updateToken($db, $con, $id){
	$secret = generateNewToken();
	
	$token_query = $db->makeQuery($con, "SELECT secret FROM token WHERE user_id='$id'");
	$token_result = $token_query->fetch_assoc();
	if ($token_result != null){
		$token_query = $db->makeQuery($con, "UPDATE token SET secret='$secret' WHERE user_id='$id'");
	} else {
		$db->makeQuery($con, "INSERT INTO token(user_id, secret) values('$id', '$secret')");
	}
	
	return $secret;
}

// Generates a random string which will be attached to password before encoding to keep it secure
function generateNewToken() {
    $alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890';
    $pass = array(); //remember to declare $pass as an array
    $alphaLength = strlen($alphabet) - 1; //put the length -1 in cache
    for ($i = 0; $i < 30; $i++) {
        $n = rand(0, $alphaLength);
        $pass[] = $alphabet[$n];
    }
    return implode($pass); //turn the array into a string
}

?>
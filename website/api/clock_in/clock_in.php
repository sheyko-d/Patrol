<?php

// Retrieve POST parameters
if (isset($_POST['token'])){
	$token = $_POST['token'];
}
if (isset($_POST['shift_id'])){
	$shift_id = $_POST['shift_id'];
}
if (isset($_POST['started'])){
	$started = $_POST['started'];
}

// Check if any parameters are empty
if (empty($token) or empty($shift_id) or empty($started)){
	http_response_code(400);
	die(json_encode(array("message"=>"Some fields are empty.")));
}

// Connect to the database
require_once("../util/database.php");
$db = new DBConnect();
$con = $db->openConnection();

// Escape POST parameters to prevent MySQL errors
$token = mysqli_real_escape_string($con, $token);
$shift_id = mysqli_real_escape_string($con, $shift_id);
$time = mysqli_real_escape_string($con, $time);
$started = mysqli_real_escape_string($con, $started);

// Check if user's token is valid to prevent unauthorized access
$token_query = $db->makeQuery($con, "SELECT user_id FROM token WHERE secret='$token'");
$token_result = $token_query->fetch_assoc();
if ($token_result != null){
	$id = $token_result["user_id"];
} else { 
	http_response_code(403);
	die(json_encode(array("message"=>"Token is incorrect.")));
}

$time = time();

if ($started == "true"){
	$started = 1;
} else {
	$started = 0;
}

mysqli_query($con, "INSERT INTO clock_in(user_id, shift_id, time, started) VALUES ('$id', '$shift_id', '$time', '$started')");




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

$shift_query = $db->makeQuery($con, "SELECT name FROM shift WHERE shift_id='$shift_id'");
if ($shift_result = $shift_query->fetch_assoc()){
	$shift_name = $shift_result["name"];
}

$recipients = $admin_emails;

if ($recipients!=null){
	mail($recipients, ($started == 1?'Clock in':'Clock out'), $name.' just '.($started==1?"started":"ended").' his shift "'.$shift_name.'"');
}

?>